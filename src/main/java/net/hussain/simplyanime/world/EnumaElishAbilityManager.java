package net.hussain.simplyanime.world;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.entity.EnumaElishVisualEntity;
import net.hussain.simplyanime.item.EnumaElishItem;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.hussain.simplyanime.registry.SoundRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraftforge.entity.PartEntity;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Charge -> release -> beam -> fade. The beam is a line test against entity boxes, nothing
// here ever reads or writes a block state apart from the one raycast that finds where it stops.
public final class EnumaElishAbilityManager {

    private static final Map<ServerWorld, Map<UUID, Cast>> CASTS = new HashMap<>();

    private static final int RELEASE_TICKS = 4;
    private static final int FADE_TICKS = 16;
    private static final double CHUNK = 16.0;

    private EnumaElishAbilityManager() {
    }

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(EnumaElishAbilityManager::tick);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(EnumaElishAbilityManager::clear);
        LifecycleEvent.SERVER_STOPPED.register(server -> CASTS.clear());
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> clearActor(player));
        PlayerEvent.PLAYER_QUIT.register(EnumaElishAbilityManager::clearActor);
        EntityEvent.LIVING_HURT.register(EnumaElishAbilityManager::onHurt);
    }

    public static boolean isBusy(ServerWorld world, LivingEntity actor) {
        Map<UUID, Cast> casts = CASTS.get(world);
        return casts != null && casts.containsKey(actor.getUuid());
    }

    public static boolean isCharging(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, Cast> casts = CASTS.get(world);
        Cast cast = casts == null ? null : casts.get(entity.getUuid());
        return cast != null && cast.stage == Stage.CHARGE;
    }

    // Architectury fires this from Forge's LivingAttackEvent, so cancelling it stops the hit
    // before armor, knockback or hurt time get touched. Void and /kill still go through.
    private static EventResult onHurt(LivingEntity entity, DamageSource source, float amount) {
        if (isCharging(entity) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    public static boolean start(WeaponAbilityContext context) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        EnumaElishItem.EffectSettings settings = Config.weapons.enuma_elish;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid();

        EnumaElishVisualEntity visual = new EnumaElishVisualEntity(world, actor, settings.chargeDuration,
                (float) settings.beamRadius);
        world.spawnEntity(visual);

        if (settings.chargeSlowness >= 0) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    settings.chargeDuration + RELEASE_TICKS + settings.beamDuration, settings.chargeSlowness,
                    false, false, false));
        }
        playSound(world, actor.getPos(), SoundRegistry.ENUMA_ELISH_CHARGE.get(), settings.chargeVolume, settings.chargePitch);

        Cast cast = new Cast(actor.getUuid(), ownerId, context.stack().copy(),
                context.hand() == null ? Hand.MAIN_HAND : context.hand(), world.getTime(), visual.getUuid(),
                context.target() == null ? null : context.target().getUuid());
        CASTS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), cast);
        return true;
    }

    private static boolean tickCast(ServerWorld world, Cast cast) {
        EnumaElishItem.EffectSettings settings = Config.weapons.enuma_elish;
        if (!(world.getEntity(cast.actorId) instanceof LivingEntity actor) || !actor.isAlive()
                || !(world.getEntity(cast.visualId) instanceof EnumaElishVisualEntity visual)) {
            return false;
        }

        int elapsed = (int) (world.getTime() - cast.startTime);
        int release = settings.chargeDuration;
        int beamStart = release + RELEASE_TICKS;
        int fadeStart = beamStart + settings.beamDuration;
        int end = fadeStart + FADE_TICKS;

        if (cast.stage == Stage.CHARGE) {
            // swapping off the sword mid charge drops the cast, no free iframes
            if (!holdingSword(actor)) {
                actor.removeStatusEffect(StatusEffects.SLOWNESS);
                return false;
            }
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            if (elapsed == Math.max(0, release - settings.shoutLead)) {
                playSound(world, actor.getPos(), SoundRegistry.ENUMA_ELISH_SHOUT.get(), settings.shoutVolume, settings.shoutPitch);
            }
            if (elapsed % 4 == 0) {
                float t = elapsed / (float) Math.max(1, release);
                world.spawnParticles(ParticleTypes.FLAME, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                        2 + (int) (t * 6), 1.4, 0.05, 1.4, 0.01);
            }
            if (elapsed >= release) {
                releaseBeam(world, cast, actor, visual, settings);
            }
            return true;
        }

        if (cast.stage == Stage.RELEASE) {
            if (elapsed >= beamStart) {
                cast.stage = Stage.BEAM;
                visual.setPhase(EnumaElishVisualEntity.PHASE_BEAM, settings.beamDuration);
                playSound(world, cast.origin, SoundRegistry.ENUMA_ELISH_BEAM.get(), settings.beamVolume, settings.beamPitch);
                if (cast.hitTerrain) {
                    playSound(world, cast.end(), SoundRegistry.ENUMA_ELISH_BEAM.get(), settings.beamVolume * 0.6F,
                            settings.beamPitch * 0.85F);
                }
            }
            return true;
        }

        if (cast.stage == Stage.BEAM) {
            int beamTicks = elapsed - beamStart;
            float reach = EnumaElishVisualEntity.growth(beamTicks);
            // the blast lands on each target as the front of the beam reaches it
            if (beamTicks <= EnumaElishVisualEntity.GROW_TICKS) {
                sweep(world, cast, actor, settings, true, 0.0F, reach);
            }
            if (beamTicks > 0 && beamTicks % Math.max(1, settings.tickInterval) == 0) {
                sweep(world, cast, actor, settings, false, 0.0F, 1.0F);
            }
            if (cast.hitTerrain && beamTicks == (int) EnumaElishVisualEntity.GROW_TICKS) {
                Vec3d impact = cast.end();
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
            }
            if (elapsed >= fadeStart) {
                cast.stage = Stage.FADE;
                visual.setPhase(EnumaElishVisualEntity.PHASE_FADE, FADE_TICKS);
                Vec3d impact = cast.end();
                playSound(world, impact, SoundRegistry.ENUMA_ELISH_IMPACT.get(), settings.impactVolume, settings.impactPitch);
                if (impact.squaredDistanceTo(actor.getPos()) > 24 * 24) {
                    // the caster should still hear the finish even when the far end is out of earshot
                    playSound(world, actor.getPos(), SoundRegistry.ENUMA_ELISH_IMPACT.get(), settings.impactVolume * 0.5F,
                            settings.impactPitch);
                }
            }
            return true;
        }

        // still hurts while it shrinks away, at the size it is drawn
        float fade = (elapsed - fadeStart) / (float) FADE_TICKS;
        if (elapsed > fadeStart && (elapsed - beamStart) % Math.max(1, settings.tickInterval) == 0
                && EnumaElishVisualEntity.fadeThickness(fade) > 0.1F) {
            sweep(world, cast, actor, settings, false, fade, 1.0F);
        }
        return elapsed < end;
    }

    private static void releaseBeam(ServerWorld world, Cast cast, LivingEntity actor, EnumaElishVisualEntity visual,
                                    EnumaElishItem.EffectSettings settings) {
        Vec3d direction = aimDirection(world, cast, actor);
        Vec3d origin = actor.getEyePos().add(0, -0.35, 0).add(direction.multiply(1.6));

        double length = settings.beamRange;
        boolean hitTerrain = false;
        if (settings.stopAtBlocks) {
            // read only: finds where the beam ends, nothing is broken or changed
            BlockHitResult hit = world.raycast(new RaycastContext(origin, origin.add(direction.multiply(settings.beamRange)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actor));
            if (hit.getType() == HitResult.Type.BLOCK) {
                length = Math.max(1.0, hit.getPos().distanceTo(origin));
                hitTerrain = true;
            }
        }

        cast.stage = Stage.RELEASE;
        cast.origin = origin;
        cast.direction = direction;
        cast.length = length;
        cast.hitTerrain = hitTerrain;

        visual.aim(origin, direction, (float) length, hitTerrain);
        visual.setPhase(EnumaElishVisualEntity.PHASE_RELEASE, RELEASE_TICKS);
        actor.swingHand(cast.hand, true);
        playSound(world, origin, SoundRegistry.ENUMA_ELISH_RELEASE.get(), settings.releaseVolume, settings.releasePitch);
        world.spawnParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 2, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 40, 0.3, 0.3, 0.3, 0.45);
        world.spawnParticles(ParticlesRegistry.RUPTURE_EMBER.get(), origin.x, origin.y, origin.z, 30, 0.5, 0.5, 0.5, 0.6);
    }

    // Players: pitch and head yaw from the server copy of the player. In first person that is exactly the
    // camera. In third person it is where the character model is looking, a detached or free-look camera
    // never reaches the server so it can't pull the aim off. Mobs aim at their target.
    private static Vec3d aimDirection(ServerWorld world, Cast cast, LivingEntity actor) {
        if (!(actor instanceof PlayerEntity)) {
            LivingEntity target = cast.targetId != null && world.getEntity(cast.targetId) instanceof LivingEntity living
                    ? living : null;
            if (target != null && target.isAlive()) {
                Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(actor.getEyePos());
                if (to.lengthSquared() > 1.0E-4) {
                    return to.normalize();
                }
            }
        }
        return Vec3d.fromPolar(actor.getPitch(), actor.getHeadYaw()).normalize();
    }

    // blast: the big opening hit with knockback, once per target. Otherwise a lighter tick for anything still
    // inside the beam. fade and reach (both 0..1) shrink the hit area the same way the beam is drawn
    private static void sweep(ServerWorld world, Cast cast, LivingEntity actor, EnumaElishItem.EffectSettings settings,
                              boolean blast, float fade, float reach) {
        float radius = (float) settings.beamRadius;
        float thickness = EnumaElishVisualEntity.fadeThickness(fade);
        float edge = EnumaElishVisualEntity.EDGE * thickness;
        // 1.08 is the top of the muzzle's pulse
        float muzzle = EnumaElishVisualEntity.muzzleRadius(radius) * 1.08F * thickness;
        double length = cast.length * reach;
        boolean arrived = reach >= 1.0F;
        // the orange layer of the fireball, the red haze past it is faint
        double impact = Math.max(settings.impactRadius, EnumaElishVisualEntity.impactSize(radius, fade) * 1.4);
        Vec3d end = cast.end();
        float damage = blast
                ? SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                        settings.blastDamageScaling, settings.blastSpellScaling)
                : SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                        settings.tickDamageScaling, settings.tickSpellScaling);
        if (damage <= 0) {
            return;
        }

        // one box round a long diagonal beam covers a huge volume, so gather it a chunk at a time
        Set<Entity> found = new HashSet<>();
        found.addAll(world.getOtherEntities(actor, new Box(cast.origin, cast.origin).expand(muzzle + 2.0)));
        for (double from = 0; from < length; from += CHUNK) {
            double to = Math.min(length, from + CHUNK);
            Box area = new Box(cast.origin.add(cast.direction.multiply(from)), cast.origin.add(cast.direction.multiply(to)))
                    .expand(radius * edge + 2.0);
            found.addAll(world.getOtherEntities(actor, area));
        }
        if (cast.hitTerrain && arrived) {
            found.addAll(world.getOtherEntities(actor, new Box(end, end).expand(impact + 2.0)));
        }

        Set<Entity> struck = new HashSet<>();
        for (Entity entity : found) {
            Box box = entity.getBoundingBox();
            double raw = box.getCenter().subtract(cast.origin).dotProduct(cast.direction);
            double along = MathHelper.clamp(raw, 0.0, length);
            // past the front of the beam while it is still shooting out
            boolean inBeam = raw <= length + 0.5 && distanceToPoint(box, cast.origin.add(cast.direction.multiply(along)))
                    <= EnumaElishVisualEntity.profile(along, radius) * edge;
            boolean inMuzzle = distanceToPoint(box, cast.origin) <= muzzle;
            boolean inBlast = cast.hitTerrain && arrived && distanceToPoint(box, end) <= impact;
            if (!inBeam && !inMuzzle && !inBlast) {
                continue;
            }

            // multipart bosses: land the hit on the parent once
            Entity root = entity instanceof PartEntity<?> part ? part.getParent() : entity;
            if (root == null || root == actor || !struck.add(root)) {
                continue;
            }
            if (blast && !cast.blasted.add(root.getUuid())) {
                continue;
            }

            if (root instanceof EnderDragonEntity dragon) {
                // the head takes full damage, anywhere else the dragon quarters it
                if (dragon.isAlive() && canHitOwner(world, dragon, cast.ownerId)) {
                    dragon.damagePart(dragon.head, SimplySwordsAPI.getWeaponDamageSource(actor), damage);
                    if (blast) {
                        burst(world, dragon.head.getPos());
                    }
                }
                continue;
            }

            if (root instanceof LivingEntity target && canHit(world, target, actor, cast.ownerId)) {
                if (SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, actor, cast.stack, target, damage,
                        SpellScalingProfile.ARCANE) && blast) {
                    Vec3d push = cast.direction.multiply(settings.beamKnockback);
                    target.addVelocity(push.x, push.y * 0.5 + 0.25 * settings.beamKnockback, push.z);
                    target.velocityModified = true;
                    burst(world, target.getPos().add(0, target.getHeight() * 0.5, 0));
                }
            } else if (!(root instanceof LivingEntity) && entity instanceof PartEntity<?> part && part.getParent() != actor) {
                part.damage(SimplySwordsAPI.getWeaponDamageSource(actor), damage);
            }
        }
    }

    private static void burst(ServerWorld world, Vec3d at) {
        world.spawnParticles(ParticlesRegistry.RUPTURE_EMBER.get(), at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.35);
        world.spawnParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private static double distanceToPoint(Box box, Vec3d point) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean canHit(ServerWorld world, LivingEntity target, LivingEntity actor, @Nullable UUID ownerId) {
        if (target == actor || !target.isAlive() || !SimplySwordsAPI.isValidAbilityTarget(target, actor)) {
            return false;
        }
        return canHitOwner(world, target, ownerId);
    }

    private static boolean canHitOwner(ServerWorld world, LivingEntity target, @Nullable UUID ownerId) {
        if (ownerId == null) {
            return true;
        }
        Entity owner = world.getEntity(ownerId);
        return !(owner instanceof LivingEntity livingOwner)
                || target != livingOwner && SimplySwordsAPI.isValidAbilityTarget(target, livingOwner);
    }

    private static boolean holdingSword(LivingEntity actor) {
        return actor.getMainHandStack().getItem() instanceof EnumaElishItem
                || actor.getOffHandStack().getItem() instanceof EnumaElishItem;
    }

    private static void playSound(ServerWorld world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        if (volume > 0) {
            world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    private static void end(ServerWorld world, Cast cast) {
        Entity visual = world.getEntity(cast.visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void tick(ServerWorld world) {
        Map<UUID, Cast> casts = CASTS.get(world);
        if (casts == null || casts.isEmpty()) {
            return;
        }
        Iterator<Cast> iterator = casts.values().iterator();
        while (iterator.hasNext()) {
            Cast cast = iterator.next();
            if (!tickCast(world, cast)) {
                end(world, cast);
                iterator.remove();
            }
        }
    }

    private static void clear(ServerWorld world) {
        Map<UUID, Cast> casts = CASTS.remove(world);
        if (casts != null) {
            casts.values().forEach(cast -> end(world, cast));
        }
    }

    private static void clearActor(LivingEntity actor) {
        for (Map.Entry<ServerWorld, Map<UUID, Cast>> entry : CASTS.entrySet()) {
            Cast cast = entry.getValue().remove(actor.getUuid());
            if (cast != null) {
                end(entry.getKey(), cast);
            }
        }
    }

    private enum Stage {
        CHARGE, RELEASE, BEAM, FADE
    }

    private static final class Cast {
        final UUID actorId;
        @Nullable final UUID ownerId;
        final ItemStack stack;
        final Hand hand;
        final long startTime;
        final UUID visualId;
        @Nullable final UUID targetId;
        Stage stage = Stage.CHARGE;
        Vec3d origin = Vec3d.ZERO;
        Vec3d direction = new Vec3d(0, 0, 1);
        double length;
        boolean hitTerrain;
        final Set<UUID> blasted = new HashSet<>();

        Cast(UUID actorId, @Nullable UUID ownerId, ItemStack stack, Hand hand, long startTime, UUID visualId,
             @Nullable UUID targetId) {
            this.actorId = actorId;
            this.ownerId = ownerId;
            this.stack = stack;
            this.hand = hand;
            this.startTime = startTime;
            this.visualId = visualId;
            this.targetId = targetId;
        }

        Vec3d end() {
            return this.origin.add(this.direction.multiply(this.length));
        }
    }
}
