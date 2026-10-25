package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.callout.Callout;
import net.ghostedhype.simplyanime.callout.Callouts;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.config.EaSettings;
import net.ghostedhype.simplyanime.entity.EaVisualEntity;
import net.ghostedhype.simplyanime.item.EaItem;
import net.ghostedhype.simplyanime.platform.Services;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.ghostedhype.simplyanime.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;

import java.util.*;

public class EaAbilityManager {

    private static final Map<ServerLevel, Map<UUID, EaCast>> CASTS = new HashMap<>();

    private static final int RELEASE_TICKS = 4;
    private static final int FADE_TICKS = 16;
    private static final double CHUNK = 16.0;

    public static boolean isBusy(ServerLevel world, LivingEntity actor) {
        Map<UUID, EaCast> casts = CASTS.get(world);
        return casts != null && casts.containsKey(actor.getUUID());
    }

    public static boolean isCharging(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel world)) {
            return false;
        }
        Map<UUID, EaCast> casts = CASTS.get(world);
        EaCast cast = casts == null ? null : casts.get(entity.getUUID());
        return cast != null && cast.stage == EaCast.Stage.CHARGE;
    }

    public static boolean blocksDamage(LivingEntity entity, DamageSource source) {
        return isCharging(entity) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    // Ea has no basic attacks, Enuma Elish is all it does
    public static boolean blocksAttack(LivingEntity attacker) {
        return attacker.getMainHandItem().getItem() instanceof EaItem;
    }

    public static boolean start(WeaponAbilityContext context) {
        ServerLevel world = context.world();
        LivingEntity actor = context.actor();
        EaSettings settings = Config.weapons.ea;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUUID();

        EaVisualEntity visual = new EaVisualEntity(world, actor, settings.chargeDuration,
                (float) settings.beamRadius);
        world.addFreshEntity(visual);

        if (settings.chargeSlowness >= 0) {
            actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    settings.chargeDuration + RELEASE_TICKS + settings.beamDuration, settings.chargeSlowness,
                    false, false, false));
        }
        AbilityHelper.playSound(world, actor.position(), SoundRegistry.EA_CHARGE.get(), settings.chargeVolume, settings.chargePitch);

        EaCast cast = new EaCast(actor.getUUID(), ownerId, context.stack().copy(),
                context.hand() == null ? InteractionHand.MAIN_HAND : context.hand(), world.getGameTime(), visual.getUUID(),
                context.target() == null ? null : context.target().getUUID());
        CASTS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUUID(), cast);
        return true;
    }

    private static boolean tickCast(ServerLevel world, EaCast cast) {
        EaSettings settings = Config.weapons.ea;
        if (!(world.getEntity(cast.actorId) instanceof LivingEntity actor) || !actor.isAlive()
                || !(world.getEntity(cast.visualId) instanceof EaVisualEntity visual)) {
            return false;
        }

        int elapsed = (int) (world.getGameTime() - cast.startTime);
        int release = settings.chargeDuration;
        int beamStart = release + RELEASE_TICKS;
        int fadeStart = beamStart + settings.beamDuration;
        int end = fadeStart + FADE_TICKS;

        if (cast.stage == EaCast.Stage.CHARGE) {
            if (!holdingSword(actor)) {
                actor.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                return false;
            }
            visual.setPos(actor.getX(), actor.getY(), actor.getZ());
            if (settings.nameText && elapsed == Math.max(0, release - settings.nameTextLead)) {
                Callouts.send(world, actor.position(), Callout.EA);
            }
            if (elapsed % 4 == 0) {
                float tickProgress = elapsed / (float) Math.max(1, release);
                world.sendParticles(ParticleTypes.FLAME, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                        2 + (int) (tickProgress * 6), 1.4, 0.05, 1.4, 0.01);
            }
            if (elapsed >= release) {
                releaseBeam(world, cast, actor, visual, settings);
            }
            return true;
        }

        if (cast.stage == EaCast.Stage.RELEASE) {
            if (elapsed >= beamStart) {
                cast.stage = EaCast.Stage.BEAM;
                visual.setPhase(EaVisualEntity.PHASE_BEAM, settings.beamDuration);
                AbilityHelper.playSound(world, cast.origin, SoundRegistry.EA_BEAM.get(), settings.beamVolume, settings.beamPitch);
                if (cast.hitTerrain) {
                    AbilityHelper.playSound(world, cast.end(), SoundRegistry.EA_BEAM.get(), settings.beamVolume * 0.6F,
                            settings.beamPitch * 0.85F);
                }
            }
            return true;
        }

        if (cast.stage == EaCast.Stage.BEAM) {
            int beamTicks = elapsed - beamStart;
            float reach = EaVisualEntity.growth(beamTicks);
            if (beamTicks <= EaVisualEntity.GROW_TICKS) {
                sweep(world, cast, actor, settings, true, 0.0F, reach);
            }
            if (beamTicks > 0 && beamTicks % Math.max(1, settings.tickInterval) == 0) {
                sweep(world, cast, actor, settings, false, 0.0F, 1.0F);
            }
            if (cast.hitTerrain && beamTicks == (int) EaVisualEntity.GROW_TICKS) {
                Vec3 impact = cast.end();
                world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
            }
            if (elapsed >= fadeStart) {
                cast.stage = EaCast.Stage.FADE;
                visual.setPhase(EaVisualEntity.PHASE_FADE, FADE_TICKS);
                Vec3 impact = cast.end();
                AbilityHelper.playSound(world, impact, SoundRegistry.EA_IMPACT.get(), settings.impactVolume, settings.impactPitch);
                if (impact.distanceToSqr(actor.position()) > 24 * 24) {
                    AbilityHelper.playSound(world, actor.position(), SoundRegistry.EA_IMPACT.get(), settings.impactVolume * 0.5F,
                            settings.impactPitch);
                }
            }
            return true;
        }

        float fade = (elapsed - fadeStart) / (float) FADE_TICKS;
        if (elapsed > fadeStart && (elapsed - beamStart) % Math.max(1, settings.tickInterval) == 0
                && EaVisualEntity.fadeThickness(fade) > 0.1F) {
            sweep(world, cast, actor, settings, false, fade, 1.0F);
        }
        return elapsed < end;
    }

    private static void releaseBeam(ServerLevel world, EaCast cast, LivingEntity actor, EaVisualEntity visual,
                                    EaSettings settings) {
        Vec3 direction = aimDirection(world, cast, actor);
        Vec3 origin = actor.getEyePosition().add(0, -0.35, 0).add(direction.scale(1.6));

        double length = settings.beamRange;
        boolean hitTerrain = false;
        if (settings.stopAtBlocks) {
            BlockHitResult hit = world.clip(new ClipContext(origin, origin.add(direction.scale(settings.beamRange)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
            if (hit.getType() == HitResult.Type.BLOCK) {
                length = Math.max(1.0, hit.getLocation().distanceTo(origin));
                hitTerrain = true;
            }
        }

        cast.stage = EaCast.Stage.RELEASE;
        cast.origin = origin;
        cast.direction = direction;
        cast.length = length;
        cast.hitTerrain = hitTerrain;

        visual.aim(origin, direction, (float) length, hitTerrain);
        visual.setPhase(EaVisualEntity.PHASE_RELEASE, RELEASE_TICKS);
        actor.swing(cast.hand, true);
        AbilityHelper.playSound(world, origin, SoundRegistry.EA_RELEASE.get(), settings.releaseVolume, settings.releasePitch);
        world.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 2, 0, 0, 0, 0);
        world.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 40, 0.3, 0.3, 0.3, 0.45);
        world.sendParticles(ParticlesRegistry.RUPTURE_EMBER.get(), origin.x, origin.y, origin.z, 30, 0.5, 0.5, 0.5, 0.6);
    }

    private static Vec3 aimDirection(ServerLevel world, EaCast cast, LivingEntity actor) {
        if (!(actor instanceof Player)) {
            LivingEntity target = cast.targetId != null && world.getEntity(cast.targetId) instanceof LivingEntity living
                    ? living : null;
            if (target != null && target.isAlive()) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(actor.getEyePosition());
                if (to.lengthSqr() > 1.0E-4) {
                    return to.normalize();
                }
            }
        }
        return Vec3.directionFromRotation(actor.getXRot(), actor.getYHeadRot()).normalize();
    }

    private static void sweep(ServerLevel world, EaCast cast, LivingEntity actor, EaSettings settings,
                              boolean blast, float fade, float reach) {
        float radius = (float) settings.beamRadius;
        float thickness = EaVisualEntity.fadeThickness(fade);
        float edge = EaVisualEntity.EDGE * thickness;
        // 1.08 is the top of the muzzle's pulse
        float muzzle = EaVisualEntity.muzzleRadius(radius) * 1.08F * thickness;
        double length = cast.length * reach;
        boolean arrived = reach >= 1.0F;
        double impact = Math.max(settings.impactRadius, EaVisualEntity.impactSize(radius, fade) * 1.4);
        Vec3 end = cast.end();
        float damage = blast
                ? SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                        settings.blastDamageScaling, settings.blastSpellScaling)
                : SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                        settings.tickDamageScaling, settings.tickSpellScaling);
        if (damage <= 0) {
            return;
        }

        Set<Entity> found = new HashSet<>();
        found.addAll(world.getEntities(actor, new AABB(cast.origin, cast.origin).inflate(muzzle + 2.0)));
        for (double from = 0; from < length; from += CHUNK) {
            double to = Math.min(length, from + CHUNK);
            AABB area = new AABB(cast.origin.add(cast.direction.scale(from)), cast.origin.add(cast.direction.scale(to)))
                    .inflate(radius * edge + 2.0);
            found.addAll(world.getEntities(actor, area));
        }
        if (cast.hitTerrain && arrived) {
            found.addAll(world.getEntities(actor, new AABB(end, end).inflate(impact + 2.0)));
        }

        Set<Entity> struck = new HashSet<>();
        for (Entity entity : found) {
            AABB box = entity.getBoundingBox();
            double raw = box.getCenter().subtract(cast.origin).dot(cast.direction);
            double along = Mth.clamp(raw, 0.0, length);
            boolean inBeam = raw <= length + 0.5 && AbilityHelper.distanceToPoint(box, cast.origin.add(cast.direction.scale(along)))
                    <= EaVisualEntity.profile(along, radius) * edge;
            boolean inMuzzle = AbilityHelper.distanceToPoint(box, cast.origin) <= muzzle;
            boolean inBlast = cast.hitTerrain && arrived && AbilityHelper.distanceToPoint(box, end) <= impact;
            if (!inBeam && !inMuzzle && !inBlast) {
                continue;
            }

            // multipart bosses: land the hit on the parent once
            Entity root = Services.PLATFORM.getParentPart(entity);
            if (root == null || root == actor || !struck.add(root)) {
                continue;
            }
            if (blast && !cast.blasted.add(root.getUUID())) {
                continue;
            }

            Vec3 strikePosition = AbilityHelper.strike(world, actor, cast.ownerId, cast.stack, entity, root, damage);
            if (strikePosition != null && blast) {
                if (!(root instanceof EnderDragon)) {
                    Vec3 push = cast.direction.scale(settings.beamKnockback);
                    root.push(push.x, push.y * 0.5 + 0.25 * settings.beamKnockback, push.z);
                    root.hurtMarked = true;
                }
                burst(world, strikePosition);
            }
        }
    }

    private static void burst(ServerLevel world, Vec3 position) {
        world.sendParticles(ParticlesRegistry.RUPTURE_EMBER.get(), position.x, position.y, position.z, 16, 0.4, 0.4, 0.4, 0.35);
        world.sendParticles(ParticleTypes.FLASH, position.x, position.y, position.z, 1, 0, 0, 0, 0);
    }

    private static boolean holdingSword(LivingEntity actor) {
        return actor.getMainHandItem().getItem() instanceof EaItem
                || actor.getOffhandItem().getItem() instanceof EaItem;
    }

    private static void end(ServerLevel world, EaCast cast) {
        Entity visual = world.getEntity(cast.visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    public static void tick(ServerLevel world) {
        Map<UUID, EaCast> casts = CASTS.get(world);
        if (casts == null || casts.isEmpty()) {
            return;
        }
        Iterator<EaCast> iterator = casts.values().iterator();
        while (iterator.hasNext()) {
            EaCast cast = iterator.next();
            if (!tickCast(world, cast)) {
                end(world, cast);
                iterator.remove();
            }
        }
    }

    public static void clear(ServerLevel world) {
        Map<UUID, EaCast> casts = CASTS.remove(world);
        if (casts != null) {
            casts.values().forEach(cast -> end(world, cast));
        }
    }

    public static void clearAll() {
        CASTS.clear();
    }

    public static void clearActor(LivingEntity actor) {
        for (Map.Entry<ServerLevel, Map<UUID, EaCast>> entry : CASTS.entrySet()) {
            EaCast cast = entry.getValue().remove(actor.getUUID());
            if (cast != null) {
                end(entry.getKey(), cast);
            }
        }
    }
}
