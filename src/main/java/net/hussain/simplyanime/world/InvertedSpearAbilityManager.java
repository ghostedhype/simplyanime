package net.hussain.simplyanime.world;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.entity.HeavenChainVisualEntity;
import net.hussain.simplyanime.entity.HeavenMarkVisualEntity;
import net.hussain.simplyanime.item.InvertedSpearItem;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.hussain.simplyanime.registry.SoundRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InvertedSpearAbilityManager {

    private static final Map<ServerWorld, Map<UUID, SlashField>> FIELDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, TwinStrike>> STRIKES = new HashMap<>();

    private static final double DASH_STEP = 0.25;
    private static final double LEDGE_DROP = 1.25;
    private static final double STOP_SHORT_OF_TARGET = 1.2;
    private static final int SLASH_TICKS = 6;

    private InvertedSpearAbilityManager() {
    }

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(InvertedSpearAbilityManager::tick);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(InvertedSpearAbilityManager::clear);
        LifecycleEvent.SERVER_STOPPED.register(server -> clearAll());
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> clearActor(player));
        PlayerEvent.PLAYER_QUIT.register(InvertedSpearAbilityManager::clearActor);
    }

    public static boolean wantsTwinStrike(WeaponAbilityContext context) {
        if (context == null || context.actor() == null) {
            return false;
        }
        if (context.actor() instanceof PlayerEntity player) {
            return !player.isSneaking();
        }
        // mobs can't sneak, dash if the target is far
        LivingEntity target = context.target();
        return target != null && context.actor().squaredDistanceTo(target) > 3.5 * 3.5;
    }

    public static boolean isBusy(ServerWorld world, LivingEntity actor) {
        Map<UUID, SlashField> fields = FIELDS.get(world);
        Map<UUID, TwinStrike> strikes = STRIKES.get(world);
        return (fields != null && fields.containsKey(actor.getUuid()))
                || (strikes != null && strikes.containsKey(actor.getUuid()));
    }

    // Heavenly Slash Field (sneak + right click)

    public static boolean startSlashField(WeaponAbilityContext context) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        InvertedSpearItem.EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid();

        LivingEntity lockTarget = findLockTarget(world, actor, ownerId, context.target(), settings.lockOnRange);

        HeavenChainVisualEntity chain = new HeavenChainVisualEntity(world, actor, context.stack(),
                settings.fieldSpinDuration, (float) settings.fieldRadius);
        world.spawnEntity(chain);

        HeavenMarkVisualEntity crack = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_CRACK,
                actor.getPos().add(0, 0.02, 0), settings.fieldWindup + 22, (float) settings.fieldRadius * 0.6F);
        world.spawnEntity(crack);

        HeavenMarkVisualEntity lockOn = null;
        if (lockTarget != null) {
            lockOn = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_LOCK_ON, actor.getPos(),
                    settings.fieldWindup + settings.fieldSpinDuration + settings.flingDuration + 2, lockTarget.getWidth());
            lockOn.setTargetId(lockTarget.getId());
            world.spawnEntity(lockOn);
        }

        playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 1.0F, 1.0F);
        world.spawnParticles(ParticleTypes.SMOKE, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                12, 0.6, 0.02, 0.6, 0.01);

        SlashField field = new SlashField(actor.getUuid(), ownerId, context.stack().copy(),
                context.hand() == null ? Hand.MAIN_HAND : context.hand(), world.getTime(),
                chain.getUuid(), lockOn == null ? null : lockOn.getUuid(),
                lockTarget == null ? null : lockTarget.getUuid());
        FIELDS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), field);
        return true;
    }

    private static boolean tickSlashField(ServerWorld world, SlashField field) {
        InvertedSpearItem.EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        if (!(world.getEntity(field.actorId) instanceof LivingEntity actor) || !actor.isAlive()
                || !(world.getEntity(field.chainId) instanceof HeavenChainVisualEntity chain)) {
            return false;
        }

        chain.setPosition(actor.getX(), actor.getY(), actor.getZ());
        Entity lockOn = field.lockOnId == null ? null : world.getEntity(field.lockOnId);
        if (lockOn != null) {
            lockOn.setPosition(actor.getX(), actor.getY(), actor.getZ());
        }

        int elapsed = (int) (world.getTime() - field.startTime);
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
            Vec3d orbit = chain.orbitOffset(spinTime);
            Vec3d blade = actor.getPos().add(orbit);
            chain.setBlade(blade);

            if (spinTime % 9 == 0) {
                actor.swingHand(field.hand, true);
            }
            if (spinTime % 3 == 0) {
                float pitch = 0.85F + world.getRandom().nextFloat() * 0.45F;
                playSound(world, actor, SoundRegistry.HEAVEN_SPIN_SWOOSH.get(), 0.7F, pitch);
            }
            world.spawnParticles(ParticlesRegistry.HEAVEN_SPARK.get(), blade.x, blade.y, blade.z,
                    1, 0.1, 0.1, 0.1, 0.0);

            if (spinTime % Math.max(1, settings.fieldHitFrequency) == 0) {
                double reach = orbit.horizontalLength() + 0.8;
                float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                        settings.fieldDamageScaling, settings.fieldSpellScaling);
                for (LivingEntity target : collectTargets(world, actor, field.ownerId, actor.getPos(), reach, 2.5)) {
                    if (SimplySwordsAPI.applyAbilityBoltDamageWithoutKnockback(world, actor, field.stack, target, damage)) {
                        world.spawnParticles(ParticlesRegistry.HEAVEN_SPARK.get(), target.getX(), target.getBodyY(0.6),
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
                if (payoff == null || !canHit(world, payoff, actor, field.ownerId)
                        || actor.distanceTo(payoff) > settings.lockOnRange + 2.0) {
                    payoff = findLockTarget(world, actor, field.ownerId, null, settings.lockOnRange);
                }
                field.payoffTargetId = payoff == null ? null : payoff.getUuid();
                Vec3d facing = horizontalFacing(actor);
                field.flingTo = payoff == null
                        ? actor.getPos().add(facing.multiply(settings.lockOnRange * 0.7)).add(0, 1.0, 0)
                        : payoff.getPos().add(0, payoff.getHeight() * 0.55, 0);
                playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 0.8F, 1.4F);
            }

            LivingEntity payoff = livingById(world, field.payoffTargetId);
            if (payoff != null && payoff.isAlive()) {
                field.flingTo = payoff.getPos().add(0, payoff.getHeight() * 0.55, 0);
            }
            float t = (elapsed - flingStart + 1) / (float) settings.flingDuration;
            float eased = 1.0F - (1.0F - t) * (1.0F - t);
            chain.setBlade(field.flingFrom.lerp(field.flingTo, eased));

            if (elapsed == retractStart - 1) {
                if (lockOn != null) {
                    lockOn.discard();
                }
                if (payoff != null && canHit(world, payoff, actor, field.ownerId)) {
                    float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                            settings.payoffDamageScaling, settings.payoffSpellScaling);
                    SimplySwordsAPI.applyEntityWeaponHit(field.stack, payoff, actor, damage);
                    playSound(world, payoff, SoundRegistry.HEAVEN_PAYOFF_CLANG.get(), 1.0F, 1.0F);
                    world.spawnParticles(ParticlesRegistry.HEAVEN_SPARK.get(), payoff.getX(), payoff.getBodyY(0.55),
                            payoff.getZ(), 14, 0.2, 0.25, 0.2, 0.25);
                    world.spawnParticles(ParticleTypes.CRIT, payoff.getX(), payoff.getBodyY(0.55), payoff.getZ(),
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
                if (payoff != null && canHit(world, payoff, actor, field.ownerId) && settings.retractDamageScaling > 0) {
                    float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, field.stack,
                            settings.retractDamageScaling, settings.retractSpellScaling);
                    SimplySwordsAPI.applyAbilityBoltDamageWithoutKnockback(world, actor, field.stack, payoff, damage);
                    world.spawnParticles(ParticlesRegistry.SEVERED_ASH.get(), payoff.getX(), payoff.getBodyY(0.55),
                            payoff.getZ(), 8, 0.2, 0.2, 0.2, 0.08);
                }
            }

            Vec3d hand = handPos(actor);
            Vec3d mid = field.retractFrom.lerp(hand, 0.5);
            Vec3d across = hand.subtract(field.retractFrom);
            Vec3d side = new Vec3d(-across.z, 0, across.x).normalize().multiply(across.length() * 0.55);
            Vec3d control = mid.add(side).add(0, 0.6, 0);
            float t = (elapsed - retractStart + 1) / (float) settings.retractDuration;
            chain.setBlade(bezier(field.retractFrom, control, hand, t * t));
            return true;
        }

        return false;
    }

    private static void endSlashField(ServerWorld world, SlashField field) {
        discard(world, field.chainId);
        discard(world, field.lockOnId);
    }

    // Twin Heaven Strike (right click)

    public static boolean startTwinStrike(WeaponAbilityContext context) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        InvertedSpearItem.EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        UUID ownerId = context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid();

        LivingEntity target = findDashTarget(world, actor, ownerId, context.target(), settings);

        HeavenChainVisualEntity stance = new HeavenChainVisualEntity(world, actor, context.stack(), 0, 0.0F);
        stance.setPhase(HeavenChainVisualEntity.PHASE_DASH_READY, Math.max(1, settings.dashWindup));
        world.spawnEntity(stance);
        playSound(world, actor, SoundRegistry.HEAVEN_CHAIN_WINDUP.get(), 0.6F, 1.5F);

        TwinStrike strike = new TwinStrike(actor.getUuid(), ownerId, context.stack().copy(),
                context.hand() == null ? Hand.MAIN_HAND : context.hand(), world.getTime(), stance.getUuid(),
                target == null ? null : target.getUuid());
        STRIKES.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), strike);
        return true;
    }

    @Nullable
    private static LivingEntity findDashTarget(ServerWorld world, LivingEntity actor, @Nullable UUID ownerId,
                                               @Nullable LivingEntity preferred, InvertedSpearItem.EffectSettings settings) {
        Vec3d facing = horizontalFacing(actor);
        double reach = settings.dashDistance + settings.strikeRadius;
        if (preferred != null && canHit(world, preferred, actor, ownerId) && actor.distanceTo(preferred) <= reach
                && preferred.getPos().subtract(actor.getPos()).normalize().dotProduct(facing) >= 0.3) {
            return preferred;
        }
        return SimplySwordsAPI.findClosestAbilityTarget(actor, reach, 2.0)
                .filter(found -> canHit(world, found, actor, ownerId))
                .orElse(null);
    }

    private static void beginDash(ServerWorld world, LivingEntity actor, TwinStrike strike,
                                  InvertedSpearItem.EffectSettings settings) {
        // aim again, the target can move during the windup
        LivingEntity target = livingById(world, strike.targetId);
        if (target == null || !canHit(world, target, actor, strike.ownerId)) {
            target = findDashTarget(world, actor, strike.ownerId, null, settings);
            strike.targetId = target == null ? null : target.getUuid();
        }

        double distance = settings.dashDistance;
        Vec3d direction = horizontalFacing(actor);
        if (target != null) {
            Vec3d toTarget = new Vec3d(target.getX() - actor.getX(), 0, target.getZ() - actor.getZ());
            if (toTarget.lengthSquared() > 0.01) {
                direction = toTarget.normalize();
                distance = MathHelper.clamp(toTarget.length() - STOP_SHORT_OF_TARGET, 0.0, settings.dashDistance);
            }
        }

        Vec3d start = actor.getPos();
        strike.direction = direction;
        strike.end = findDashEnd(world, actor, direction, distance);
        strike.dashStart = world.getTime();

        HeavenMarkVisualEntity trail = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_DASH_TRAIL,
                start.add(0, 0.05, 0), settings.dashDuration + 10, 1.0F);
        trail.setEndOffset(strike.end.subtract(start));
        trail.setFacing(yawOf(direction));
        world.spawnEntity(trail);

        setStance(world, strike, HeavenChainVisualEntity.PHASE_DASH, settings.dashDuration);
        playSound(world, actor, SoundRegistry.HEAVEN_DASH.get(), 1.0F, 1.0F);
    }

    private static boolean tickTwinStrike(ServerWorld world, TwinStrike strike) {
        InvertedSpearItem.EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        if (!(world.getEntity(strike.actorId) instanceof LivingEntity actor) || !actor.isAlive()) {
            return false;
        }
        long now = world.getTime();

        if (strike.dashStart < 0) {
            if (now - strike.startTime >= Math.max(1, settings.dashWindup)) {
                beginDash(world, actor, strike, settings);
            }
            return true;
        }

        if (strike.arrivedAt < 0) {
            Vec3d toEnd = strike.end.subtract(actor.getPos());
            int remaining = (int) (settings.dashDuration - (now - strike.dashStart));
            if (remaining <= 0 || toEnd.horizontalLength() < 0.2) {
                arrive(world, actor, strike);
                setStance(world, strike, HeavenChainVisualEntity.PHASE_JAB, settings.secondStrikeDelay);
                firstStrike(world, actor, strike, settings);
            } else {
                Vec3d step = toEnd.multiply(1.0 / remaining);
                double y = actor.isOnGround() ? 0.0 : Math.min(actor.getVelocity().y, 0.0);
                actor.setVelocity(step.x, y, step.z);
                actor.velocityModified = true;
                actor.fallDistance = 0.0F;
                world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.1, actor.getZ(),
                        1, 0.1, 0.0, 0.1, 0.0);
            }
            return true;
        }

        long sinceArrival = now - strike.arrivedAt;
        // slash starts a bit early so the arm is already moving when it lands
        if (strike.slashStart < 0 && sinceArrival >= Math.max(0, settings.secondStrikeDelay - 2)) {
            strike.slashStart = now;
            setStance(world, strike, HeavenChainVisualEntity.PHASE_SLASH, SLASH_TICKS);
        }
        if (!strike.secondDone && sinceArrival >= settings.secondStrikeDelay) {
            strike.secondDone = true;
            secondStrike(world, actor, strike, settings);
            if (settings.recoveryDuration > 0) {
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, settings.recoveryDuration,
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

    private static void setStance(ServerWorld world, TwinStrike strike, int phase, int length) {
        if (world.getEntity(strike.stanceId) instanceof HeavenChainVisualEntity stance) {
            stance.setPhase(phase, Math.max(1, length));
        }
    }

    private static void arrive(ServerWorld world, LivingEntity actor, TwinStrike strike) {
        strike.arrivedAt = world.getTime();
        actor.setVelocity(0.0, Math.min(actor.getVelocity().y, 0.0), 0.0);
        actor.velocityModified = true;
        // snapping players rubber bands them
        if (!(actor instanceof PlayerEntity) && actor.getPos().squaredDistanceTo(strike.end) > 0.09) {
            actor.refreshPositionAfterTeleport(strike.end);
        }
    }

    private static void firstStrike(ServerWorld world, LivingEntity actor, TwinStrike strike,
                                    InvertedSpearItem.EffectSettings settings) {
        LivingEntity primary = livingById(world, strike.targetId);
        Vec3d center = actor.getPos().add(strike.direction.multiply(1.1));
        List<LivingEntity> targets = collectTargets(world, actor, strike.ownerId, center, settings.strikeRadius, 2.0);
        if (primary != null && !targets.contains(primary) && canHit(world, primary, actor, strike.ownerId)
                && actor.distanceTo(primary) <= settings.strikeRadius + 1.5) {
            targets.add(primary);
        }

        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, strike.stack,
                settings.firstStrikeDamageScaling, settings.firstStrikeSpellScaling);
        for (LivingEntity target : targets) {
            if (SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, actor, damage)) {
                target.addVelocity(strike.direction.x * 0.15, settings.firstStrikeLaunch, strike.direction.z * 0.15);
                target.velocityModified = true;
                strike.hit.add(target.getUuid());
            }
        }

        Vec3d impact = primary != null && targets.contains(primary) ? primary.getPos() : center;
        HeavenMarkVisualEntity shockwave = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_SHOCKWAVE,
                impact.add(0, 0.05, 0), 12, (float) settings.strikeRadius * 1.6F);
        world.spawnEntity(shockwave);

        actor.swingHand(strike.hand, true);
        playSound(world, actor, SoundRegistry.HEAVEN_STRIKE_HEAVY.get(), 1.0F, 1.0F);
        world.spawnParticles(ParticlesRegistry.HEAVEN_SPARK.get(), impact.x, impact.y + 1.0, impact.z,
                18, 0.3, 0.5, 0.3, 0.2);
    }

    private static void secondStrike(ServerWorld world, LivingEntity actor, TwinStrike strike,
                                     InvertedSpearItem.EffectSettings settings) {
        List<LivingEntity> targets = new ArrayList<>();
        for (UUID id : strike.hit) {
            LivingEntity target = livingById(world, id);
            // got launched by the first hit
            if (target != null && canHit(world, target, actor, strike.ownerId)
                    && actor.distanceTo(target) <= settings.strikeRadius + 2.0) {
                targets.add(target);
            }
        }
        Vec3d center = actor.getPos().add(strike.direction.multiply(1.1));
        for (LivingEntity target : collectTargets(world, actor, strike.ownerId, center, settings.strikeRadius, 2.5)) {
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }

        float damage = SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, actor, strike.stack,
                settings.secondStrikeDamageScaling, settings.secondStrikeSpellScaling);
        for (LivingEntity target : targets) {
            SimplySwordsAPI.applyEntityWeaponHit(strike.stack, target, actor, damage);
            world.spawnParticles(ParticlesRegistry.SEVERED_ASH.get(), target.getX(), target.getBodyY(0.6), target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
        }

        LivingEntity primary = targets.isEmpty() ? null : targets.get(0);
        Vec3d arcPos = primary != null ? primary.getPos().add(0, primary.getHeight() * 0.5, 0) : center.add(0, 1.0, 0);
        HeavenMarkVisualEntity arc = new HeavenMarkVisualEntity(world, HeavenMarkVisualEntity.MODE_DARK_ARC,
                arcPos, 8, 2.2F);
        arc.setFacing(yawOf(strike.direction));
        world.spawnEntity(arc);

        actor.swingHand(strike.hand, true);
        playSound(world, actor, SoundRegistry.HEAVEN_STRIKE_QUICK.get(), 1.0F, 1.0F);
    }

    private static Vec3d findDashEnd(ServerWorld world, LivingEntity actor, Vec3d direction, double distance) {
        Vec3d origin = actor.getPos();
        Box box = actor.getBoundingBox();
        boolean checkLedges = actor.isOnGround();
        Vec3d last = origin;

        for (double travelled = DASH_STEP; travelled <= distance + 1.0E-4; travelled += DASH_STEP) {
            Vec3d next = last.add(direction.multiply(DASH_STEP));
            BlockPos blockPos = BlockPos.ofFloored(next);
            if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()),
                    ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
                break;
            }

            Box moved = box.offset(next.subtract(origin));
            if (!world.isSpaceEmpty(actor, moved)) {
                Box stepped = moved.offset(0, actor.getStepHeight(), 0);
                if (!world.isSpaceEmpty(actor, stepped)) {
                    break;
                }
                next = next.add(0, actor.getStepHeight(), 0);
                moved = stepped;
            }
            if (!world.getWorldBorder().contains(moved)) {
                break;
            }
            if (checkLedges) {
                Box below = new Box(moved.minX, moved.minY - LEDGE_DROP, moved.minZ, moved.maxX, moved.minY, moved.maxZ);
                if (world.isSpaceEmpty(actor, below)) {
                    break;
                }
            }
            last = next;
        }
        return last;
    }

    @Nullable
    private static LivingEntity findLockTarget(ServerWorld world, LivingEntity actor, @Nullable UUID ownerId,
                                               @Nullable LivingEntity preferred, double range) {
        if (preferred != null && canHit(world, preferred, actor, ownerId) && actor.distanceTo(preferred) <= range) {
            return preferred;
        }
        return collectTargets(world, actor, ownerId, actor.getPos(), range, 3.0).stream()
                .min(Comparator.comparingDouble(actor::squaredDistanceTo))
                .orElse(null);
    }

    private static List<LivingEntity> collectTargets(ServerWorld world, LivingEntity actor, @Nullable UUID ownerId,
                                                     Vec3d center, double radius, double height) {
        Box area = new Box(center.x - radius, center.y - height, center.z - radius,
                center.x + radius, center.y + height + actor.getHeight(), center.z + radius);
        double radiusSq = radius * radius;
        return new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, area, target -> {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            return dx * dx + dz * dz <= radiusSq && canHit(world, target, actor, ownerId);
        }));
    }

    private static boolean canHit(ServerWorld world, LivingEntity target, LivingEntity actor, @Nullable UUID ownerId) {
        if (target == actor || !target.isAlive() || !SimplySwordsAPI.isValidAbilityTarget(target, actor)) {
            return false;
        }
        if (ownerId == null) {
            return true;
        }
        Entity owner = world.getEntity(ownerId);
        return !(owner instanceof LivingEntity livingOwner)
                || target != livingOwner && SimplySwordsAPI.isValidAbilityTarget(target, livingOwner);
    }

    @Nullable
    private static LivingEntity livingById(ServerWorld world, @Nullable UUID id) {
        return id != null && world.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static Vec3d handPos(LivingEntity actor) {
        float yaw = actor.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d right = new Vec3d(-MathHelper.cos(yaw), 0, -MathHelper.sin(yaw));
        return actor.getPos().add(right.multiply(0.38)).add(0, actor.getHeight() * 0.42, 0);
    }

    private static Vec3d horizontalFacing(LivingEntity actor) {
        Vec3d look = actor.getRotationVec(1.0F);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        if (flat.lengthSquared() < 1.0E-4) {
            flat = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return flat.normalize();
    }

    private static float yawOf(Vec3d direction) {
        return (float) (MathHelper.atan2(direction.z, direction.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
    }

    private static Vec3d bezier(Vec3d a, Vec3d control, Vec3d b, float t) {
        float u = 1.0F - t;
        return a.multiply(u * u).add(control.multiply(2 * u * t)).add(b.multiply(t * t));
    }

    private static void playSound(ServerWorld world, Entity source, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, source.getX(), source.getY(), source.getZ(), sound, source.getSoundCategory(), volume, pitch);
    }

    private static void discard(ServerWorld world, @Nullable UUID id) {
        if (id != null) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void tick(ServerWorld world) {
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
                    stance.setPosition(actor.getX(), actor.getY(), actor.getZ());
                }
                if (tickTwinStrike(world, strike)) {
                    return false;
                }
                discard(world, strike.stanceId);
                return true;
            });
        }
    }

    private static void clear(ServerWorld world) {
        Map<UUID, SlashField> fields = FIELDS.remove(world);
        if (fields != null) {
            fields.values().forEach(field -> endSlashField(world, field));
        }
        Map<UUID, TwinStrike> strikes = STRIKES.remove(world);
        if (strikes != null) {
            strikes.values().forEach(strike -> discard(world, strike.stanceId));
        }
    }

    private static void clearAll() {
        FIELDS.clear();
        STRIKES.clear();
    }

    private static void clearActor(LivingEntity actor) {
        for (Map.Entry<ServerWorld, Map<UUID, SlashField>> entry : FIELDS.entrySet()) {
            SlashField field = entry.getValue().remove(actor.getUuid());
            if (field != null) {
                endSlashField(entry.getKey(), field);
            }
        }
        for (Map.Entry<ServerWorld, Map<UUID, TwinStrike>> entry : STRIKES.entrySet()) {
            TwinStrike strike = entry.getValue().remove(actor.getUuid());
            if (strike != null) {
                discard(entry.getKey(), strike.stanceId);
            }
        }
    }

    private static final class SlashField {
        final UUID actorId;
        @Nullable final UUID ownerId;
        final ItemStack stack;
        final Hand hand;
        final long startTime;
        final UUID chainId;
        @Nullable final UUID lockOnId;
        @Nullable final UUID lockTargetId;
        @Nullable UUID payoffTargetId;
        Vec3d flingFrom = Vec3d.ZERO;
        Vec3d flingTo = Vec3d.ZERO;
        Vec3d retractFrom = Vec3d.ZERO;

        SlashField(UUID actorId, @Nullable UUID ownerId, ItemStack stack, Hand hand, long startTime, UUID chainId,
                   @Nullable UUID lockOnId, @Nullable UUID lockTargetId) {
            this.actorId = actorId;
            this.ownerId = ownerId;
            this.stack = stack;
            this.hand = hand;
            this.startTime = startTime;
            this.chainId = chainId;
            this.lockOnId = lockOnId;
            this.lockTargetId = lockTargetId;
        }
    }

    private static final class TwinStrike {
        final UUID actorId;
        @Nullable final UUID ownerId;
        final ItemStack stack;
        final Hand hand;
        final long startTime;
        final UUID stanceId;
        @Nullable UUID targetId;
        final List<UUID> hit = new ArrayList<>();
        Vec3d end = Vec3d.ZERO;
        Vec3d direction = Vec3d.ZERO;
        long dashStart = -1;
        long arrivedAt = -1;
        long slashStart = -1;
        long recoverUntil = -1;
        boolean secondDone;

        TwinStrike(UUID actorId, @Nullable UUID ownerId, ItemStack stack, Hand hand, long startTime, UUID stanceId,
                   @Nullable UUID targetId) {
            this.actorId = actorId;
            this.ownerId = ownerId;
            this.stack = stack;
            this.hand = hand;
            this.startTime = startTime;
            this.stanceId = stanceId;
            this.targetId = targetId;
        }
    }
}
