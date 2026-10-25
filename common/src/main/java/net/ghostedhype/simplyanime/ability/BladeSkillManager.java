package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.ghostedhype.simplyanime.callout.Callouts;
import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.config.DarkRepulserSettings;
import net.ghostedhype.simplyanime.config.ElucidatorSettings;
import net.ghostedhype.simplyanime.config.StarburstSettings;
import net.ghostedhype.simplyanime.entity.BladeSkillEntity;
import net.ghostedhype.simplyanime.item.DarkRepulserItem;
import net.ghostedhype.simplyanime.item.ElucidatorItem;
import net.ghostedhype.simplyanime.platform.Services;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.ghostedhype.simplyanime.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BladeSkillManager {

    private static final Map<ServerLevel, Map<UUID, BladeSkillCast>> CASTS = new HashMap<>();

    private static final int AFTERGLOW = 50;
    private static final double CHUNK = 16.0;

    @Nullable
    public static BladeSkill skillFor(LivingEntity actor, @Nullable InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            return null;
        }
        Item main = actor.getMainHandItem().getItem();
        if (main instanceof ElucidatorItem) {
            return actor.getOffhandItem().getItem() instanceof DarkRepulserItem ? BladeSkill.STARBURST_STREAM : BladeSkill.VORPAL_STRIKE;
        }
        return main instanceof DarkRepulserItem ? BladeSkill.HORIZONTAL_SQUARE : null;
    }

    @Nullable
    private static BladeSkillCast active(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel world)) {
            return null;
        }
        Map<UUID, BladeSkillCast> casts = CASTS.get(world);
        BladeSkillCast cast = casts == null ? null : casts.get(entity.getUUID());
        return cast == null || cast.finished ? null : cast;
    }

    public static boolean isBusy(LivingEntity actor) {
        return active(actor) != null;
    }

    // no basic attacks in the middle of a skill
    public static boolean blocksAttack(LivingEntity attacker) {
        return isBusy(attacker);
    }

    public static float scaleDamage(LivingEntity target, DamageSource source, float amount) {
        BladeSkillCast cast = active(target);
        if (cast == null || cast.skill != BladeSkill.STARBURST_STREAM || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        return amount * Config.weapons.starburst_stream.damageTaken;
    }

    public static int cooldown(BladeSkill skill) {
        return switch (skill) {
            case VORPAL_STRIKE -> Config.weapons.elucidator.cooldown;
            case HORIZONTAL_SQUARE -> Config.weapons.dark_repulser.cooldown;
            case STARBURST_STREAM -> Config.weapons.starburst_stream.cooldown;
        };
    }

    public static boolean start(WeaponAbilityContext context) {
        ServerLevel world = context.world();
        LivingEntity actor = context.actor();
        BladeSkill skill = skillFor(actor, context.hand());
        if (skill == null) {
            return false;
        }
        boolean mirror = actor.getMainArm() == HumanoidArm.LEFT;
        AnimationClip clip = skill.clip();
        BladeSkillEntity visual = new BladeSkillEntity(world, actor, skill, mirror, clip.lengthTicks() + AFTERGLOW);
        world.addFreshEntity(visual);

        BladeSkillCast cast = new BladeSkillCast(actor.getUUID(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUUID(),
                context.target() == null ? null : context.target().getUUID(),
                context.stack().copy(), skill, world.getGameTime(), visual.getUUID(), mirror);
        cast.yaw = actor.getYHeadRot();
        cast.center = actor.position();
        CASTS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUUID(), cast);

        switch (skill) {
            case VORPAL_STRIKE -> {
                slow(actor, skill.tick("lunge_start"), 3);
                sound(world, actor.position(), SoundRegistry.VORPAL_CHARGE.get(), Config.weapons.elucidator.volume, 1.0F);
            }
            case HORIZONTAL_SQUARE -> {
                slow(actor, clip.lengthTicks(), 6);
                cast.anchored = true;
                visual.aim(AbilityHelper.look(actor), cast.yaw);
                if (Config.weapons.dark_repulser.nameText) {
                    Callouts.send(world, actor.position(), skill.callout);
                }
            }
            case STARBURST_STREAM -> {
                StarburstSettings settings = Config.weapons.starburst_stream;
                slow(actor, clip.lengthTicks(), 1);
                // the other sword rests for as long as this one
                SimplySwordsAPI.setWeaponCooldown(actor, actor.getOffhandItem(), settings.cooldown);
                sound(world, actor.position(), SoundRegistry.STARBURST_STANCE.get(), settings.volume, 1.0F);
                if (settings.nameText) {
                    Callouts.send(world, actor.position(), skill.callout);
                }
            }
        }
        return true;
    }

    private static boolean tickCast(ServerLevel world, BladeSkillCast cast) {
        LivingEntity actor = world.getEntity(cast.actorId) instanceof LivingEntity living && living.isAlive() ? living : null;
        if (!cast.finished) {
            if (actor == null || skillFor(actor, InteractionHand.MAIN_HAND) != cast.skill) {
                // dead, gone, or put a sword away part way through
                abort(world, cast, actor);
                return false;
            }
            int tick = (int) (world.getGameTime() - cast.startTime);
            if (!cast.anchored && world.getEntity(cast.visualId) instanceof BladeSkillEntity visual) {
                visual.setPos(actor.getX(), actor.getY(), actor.getZ());
            }
            switch (cast.skill) {
                case VORPAL_STRIKE -> tickVorpal(world, cast, actor, tick);
                case HORIZONTAL_SQUARE -> tickSquare(world, cast, actor, tick);
                case STARBURST_STREAM -> tickStarburst(world, cast, actor, tick);
            }
            cast.finished = tick >= cast.skill.clip().lengthTicks();
        }
        if (actor != null) {
            tickWaves(world, cast, actor);
        }
        return actor != null && (!cast.finished || !cast.waves.isEmpty());
    }

    private static void tickVorpal(ServerLevel world, BladeSkillCast cast, LivingEntity actor, int tick) {
        ElucidatorSettings settings = Config.weapons.elucidator;
        BladeSkill skill = cast.skill;
        int lunge = skill.tick("lunge_start");
        int lungeEnd = skill.tick("lunge_end");
        if (tick == skill.tick("charge_start") && settings.nameText) {
            Callouts.send(world, actor.position(), skill.callout);
        }
        if (tick == lunge) {
            cast.aim = aim(world, cast, actor);
            cast.yaw = actor.getYHeadRot();
            if (world.getEntity(cast.visualId) instanceof BladeSkillEntity visual) {
                visual.aim(cast.aim, cast.yaw);
            }
            sound(world, actor.position(), SoundRegistry.VORPAL_STRIKE.get(), settings.volume, 1.0F);
        }
        if (tick >= lunge && tick < lungeEnd) {
            Vec3 flat = new Vec3(cast.aim.x, 0, cast.aim.z);
            if (flat.lengthSqr() > 1.0E-4) {
                Vec3 step = flat.normalize().scale(settings.dashDistance / (lungeEnd - lunge));
                actor.setDeltaMovement(step.x, Math.min(actor.getDeltaMovement().y, 0.0), step.z);
                actor.hurtMarked = true;
            }
        } else if (tick == lungeEnd) {
            actor.setDeltaMovement(0.0, Math.min(actor.getDeltaMovement().y, 0.0), 0.0);
            actor.hurtMarked = true;
        }
        if (tick == skill.tick("impact")) {
            cast.anchored = true;
            if (world.getEntity(cast.visualId) instanceof BladeSkillEntity visual) {
                visual.setPos(actor.getX(), actor.getY(), actor.getZ());
            }
            lance(world, cast, actor, settings);
        }
    }

    private static void lance(ServerLevel world, BladeSkillCast cast, LivingEntity actor, ElucidatorSettings settings) {
        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                settings.damageScaling, settings.spellScaling);
        if (damage <= 0) {
            return;
        }
        Vec3 origin = BladeSkillShapes.lanceOrigin(cast.skill.clip(), actor.position(), cast.yaw, cast.mirror);
        double length = settings.lanceLength;
        double radius = settings.lanceRadius;
        Set<Entity> found = new HashSet<>();
        for (double from = 0; from < length; from += CHUNK) {
            double to = Math.min(length, from + CHUNK);
            found.addAll(world.getEntities(actor, new AABB(origin.add(cast.aim.scale(from)), origin.add(cast.aim.scale(to)))
                    .inflate(radius + 2.0)));
        }
        Vec3 push = cast.aim.scale(settings.knockback);
        for (Entity entity : found) {
            AABB box = entity.getBoundingBox();
            double along = Mth.clamp(box.getCenter().subtract(origin).dot(cast.aim), 0.0, length);
            if (AbilityHelper.distanceToPoint(box, origin.add(cast.aim.scale(along))) <= radius) {
                land(world, cast, actor, entity, cast.struck, damage, new Vec3(push.x, push.y * 0.5 + 0.25 * settings.knockback, push.z),
                        ParticlesRegistry.BLADE_SPARK_CRIMSON.get());
            }
        }
    }

    private static void tickSquare(ServerLevel world, BladeSkillCast cast, LivingEntity actor, int tick) {
        DarkRepulserSettings settings = Config.weapons.dark_repulser;
        BladeSkill skill = cast.skill;
        for (int side = 1; side <= 4; side++) {
            if (tick == skill.tick("slash_" + side)) {
                SlashWave wave = BladeSkillShapes.squareSide(skill.clip(), side, cast.center, cast.yaw, cast.mirror,
                        (float) settings.waveRange);
                wave.damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                        settings.cutDamageScaling, settings.cutSpellScaling);
                wave.knockback = settings.knockback * 0.4;
                cast.waves.add(wave);
                sound(world, actor.position(), SoundRegistry.SQUARE_SLASH.get(), settings.volume, 0.9F + side * 0.07F);
            }
        }
        if (tick == skill.tick("release")) {
            cast.releaseTick = tick;
            cast.struck.clear();
            sound(world, actor.position(), SoundRegistry.SQUARE_RELEASE.get(), settings.volume, 1.0F);
        }
        if (cast.releaseTick >= 0 && tick - cast.releaseTick <= BladeSkillShapes.RELEASE_TICKS) {
            release(world, cast, actor, settings, tick - cast.releaseTick);
        }
    }

    private static void release(ServerLevel world, BladeSkillCast cast, LivingEntity actor, DarkRepulserSettings settings, int ticks) {
        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                settings.releaseDamageScaling, settings.releaseSpellScaling);
        if (damage <= 0) {
            return;
        }
        float half = BladeSkillShapes.releaseHalf(ticks, (float) settings.releaseRadius);
        float angle = cast.yaw * Mth.DEG_TO_RAD;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        for (Entity entity : world.getEntities(actor, new AABB(cast.center, cast.center).inflate(half * 1.5 + 2.0, 4.0, half * 1.5 + 2.0))) {
            Vec3 entityPosition = entity.getBoundingBox().getCenter().subtract(cast.center);
            double x = entityPosition.x * cos + entityPosition.z * sin;
            double z = -entityPosition.x * sin + entityPosition.z * cos;
            double reach = Math.max(Math.abs(x), Math.abs(z)) - entity.getBbWidth() * 0.5;
            if (reach <= half + 0.6 && Math.abs(entityPosition.y - 1.0) <= 3.0) {
                Vec3 out = new Vec3(entityPosition.x, 0, entityPosition.z);
                Vec3 push = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(settings.knockback);
                land(world, cast, actor, entity, cast.struck, damage, push.add(0, 0.3 * settings.knockback, 0),
                        ParticlesRegistry.BLADE_SPARK_FROST.get());
            }
        }
    }

    private static void tickStarburst(ServerLevel world, BladeSkillCast cast, LivingEntity actor, int tick) {
        StarburstSettings settings = Config.weapons.starburst_stream;
        AnimationClip clip = cast.skill.clip();
        int frame = Math.round(tick * clip.framesPerTick());
        int last = BladeSkillShapes.hitFrame(clip, BladeSkillShapes.hitCount(clip));
        Vec3 look = AbilityHelper.look(actor);
        if (frame == clip.marker("coil")) {
            sound(world, actor.position(), SoundRegistry.STARBURST_COIL.get(), settings.volume, 1.0F);
        }
        for (AnimationClip.Marker marker : clip.markers()) {
            if (!BladeSkillShapes.isHit(marker) || marker.frame() != frame) {
                continue;
            }
            if (marker.frame() == last) {
                cross(world, cast, actor, settings, look);
                continue;
            }
            boolean dark = BladeSkillShapes.uses(marker, 'R');
            float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                    settings.hitDamageScaling, settings.hitSpellScaling);
            if (damage > 0) {
                // each cut lands once on everything inside the flurry
                Set<UUID> struck = new HashSet<>();
                ParticleOptions spark = dark ? ParticlesRegistry.BLADE_SPARK_STAR.get() : ParticlesRegistry.BLADE_SPARK_FROST.get();
                for (Entity entity : world.getEntities(actor, new AABB(actor.position(), actor.position())
                        .inflate(settings.reach * 1.4 + 2.0))) {
                    if (BladeSkillShapes.inFlurry(actor.position(), look, entity.getBoundingBox(), settings.reach)) {
                        land(world, cast, actor, entity, struck, damage, Vec3.ZERO, spark);
                    }
                }
            }
            sound(world, actor.position(), dark ? SoundRegistry.STARBURST_SLASH_DARK.get() : SoundRegistry.STARBURST_SLASH_LIGHT.get(),
                    settings.volume, 0.9F + world.getRandom().nextFloat() * 0.25F);
        }
    }

    private static void cross(ServerLevel world, BladeSkillCast cast, LivingEntity actor, StarburstSettings settings, Vec3 look) {
        sound(world, actor.position(), SoundRegistry.STARBURST_FINALE.get(), settings.volume * 1.5F, 1.0F);
        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, cast.stack,
                settings.finalDamageScaling, settings.finalSpellScaling);
        if (damage <= 0) {
            return;
        }
        Vec3 center = BladeSkillShapes.crossCenter(actor.getEyePosition(), look);
        Vec3[] bars = BladeSkillShapes.crossBars(look);
        double far = Math.max(settings.crossReach, settings.burstRadius);
        cast.struck.clear();
        for (Entity entity : world.getEntities(actor, new AABB(center, center).inflate(far + 2.0))) {
            if (BladeSkillShapes.inCross(center, bars, entity.getBoundingBox(), settings.crossReach, settings.burstRadius)) {
                Vec3 out = entity.getBoundingBox().getCenter().subtract(actor.getEyePosition());
                Vec3 push = out.lengthSqr() < 1.0E-4 ? look : out.normalize();
                land(world, cast, actor, entity, cast.struck, damage, push.scale(settings.finalKnockback).add(0, 0.35, 0),
                        ParticlesRegistry.BLADE_SPARK_STAR.get());
            }
        }
    }

    private static void tickWaves(ServerLevel world, BladeSkillCast cast, LivingEntity actor) {
        Iterator<SlashWave> iterator = cast.waves.iterator();
        while (iterator.hasNext()) {
            SlashWave wave = iterator.next();
            if (wave.damage > 0) {
                Vec3 wavePosition = wave.centerAt(wave.age);
                double reach = wave.halfAt(wave.age) + wave.thickness + wave.speed + 2.0;
                Vec3 push = wave.direction.scale(wave.knockback).add(0, 0.2 * wave.knockback, 0);
                for (Entity entity : world.getEntities(actor, new AABB(wavePosition, wavePosition).inflate(reach))) {
                    AABB box = entity.getBoundingBox();
                    if (wave.touches(box, wave.age) || wave.age > 0 && wave.touches(box, wave.age - 0.5F)) {
                        land(world, cast, actor, entity, wave.struck, wave.damage, push, ParticlesRegistry.BLADE_SPARK_FROST.get());
                    }
                }
            }
            wave.age++;
            if (wave.spent(wave.age)) {
                iterator.remove();
            }
        }
    }

    private static void land(ServerLevel world, BladeSkillCast cast, LivingEntity actor, Entity entity, Set<UUID> struck,
                             float damage, Vec3 push, ParticleOptions spark) {
        Entity root = Services.PLATFORM.getParentPart(entity);
        if (root == null || root == actor || !struck.add(root.getUUID())) {
            return;
        }
        Vec3 strikePosition = AbilityHelper.strike(world, actor, cast.ownerId, cast.stack, entity, root, damage);
        if (strikePosition == null) {
            return;
        }
        if (root instanceof LivingEntity) {
            root.push(push.x, push.y, push.z);
            root.hurtMarked = true;
        }
        world.sendParticles(spark, strikePosition.x, strikePosition.y, strikePosition.z, 10, 0.3, 0.3, 0.3, 0.3);
        sound(world, strikePosition, SoundRegistry.BLADE_HIT.get(), 1.0F, 0.9F + world.getRandom().nextFloat() * 0.3F);
    }

    private static Vec3 aim(ServerLevel world, BladeSkillCast cast, LivingEntity actor) {
        if (!(actor instanceof Player) && cast.targetId != null && world.getEntity(cast.targetId) instanceof LivingEntity target
                && target.isAlive()) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(actor.getEyePosition());
            if (to.lengthSqr() > 1.0E-4) {
                return to.normalize();
            }
        }
        return AbilityHelper.look(actor);
    }

    private static void slow(LivingEntity actor, int ticks, int level) {
        if (actor instanceof Player) {
            return;
        }
        actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level, false, false, false));
    }

    private static void sound(ServerLevel world, Vec3 position, SoundEvent sound, float volume, float pitch) {
        AbilityHelper.playSound(world, position, sound, volume, pitch);
    }

    private static void abort(ServerLevel world, BladeSkillCast cast, @Nullable LivingEntity actor) {
        cast.finished = true;
        cast.waves.clear();
        if (actor != null) {
            actor.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        Entity visual = world.getEntity(cast.visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    public static void tick(ServerLevel world) {
        Map<UUID, BladeSkillCast> casts = CASTS.get(world);
        if (casts == null || casts.isEmpty()) {
            return;
        }
        casts.values().removeIf(cast -> !tickCast(world, cast));
    }

    public static void clear(ServerLevel world) {
        Map<UUID, BladeSkillCast> casts = CASTS.remove(world);
        if (casts != null) {
            casts.values().forEach(cast -> abort(world, cast, null));
        }
    }

    public static void clearAll() {
        CASTS.clear();
    }

    public static void clearActor(LivingEntity actor) {
        for (Map.Entry<ServerLevel, Map<UUID, BladeSkillCast>> entry : CASTS.entrySet()) {
            BladeSkillCast cast = entry.getValue().remove(actor.getUUID());
            if (cast != null) {
                abort(entry.getKey(), cast, null);
            }
        }
    }
}
