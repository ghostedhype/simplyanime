package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.config.Config;
import net.ghostedhype.simplyanime.config.InvertedSpearSettings;
import net.ghostedhype.simplyanime.entity.HeavenChainVisualEntity;
import net.ghostedhype.simplyanime.entity.HeavenMarkVisualEntity;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.ghostedhype.simplyanime.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InvertedSpearAbilityManager {

    private static final Map<ServerLevel, Map<UUID, SlashField>> FIELDS = new HashMap<>();
    private static final Map<ServerLevel, Map<UUID, TwinStrike>> STRIKES = new HashMap<>();

    private static final double DASH_STEP = 0.25;
    private static final double LEDGE_DROP = 1.25;
    private static final double STOP_SHORT_OF_TARGET = 1.2;
    private static final int SLASH_TICKS = 6;

    public static boolean wantsTwinStrike(WeaponAbilityContext context) {
        if (context == null || context.actor() == null) {
            return false;
        }
        if (context.actor() instanceof Player player) {
            return !player.isShiftKeyDown();
        }
        // mobs can't sneak, dash if the target is far
        LivingEntity target = context.target();
        return target != null && context.actor().distanceToSqr(target) > 3.5 * 3.5;
    }

    public static boolean isBusy(ServerLevel world, LivingEntity actor) {
        Map<UUID, SlashField> fields = FIELDS.get(world);
        Map<UUID, TwinStrike> strikes = STRIKES.get(world);
        return (fields != null && fields.containsKey(actor.getUUID()))
                || (strikes != null && strikes.containsKey(actor.getUUID()));
    }

    // Heavenly Slash Field (sneak + right click)

    public static boolean startSlashField(WeaponAbilityContext context) {
        ServerLevel world = context.world();
        LivingEntity actor = context.actor();
        InvertedSpearSettings settings = Config.weapons.inverted_spear_of_heaven;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUUID();

        LivingEntity lockTarget = findLockTarget(world, actor, ownerId, context.target(), settings.lockOnRange);

        HeavenChainVisualEntity chain = new HeavenChainVisualEntity(world, actor, context.stack(),
                settings.fieldSpinDuration, (float) settings.fieldRadius);
        world.addFreshEntity(chain);

        HeavenMarkVisualEntity crack = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_CRACK,
                actor.position().add(0, 0.02, 0), settings.fieldWindup + 22, (float) settings.fieldRadius * 0.6F);
        world.addFreshEntity(crack);

        HeavenMarkVisualEntity lockOn = null;
        if (lockTarget != null) {
            lockOn = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_LOCK_ON, actor.position(),
                    settings.fieldWindup + settings.fieldSpinDuration + settings.flingDuration + 2, lockTarget.getBbWidth());
            lockOn.setTargetId(lockTarget.getId());
            world.addFreshEntity(lockOn);
        }

        playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 1.0F, 1.0F);
        world.sendParticles(ParticleTypes.SMOKE, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                12, 0.6, 0.02, 0.6, 0.01);

        SlashField field = new SlashField(actor.getUUID(), ownerId, context.stack().copy(),
                context.hand() == null ? InteractionHand.MAIN_HAND : context.hand(), world.getGameTime(),
                chain.getUUID(), lockOn == null ? null : lockOn.getUUID(),
                lockTarget == null ? null : lockTarget.getUUID());
        FIELDS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUUID(), field);
        return true;
    }

    private static boolean tickSlashField(ServerLevel world, SlashField field) {
        InvertedSpearSettings settings = Config.weapons.inverted_spear_of_heaven;
        if (!(world.getEntity(field.actorId) instanceof LivingEntity actor) || !actor.isAlive()
                || !(world.getEntity(field.chainId) instanceof HeavenChainVisualEntity chain)) {
            return false;
        }

        chain.setPos(actor.getX(), actor.getY(), actor.getZ());
        Entity lockOn = field.lockOnId == null ? null : world.getEntity(field.lockOnId);
        if (lockOn != null) {
            lockOn.setPos(actor.getX(), actor.getY(), actor.getZ());
        }

        int elapsed = (int) (world.getGameTime() - field.startTime);
        int spinStart = settings.fieldWindup;
        int flingStart = spinStart + settings.fieldSpinDuration;
        int retractStart = flingStart + settings.flingDuration;
        int end = retractStart + settings.retractDuration;

        if (elapsed < spinStart) {
            chain.setBlade(handPos(actor));
            return true;
        }

        if (elapsed < flingStart) {
            if (chain.getPhase() != HeavenChainVisualEntity.PHASE_SPIN) {
                chain.setPhase(HeavenChainVisualEntity.PHASE_SPIN);
            }
            int spinTime = elapsed - spinStart;
            Vec3 orbit = chain.orbitOffset(spinTime);
            Vec3 blade = actor.position().add(orbit);
            chain.setBlade(blade);

            if (spinTime % 9 == 0) {
                actor.swing(field.hand, true);
            }
            if (spinTime % 3 == 0) {
                float pitch = 0.85F + world.getRandom().nextFloat() * 0.45F;
                playSound(world, actor, SoundRegistry.HEAVEN_SPIN_SWOOSH.get(), 0.7F, pitch);
            }
            world.sendParticles(ParticlesRegistry.HEAVEN_SPARK.get(), blade.x, blade.y, blade.z,
                    1, 0.1, 0.1, 0.1, 0.0);

            if (spinTime % Math.max(1, settings.fieldHitFrequency) == 0) {
                double reach = orbit.horizontalDistance() + 0.8;
                float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                        settings.fieldDamageScaling, settings.fieldSpellScaling);
                for (LivingEntity target : collectTargets(world, actor, field.ownerId, actor.position(), reach, 2.5)) {
                    if (SimplySwordsAPI.applyAbilityBoltDamageWithoutKnockback(world, actor, field.stack, target, damage)) {
                        world.sendParticles(ParticlesRegistry.HEAVEN_SPARK.get(), target.getX(), target.getY(0.6),
                                target.getZ(), 3, 0.25, 0.3, 0.25, 0.05);
                    }
                }
            }
            return true;
        }

        if (elapsed < retractStart) {
            if (chain.getPhase() != HeavenChainVisualEntity.PHASE_FLING) {
                chain.setPhase(HeavenChainVisualEntity.PHASE_FLING);
                field.flingFrom = chain.getBlade();
                LivingEntity payoff = field.lockTargetId == null ? null
                        : world.getEntity(field.lockTargetId) instanceof LivingEntity living ? living : null;
                if (payoff == null || !AbilityHelper.canHit(world, payoff, actor, field.ownerId)
                        || actor.distanceTo(payoff) > settings.lockOnRange + 2.0) {
                    payoff = findLockTarget(world, actor, field.ownerId, null, settings.lockOnRange);
                }
                field.payoffTargetId = payoff == null ? null : payoff.getUUID();
                Vec3 facing = horizontalFacing(actor);
                field.flingTo = payoff == null
                        ? actor.position().add(facing.scale(settings.lockOnRange * 0.7)).add(0, 1.0, 0)
                        : payoff.position().add(0, payoff.getBbHeight() * 0.55, 0);
                playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 0.8F, 1.4F);
            }

            LivingEntity payoff = livingById(world, field.payoffTargetId);
            if (payoff != null && payoff.isAlive()) {
                field.flingTo = payoff.position().add(0, payoff.getBbHeight() * 0.55, 0);
            }
            float tickProgress = (elapsed - flingStart + 1) / (float) settings.flingDuration;
            float eased = 1.0F - (1.0F - tickProgress) * (1.0F - tickProgress);
            chain.setBlade(field.flingFrom.lerp(field.flingTo, eased));

            if (elapsed == retractStart - 1) {
                if (lockOn != null) {
                    lockOn.discard();
                }
                if (payoff != null && AbilityHelper.canHit(world, payoff, actor, field.ownerId)) {
                    float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                            settings.payoffDamageScaling, settings.payoffSpellScaling);
                    SimplySwordsAPI.applyEntityWeaponHit(field.stack, payoff, actor, damage);
                    playSound(world, payoff, SoundRegistry.HEAVEN_PAYOFF_CLANG.get(), 1.0F, 1.0F);
                    world.sendParticles(ParticlesRegistry.HEAVEN_SPARK.get(), payoff.getX(), payoff.getY(0.55),
                            payoff.getZ(), 14, 0.2, 0.25, 0.2, 0.25);
                    world.sendParticles(ParticleTypes.CRIT, payoff.getX(), payoff.getY(0.55), payoff.getZ(),
                            10, 0.3, 0.3, 0.3, 0.3);
                }
            }
            return true;
        }

        if (elapsed < end) {
            if (chain.getPhase() != HeavenChainVisualEntity.PHASE_RETRACT) {
                chain.setPhase(HeavenChainVisualEntity.PHASE_RETRACT);
                field.retractFrom = chain.getBlade();
                playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_RETRACT.get(), 1.0F, 1.0F);

                LivingEntity payoff = livingById(world, field.payoffTargetId);
                if (payoff != null && AbilityHelper.canHit(world, payoff, actor, field.ownerId) && settings.retractDamageScaling > 0) {
                    float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                            settings.retractDamageScaling, settings.retractSpellScaling);
                    SimplySwordsAPI.applyAbilityBoltDamageWithoutKnockback(world, actor, field.stack, payoff, damage);
                    world.sendParticles(ParticlesRegistry.SEVERED_ASH.get(), payoff.getX(), payoff.getY(0.55),
                            payoff.getZ(), 8, 0.2, 0.2, 0.2, 0.08);
                }
            }

            Vec3 hand = handPos(actor);
            Vec3 mid = field.retractFrom.lerp(hand, 0.5);
            Vec3 across = hand.subtract(field.retractFrom);
            Vec3 side = new Vec3(-across.z, 0, across.x).normalize().scale(across.length() * 0.55);
            Vec3 control = mid.add(side).add(0, 0.6, 0);
            float tickProgress = (elapsed - retractStart + 1) / (float) settings.retractDuration;
            chain.setBlade(bezier(field.retractFrom, control, hand, tickProgress * tickProgress));
            return true;
        }

        return false;
    }

    private static void endSlashField(ServerLevel world, SlashField field) {
        discard(world, field.chainId);
        discard(world, field.lockOnId);
    }

    // Twin Heaven Strike (right click)

    public static boolean startTwinStrike(WeaponAbilityContext context) {
        ServerLevel world = context.world();
        LivingEntity actor = context.actor();
        InvertedSpearSettings settings = Config.weapons.inverted_spear_of_heaven;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUUID();

        LivingEntity target = findDashTarget(world, actor, ownerId, context.target(), settings);

        HeavenChainVisualEntity stance = new HeavenChainVisualEntity(world, actor, context.stack(), 0, 0.0F);
        stance.setPhase(HeavenChainVisualEntity.PHASE_DASH_READY, Math.max(1, settings.dashWindup));
        world.addFreshEntity(stance);
        playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 0.6F, 1.5F);

        TwinStrike strike = new TwinStrike(actor.getUUID(), ownerId, context.stack().copy(),
                context.hand() == null ? InteractionHand.MAIN_HAND : context.hand(), world.getGameTime(), stance.getUUID(),
                target == null ? null : target.getUUID());
        STRIKES.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUUID(), strike);
        return true;
    }

    @Nullable
    private static LivingEntity findDashTarget(ServerLevel world, LivingEntity actor, @Nullable UUID ownerId,
                                               @Nullable LivingEntity preferred, InvertedSpearSettings settings) {
        Vec3 facing = horizontalFacing(actor);
        double reach = settings.dashDistance + settings.strikeRadius;
        if (preferred != null && AbilityHelper.canHit(world, preferred, actor, ownerId) && actor.distanceTo(preferred) <= reach
                && preferred.position().subtract(actor.position()).normalize().dot(facing) >= 0.3) {
            return preferred;
        }
        return SimplySwordsAPI.findClosestAbilityTarget(actor, reach, 2.0)
                .filter(found -> AbilityHelper.canHit(world, found, actor, ownerId))
                .orElse(null);
    }

    private static void beginDash(ServerLevel world, LivingEntity actor, TwinStrike strike,
                                  InvertedSpearSettings settings) {
        // aim again, the target can move during the windup
        LivingEntity target = livingById(world, strike.targetId);
        if (target == null || !AbilityHelper.canHit(world, target, actor, strike.ownerId)) {
            target = findDashTarget(world, actor, strike.ownerId, null, settings);
            strike.targetId = target == null ? null : target.getUUID();
        }

        double distance = settings.dashDistance;
        Vec3 direction = horizontalFacing(actor);
        if (target != null) {
            Vec3 toTarget = new Vec3(target.getX() - actor.getX(), 0, target.getZ() - actor.getZ());
            if (toTarget.lengthSqr() > 0.01) {
                direction = toTarget.normalize();
                distance = Mth.clamp(toTarget.length() - STOP_SHORT_OF_TARGET, 0.0, settings.dashDistance);
            }
        }

        Vec3 start = actor.position();
        strike.direction = direction;
        strike.end = findDashEnd(world, actor, direction, distance);
        strike.dashStart = world.getGameTime();

        HeavenMarkVisualEntity trail = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_DASH_TRAIL,
                start.add(0, 0.05, 0), settings.dashDuration + 10, 1.0F);
        trail.setEndOffset(strike.end.subtract(start));
        trail.setFacing(yawOf(direction));
        world.addFreshEntity(trail);

        setStance(world, strike, HeavenChainVisualEntity.PHASE_DASH, settings.dashDuration);
        playSound(world, actor, SoundRegistry.HEAVEN_DASH.get(), 1.0F, 1.0F);
    }

    private static boolean tickTwinStrike(ServerLevel world, TwinStrike strike) {
        InvertedSpearSettings settings = Config.weapons.inverted_spear_of_heaven;
        if (!(world.getEntity(strike.actorId) instanceof LivingEntity actor) || !actor.isAlive()) {
            return false;
        }
        long now = world.getGameTime();

        if (strike.dashStart < 0) {
            if (now - strike.startTime >= Math.max(1, settings.dashWindup)) {
                beginDash(world, actor, strike, settings);
            }
            return true;
        }

        if (strike.arrivedAt < 0) {
            Vec3 toEnd = strike.end.subtract(actor.position());
            int remaining = (int) (settings.dashDuration - (now - strike.dashStart));
            if (remaining <= 0 || toEnd.horizontalDistance() < 0.2) {
                arrive(world, actor, strike);
                setStance(world, strike, HeavenChainVisualEntity.PHASE_JAB, settings.secondStrikeDelay);
                firstStrike(world, actor, strike, settings);
            } else {
                Vec3 step = toEnd.scale(1.0 / remaining);
                double y = actor.onGround() ? 0.0 : Math.min(actor.getDeltaMovement().y, 0.0);
                actor.setDeltaMovement(step.x, y, step.z);
                actor.hurtMarked = true;
                actor.fallDistance = 0.0F;
                world.sendParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                        1, 0.1, 0.0, 0.1, 0.0);
            }
            return true;
        }

        long sinceArrival = now - strike.arrivedAt;
        if (strike.slashStart < 0 && sinceArrival >= Math.max(0, settings.secondStrikeDelay - 2)) {
            strike.slashStart = now;
            setStance(world, strike, HeavenChainVisualEntity.PHASE_SLASH, SLASH_TICKS);
        }
        if (!strike.secondDone && sinceArrival >= settings.secondStrikeDelay) {
            strike.secondDone = true;
            secondStrike(world, actor, strike, settings);
            if (settings.recoveryDuration > 0) {
                actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, settings.recoveryDuration,
                        2, false, false, false));
            }
        }
        if (strike.slashStart >= 0 && strike.recoverUntil < 0 && now - strike.slashStart >= SLASH_TICKS) {
            int recovery = Math.max(1, settings.recoveryDuration);
            strike.recoverUntil = now + recovery;
            setStance(world, strike, HeavenChainVisualEntity.PHASE_RECOVER, recovery);
        }
        return strike.recoverUntil < 0 || now < strike.recoverUntil;
    }

    private static void setStance(ServerLevel world, TwinStrike strike, int phase, int length) {
        if (world.getEntity(strike.stanceId) instanceof HeavenChainVisualEntity stance) {
            stance.setPhase(phase, Math.max(1, length));
        }
    }

    private static void arrive(ServerLevel world, LivingEntity actor, TwinStrike strike) {
        strike.arrivedAt = world.getGameTime();
        actor.setDeltaMovement(0.0, Math.min(actor.getDeltaMovement().y, 0.0), 0.0);
        actor.hurtMarked = true;
        // snapping players rubber bands them
        if (!(actor instanceof Player) && actor.position().distanceToSqr(strike.end) > 0.09) {
            actor.moveTo(strike.end);
        }
    }

    private static void firstStrike(ServerLevel world, LivingEntity actor, TwinStrike strike,
                                    InvertedSpearSettings settings) {
        LivingEntity primary = livingById(world, strike.targetId);
        Vec3 center = actor.position().add(strike.direction.scale(1.1));
        List<LivingEntity> targets = collectTargets(world, actor, strike.ownerId, center, settings.strikeRadius, 2.0);
        if (primary != null && !targets.contains(primary) && AbilityHelper.canHit(world, primary, actor, strike.ownerId)
                && actor.distanceTo(primary) <= settings.strikeRadius + 1.5) {
            targets.add(primary);
        }

        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, strike.stack,
                settings.firstStrikeDamageScaling, settings.firstStrikeSpellScaling);
        for (LivingEntity target : targets) {
            if (SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, actor, damage)) {
                target.push(strike.direction.x * 0.15, settings.firstStrikeLaunch, strike.direction.z * 0.15);
                target.hurtMarked = true;
                strike.hit.add(target.getUUID());
            }
        }

        Vec3 impact = primary != null && targets.contains(primary) ? primary.position() : center;
        HeavenMarkVisualEntity shockwave = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_SHOCKWAVE,
                impact.add(0, 0.05, 0), 12, (float) settings.strikeRadius * 1.6F);
        world.addFreshEntity(shockwave);

        actor.swing(strike.hand, true);
        playSound(world, actor, SoundRegistry.HEAVEN_STRIKE_HEAVY.get(), 1.0F, 1.0F);
        world.sendParticles(ParticlesRegistry.HEAVEN_SPARK.get(), impact.x, impact.y + 1.0, impact.z,
                18, 0.3, 0.5, 0.3, 0.2);
    }

    private static void secondStrike(ServerLevel world, LivingEntity actor, TwinStrike strike,
                                     InvertedSpearSettings settings) {
        List<LivingEntity> targets = new ArrayList<>();
        for (UUID id : strike.hit) {
            LivingEntity target = livingById(world, id);
            // got launched by the first hit
            if (target != null && AbilityHelper.canHit(world, target, actor, strike.ownerId)
                    && actor.distanceTo(target) <= settings.strikeRadius + 2.0) {
                targets.add(target);
            }
        }
        Vec3 center = actor.position().add(strike.direction.scale(1.1));
        for (LivingEntity target : collectTargets(world, actor, strike.ownerId, center, settings.strikeRadius, 2.5)) {
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }

        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, strike.stack,
                settings.secondStrikeDamageScaling, settings.secondStrikeSpellScaling);
        for (LivingEntity target : targets) {
            SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, actor, damage);
            world.sendParticles(ParticlesRegistry.SEVERED_ASH.get(), target.getX(), target.getY(0.6), target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
        }

        LivingEntity primary = targets.isEmpty() ? null : targets.get(0);
        Vec3 arcPos = primary != null ? primary.position().add(0, primary.getBbHeight() * 0.5, 0) : center.add(0, 1.0, 0);
        HeavenMarkVisualEntity arc = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_DARK_ARC,
                arcPos, 8, 2.2F);
        arc.setFacing(yawOf(strike.direction));
        world.addFreshEntity(arc);

        actor.swing(strike.hand, true);
        playSound(world, actor, SoundRegistry.HEAVEN_STRIKE_QUICK.get(), 1.0F, 1.0F);
    }

    private static Vec3 findDashEnd(ServerLevel world, LivingEntity actor, Vec3 direction, double distance) {
        Vec3 origin = actor.position();
        AABB box = actor.getBoundingBox();
        boolean checkLedges = actor.onGround();
        Vec3 last = origin;

        for (double travelled = DASH_STEP; travelled <= distance + 1.0E-4; travelled += DASH_STEP) {
            Vec3 next = last.add(direction.scale(DASH_STEP));
            BlockPos blockPos = BlockPos.containing(next);
            if (!world.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()),
                    SectionPos.blockToSectionCoord(blockPos.getZ()))) {
                break;
            }

            AABB moved = box.move(next.subtract(origin));
            if (!world.noCollision(actor, moved)) {
                AABB stepped = moved.move(0, actor.maxUpStep(), 0);
                if (!world.noCollision(actor, stepped)) {
                    break;
                }
                next = next.add(0, actor.maxUpStep(), 0);
                moved = stepped;
            }
            if (!world.getWorldBorder().isWithinBounds(moved)) {
                break;
            }
            if (checkLedges) {
                AABB below = new AABB(moved.minX, moved.minY - LEDGE_DROP, moved.minZ, moved.maxX, moved.minY, moved.maxZ);
                if (world.noCollision(actor, below)) {
                    break;
                }
            }
            last = next;
        }
        return last;
    }

    @Nullable
    private static LivingEntity findLockTarget(ServerLevel world, LivingEntity actor, @Nullable UUID ownerId,
                                               @Nullable LivingEntity preferred, double range) {
        if (preferred != null && AbilityHelper.canHit(world, preferred, actor, ownerId) && actor.distanceTo(preferred) <= range) {
            return preferred;
        }
        return collectTargets(world, actor, ownerId, actor.position(), range, 3.0).stream()
                .min(Comparator.comparingDouble(actor::distanceToSqr))
                .orElse(null);
    }

    private static List<LivingEntity> collectTargets(ServerLevel world, LivingEntity actor, @Nullable UUID ownerId,
                                                     Vec3 center, double radius, double height) {
        AABB area = new AABB(center.x - radius, center.y - height, center.z - radius,
                center.x + radius, center.y + height + actor.getBbHeight(), center.z + radius);
        double radiusSq = radius * radius;
        return new ArrayList<>(world.getEntitiesOfClass(LivingEntity.class, area, target -> {
            double offsetX = target.getX() - center.x;
            double offsetZ = target.getZ() - center.z;
            return offsetX * offsetX + offsetZ * offsetZ <= radiusSq && AbilityHelper.canHit(world, target, actor, ownerId);
        }));
    }

    @Nullable
    private static LivingEntity livingById(ServerLevel world, @Nullable UUID id) {
        return id != null && world.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static Vec3 handPos(LivingEntity actor) {
        float yaw = actor.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return actor.position().add(right.scale(0.38)).add(0, actor.getBbHeight() * 0.42, 0);
    }

    private static Vec3 horizontalFacing(LivingEntity actor) {
        Vec3 look = actor.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = Vec3.directionFromRotation(0.0F, actor.getYRot());
        }
        return flat.normalize();
    }

    private static float yawOf(Vec3 direction) {
        return (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private static Vec3 bezier(Vec3 bezierStart, Vec3 control, Vec3 bezierEnd, float tickProgress) {
        float progressRemaining = 1.0F - tickProgress;
        return bezierStart.scale(progressRemaining * progressRemaining).add(control.scale(2 * progressRemaining * tickProgress)).add(bezierEnd.scale(tickProgress * tickProgress));
    }

    private static void playSound(ServerLevel world, Entity source, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, source.getX(), source.getY(), source.getZ(), sound, source.getSoundSource(), volume, pitch);
        SoundEvent layer = SoundRegistry.layer(sound);
        if (layer != null) {
            world.playSound(null, source.getX(), source.getY(), source.getZ(), layer, source.getSoundSource(), volume * 0.45F, pitch);
        }
    }

    private static void discard(ServerLevel world, @Nullable UUID id) {
        if (id != null) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    public static void tick(ServerLevel world) {
        Map<UUID, SlashField> fields = FIELDS.get(world);
        if (fields != null && !fields.isEmpty()) {
            Iterator<SlashField> iterator = fields.values().iterator();
            while (iterator.hasNext()) {
                SlashField field = iterator.next();
                if (!tickSlashField(world, field)) {
                    endSlashField(world, field);
                    iterator.remove();
                }
            }
        }

        Map<UUID, TwinStrike> strikes = STRIKES.get(world);
        if (strikes != null && !strikes.isEmpty()) {
            strikes.values().removeIf(strike -> {
                Entity stance = world.getEntity(strike.stanceId);
                if (stance != null && world.getEntity(strike.actorId) instanceof LivingEntity actor) {
                    stance.setPos(actor.getX(), actor.getY(), actor.getZ());
                }
                if (tickTwinStrike(world, strike)) {
                    return false;
                }
                discard(world, strike.stanceId);
                return true;
            });
        }
    }

    public static void clear(ServerLevel world) {
        Map<UUID, SlashField> fields = FIELDS.remove(world);
        if (fields != null) {
            fields.values().forEach(field -> endSlashField(world, field));
        }
        Map<UUID, TwinStrike> strikes = STRIKES.remove(world);
        if (strikes != null) {
            strikes.values().forEach(strike -> discard(world, strike.stanceId));
        }
    }

    public static void clearAll() {
        FIELDS.clear();
        STRIKES.clear();
    }

    public static void clearActor(LivingEntity actor) {
        for (Map.Entry<ServerLevel, Map<UUID, SlashField>> entry : FIELDS.entrySet()) {
            SlashField field = entry.getValue().remove(actor.getUUID());
            if (field != null) {
                endSlashField(entry.getKey(), field);
            }
        }
        for (Map.Entry<ServerLevel, Map<UUID, TwinStrike>> entry : STRIKES.entrySet()) {
            TwinStrike strike = entry.getValue().remove(actor.getUUID());
            if (strike != null) {
                discard(entry.getKey(), strike.stanceId);
            }
        }
    }
}
