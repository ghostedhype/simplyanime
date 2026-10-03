package net.hussain.simplyanime.rhitta.entity;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.client.EffectLights;
import net.hussain.simplyanime.rhitta.DivineAxeRhittaItem;
import net.hussain.simplyanime.callout.Callout;
import net.hussain.simplyanime.callout.Callouts;
import net.hussain.simplyanime.rhitta.RhittaRegistry;
import net.hussain.simplyanime.rhitta.SunPower;
import net.hussain.simplyanime.rhitta.client.RhittaClientHooks;
import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraftforge.entity.PartEntity;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// one entity for the whole cast: forms over the caster, flies, explodes.
// server does the damage, the client just draws from the tracked values
public class CruelSunEntity extends Entity {

    public static final int PHASE_CHARGE = 0;
    public static final int PHASE_FLIGHT = 1;
    public static final int PHASE_BLAST = 2;

    // shared with the renderer
    public static final float HEAT_EDGE = 2.0F;          // corona ends here, x orb radius
    public static final float FLIGHT_GROWTH = 0.2F;
    public static final float FLIGHT_GROW_TICKS = 30.0F;
    // the eruption lines up with the boom in the build up audio
    public static final int IGNITE_TICKS = 20;
    public static final int ERUPT_TICKS = 25;
    public static final int SWELL_TICKS = 55;
    public static final int BLAST_GROW = 10;
    public static final int BLAST_TICKS = 70;
    public static final float LINGER_CUTOFF = 0.6F;      // fireball is too faint to burn past this
    public static final float TRAIL_LENGTH = 3.0F;       // x orb radius
    public static final float TRAIL_WIDTH = 0.9F;        // half width at the sun
    public static final float TRAIL_TIP = 0.15F;
    public static final float FADE_SWELL = 0.08F;
    private static final double HAND_OFFSET = 0.36;      // raised hand, out from the middle of the body

    // set while Cruel Sun deals damage so the melee sun bonus doesn't stack on top
    public static boolean dealing;

    public static final Map<Integer, CruelSunEntity> CLIENT_BY_OWNER = new HashMap<>();
    public static final Set<CruelSunEntity> CLIENT_ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, CruelSunEntity> CHARGING = new HashMap<>();

    private static final TrackedData<Integer> PHASE = register(TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_START_AGE = register(TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_LENGTH = register(TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID = register(TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> POWER = register(TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SUN = register(TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORB_BASE = register(TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> BLAST_RADIUS = register(TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> DIRECTION = register(TrackedDataHandlerRegistry.VECTOR3F);

    @SuppressWarnings("unchecked")
    private static <T> TrackedData<T> register(net.minecraft.entity.data.TrackedDataHandler<T> handler) {
        return DataTracker.registerData(CruelSunEntity.class, handler);
    }

    @Nullable private UUID ownerUuid;
    @Nullable private UUID targetUuid;
    private ItemStack stack = ItemStack.EMPTY;
    private Hand hand = Hand.MAIN_HAND;
    private Vec3d direction = new Vec3d(0, 0, 1);
    private double traveled;
    private final Set<UUID> blasted = new HashSet<>();

    private int lastPhase = -1;
    private int clientPhaseStart;
    private boolean flightSoundStarted;

    public CruelSunEntity(EntityType<? extends CruelSunEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static void init() {
        LifecycleEvent.SERVER_STOPPED.register(server -> CHARGING.clear());
        EntityEvent.LIVING_HURT.register((entity, source, amount) ->
                RhittaConfig.get().cruelSun.chargeInvulnerable && isCharging(entity)
                        && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                        ? EventResult.interruptFalse() : EventResult.pass());
    }

    public static boolean isCharging(LivingEntity actor) {
        CruelSunEntity sun = CHARGING.get(actor.getUuid());
        return sun != null && !sun.isRemoved() && sun.getPhase() == PHASE_CHARGE;
    }

    public static boolean start(WeaponAbilityContext context) {
        return start(context.world(), context.actor(), context.target(), context.stack(),
                context.hand() == null ? Hand.MAIN_HAND : context.hand());
    }

    public static boolean start(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack, Hand hand) {
        RhittaConfig.CruelSunSettings cfg = RhittaConfig.get().cruelSun;
        float power = SunPower.sunPower(world, actor);
        float sun = SunPower.fraction(power);

        CruelSunEntity entity = new CruelSunEntity(RhittaRegistry.CRUEL_SUN.get(), world);
        entity.ownerUuid = actor.getUuid();
        entity.targetUuid = target == null ? null : target.getUuid();
        entity.stack = stack.copy();
        entity.hand = hand;
        entity.dataTracker.set(OWNER_ID, actor.getId());
        entity.dataTracker.set(POWER, power);
        entity.dataTracker.set(SUN, sun);
        entity.dataTracker.set(ORB_BASE, cfg.orbRadius * (1.0F + cfg.orbSunScaling * sun));
        entity.dataTracker.set(BLAST_RADIUS, cfg.blastRadius * (1.0F + cfg.blastSunScaling * sun));
        entity.setPhase(PHASE_CHARGE, cfg.chargeDuration);
        Vec3d at = chargeCenter(actor, 1.0F, entity.getOrbRadius(0.0F));
        entity.setPosition(at.x, at.y, at.z);
        world.spawnEntity(entity);
        CHARGING.put(actor.getUuid(), entity);

        if (cfg.chargeSlowness >= 0) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, cfg.chargeDuration + 2,
                    cfg.chargeSlowness, false, false, false));
        }
        playSound(world, actor.getPos(), RhittaRegistry.BUILD_UP.get(), cfg.buildUpVolume, 1.0F);
        if (cfg.nameText && cfg.nameTextLead >= cfg.chargeDuration) {
            Callouts.send(world, actor.getPos(), Callout.CRUEL_SUN);
        }
        return true;
    }

    public static float orbRadius(float base, int phase, float chargeTicks, float flightTicks) {
        if (phase == PHASE_CHARGE) {
            if (chargeTicks < IGNITE_TICKS) {
                return base * (0.06F + 0.03F * chargeTicks / IGNITE_TICKS);
            }
            float g = Math.min(1.0F, (chargeTicks - IGNITE_TICKS) / ERUPT_TICKS);
            g = 1.0F - (1.0F - g) * (1.0F - g) * (1.0F - g);
            float late = MathHelper.clamp((chargeTicks - IGNITE_TICKS - ERUPT_TICKS) / SWELL_TICKS, 0.0F, 1.0F);
            return base * (0.09F + 0.76F * g + 0.15F * late);
        }
        return base * (1.0F + FLIGHT_GROWTH * Math.min(1.0F, flightTicks / FLIGHT_GROW_TICKS));
    }

    // how far the fireball has spread, ticks counted from the detonation
    public static float blastFront(float radius, float ticks) {
        float g = Math.min(1.0F, ticks / BLAST_GROW);
        return radius * (1.0F - (1.0F - g) * (1.0F - g));
    }

    public static float blastFade(float ticks) {
        return MathHelper.clamp((ticks - BLAST_GROW) / (float) (BLAST_TICKS - BLAST_GROW), 0.0F, 1.0F);
    }

    // over the hand holding Rhitta
    public static Vec3d chargeCenter(LivingEntity owner, float tickDelta, float orb) {
        float yaw = MathHelper.lerp(tickDelta, owner.prevBodyYaw, owner.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        double side = owner.getMainArm() == Arm.LEFT ? -HAND_OFFSET : HAND_OFFSET;
        return owner.getLerpedPos(tickDelta).add(-MathHelper.cos(yaw) * side, owner.getHeight() + 2.2 + orb,
                -MathHelper.sin(yaw) * side);
    }

    // full size, reached at the end of the build up
    public float getOrbBase() {
        return this.dataTracker.get(ORB_BASE);
    }

    public float getOrbRadius(float tickDelta) {
        int phase = this.getPhase();
        float ticks = this.getPhaseTicks(tickDelta);
        return orbRadius(this.dataTracker.get(ORB_BASE), phase, phase == PHASE_CHARGE ? ticks : 0.0F,
                phase == PHASE_FLIGHT ? ticks : 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            this.clientTick();
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        LivingEntity owner = this.owner(world);
        RhittaConfig.CruelSunSettings cfg = RhittaConfig.get().cruelSun;
        int ticks = (int) this.getPhaseTicks(0.0F);

        switch (this.getPhase()) {
            case PHASE_CHARGE -> this.tickCharge(world, owner, cfg, ticks);
            case PHASE_FLIGHT -> this.tickFlight(world, owner, cfg, ticks);
            case PHASE_BLAST -> this.tickBlast(world, owner, cfg, ticks);
            default -> this.discard();
        }
        if (this.age > 20 * 60) {
            this.discard();
        }
    }

    private void tickCharge(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg, int ticks) {
        if (owner == null || !owner.isAlive() || !holdingAxe(owner)) {
            // swapped off the axe or died, the sun fizzles
            if (owner != null) {
                owner.removeStatusEffect(StatusEffects.SLOWNESS);
            }
            playSound(world, this.getPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, 2.0F, 0.6F);
            this.stopSounds(world, List.of(RhittaRegistry.BUILD_UP));
            this.discard();
            return;
        }
        float orb = this.getOrbRadius(0.0F);
        Vec3d at = chargeCenter(owner, 1.0F, orb);
        this.setPosition(at.x, at.y, at.z);

        if (cfg.nameText && cfg.nameTextLead < cfg.chargeDuration && ticks == Math.max(1, cfg.chargeDuration - cfg.nameTextLead)) {
            Callouts.send(world, owner.getPos(), Callout.CRUEL_SUN);
        }
        if (ticks == IGNITE_TICKS) {
            playSound(world, at, SoundEvents.ENTITY_BLAZE_SHOOT, cfg.buildUpVolume * 0.5F, 0.5F);
        }
        if (ticks >= IGNITE_TICKS && ticks % 3 == 0 && cfg.surroundingsCatchFire) {
            this.setSurroundingsAlight(world, owner, orb);
        }
        if (ticks >= IGNITE_TICKS && ticks % Math.max(1, cfg.heatInterval) == 0) {
            this.heat(world, owner, at, orb * HEAT_EDGE, null, 0.0F, cfg);
        }
        if (ticks >= cfg.chargeDuration) {
            this.launch(world, owner, cfg);
        }
    }

    private void launch(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg) {
        this.direction = this.aim(world, owner);
        this.dataTracker.set(DIRECTION, new Vector3f((float) this.direction.x, (float) this.direction.y, (float) this.direction.z));
        this.setPhase(PHASE_FLIGHT, (int) Math.ceil(cfg.range / Math.max(0.2, cfg.projectileSpeed)));
        CHARGING.remove(owner.getUuid(), this);
        // don't let the build up audio run into the throw
        this.stopSounds(world, List.of(RhittaRegistry.BUILD_UP));
        owner.removeStatusEffect(StatusEffects.SLOWNESS);
        owner.swingHand(this.hand, true);
        playSound(world, owner.getPos(), RhittaRegistry.LAUNCH.get(), cfg.launchVolume, 0.9F);
        playSound(world, owner.getPos(), SoundEvents.ENTITY_BLAZE_SHOOT, cfg.launchVolume * 0.6F, 0.6F);
    }

    // players throw it at whatever is under the crosshair, mobs at their target
    private Vec3d aim(ServerWorld world, LivingEntity owner) {
        if (owner instanceof PlayerEntity) {
            RhittaConfig.CruelSunSettings cfg = RhittaConfig.get().cruelSun;
            Vec3d eye = owner.getEyePos();
            Vec3d reach = eye.add(owner.getRotationVec(1.0F).multiply(cfg.range));
            BlockHitResult look = world.raycast(new RaycastContext(eye, reach, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, owner));
            Vec3d to = (look.getType() == HitResult.Type.BLOCK ? look.getPos() : reach).subtract(this.getPos());
            if (to.lengthSquared() > 1.0) {
                return to.normalize();
            }
        }
        if (!(owner instanceof PlayerEntity) && this.targetUuid != null
                && world.getEntity(this.targetUuid) instanceof LivingEntity target && target.isAlive()) {
            Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(this.getPos());
            if (to.lengthSquared() > 1.0E-4) {
                return to.normalize();
            }
        }
        return Vec3d.fromPolar(owner.getPitch(), owner.getHeadYaw()).normalize();
    }

    private void tickFlight(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg, int ticks) {
        float orb = this.getOrbRadius(0.0F);
        Vec3d from = this.getPos();
        Vec3d to = from.add(this.direction.multiply(cfg.projectileSpeed));

        Vec3d hit = null;
        BlockHitResult block = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this));
        if (block.getType() == HitResult.Type.BLOCK) {
            to = block.getPos().subtract(this.direction.multiply(Math.min(orb, 0.5)));
            hit = to;
        }
        Vec3d entityHit = this.firstEntityOnPath(world, owner, from, to, orb);
        if (entityHit != null) {
            hit = entityHit;
            to = entityHit;
        }

        this.traveled += from.distanceTo(to);
        this.setPosition(to.x, to.y, to.z);
        if (ticks % Math.max(1, cfg.heatInterval) == 0) {
            this.heat(world, owner, to, orb * HEAT_EDGE, this.direction.multiply(-orb * TRAIL_LENGTH), orb, cfg);
        }
        if (hit != null || this.traveled >= cfg.range) {
            this.detonate(world, owner, cfg);
        }
    }

    @Nullable
    private Vec3d firstEntityOnPath(ServerWorld world, LivingEntity owner, Vec3d from, Vec3d to, float orb) {
        Vec3d best = null;
        double bestAlong = Double.MAX_VALUE;
        Vec3d path = to.subtract(from);
        double length = path.length();
        Vec3d dir = length < 1.0E-6 ? this.direction : path.multiply(1.0 / length);
        for (Entity entity : world.getOtherEntities(this, new Box(from, to).expand(orb + 1.0))) {
            LivingEntity target = rootLiving(entity);
            if (target == null || !canHit(target, owner)) {
                continue;
            }
            Box box = entity.getBoundingBox();
            double along = MathHelper.clamp(box.getCenter().subtract(from).dotProduct(dir), 0.0, length);
            Vec3d point = from.add(dir.multiply(along));
            if (distanceToPoint(box, point) <= orb && along < bestAlong) {
                bestAlong = along;
                best = point;
            }
        }
        return best;
    }

    private void detonate(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg) {
        this.setPhase(PHASE_BLAST, BLAST_TICKS);
        float sun = this.getSun();
        Vec3d at = this.getPos();
        float volume = cfg.blastVolume * (0.7F + 0.3F * sun);
        float pitch = 1.05F - 0.25F * sun;
        playSound(world, at, RhittaRegistry.BLAST.get(), volume, pitch);
        playSound(world, at, RhittaRegistry.BLAST_ROAR.get(), volume * 0.7F, pitch * 0.9F);
        playSound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, volume * 0.6F, pitch * 0.7F);
        this.explosionBlocks(world, owner, cfg);
    }

    private void tickBlast(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg, int ticks) {
        float radius = this.getBlastRadius();
        Vec3d center = this.getPos();
        if (ticks <= BLAST_GROW) {
            this.blast(world, owner, center, blastFront(radius, ticks), radius, cfg);
        } else if (blastFade(ticks) < LINGER_CUTOFF && ticks % Math.max(1, cfg.heatInterval) == 0) {
            this.heat(world, owner, center, radius * (1.0F + FADE_SWELL * blastFade(ticks)), null, 0.0F, cfg);
        }
        if (ticks >= BLAST_TICKS) {
            this.discard();
        }
    }

    // the big hit, once per target as the edge reaches it
    private void blast(ServerWorld world, LivingEntity owner, Vec3d center, float front, float radius,
                       RhittaConfig.CruelSunSettings cfg) {
        float full = this.scaled(owner, cfg.blastDamageScaling, cfg.blastSpellScaling);
        for (Entity entity : world.getOtherEntities(this, new Box(center, center).expand(front + 2.0))) {
            LivingEntity target = rootLiving(entity);
            if (target == null || !canHit(target, owner)) {
                continue;
            }
            double dist = distanceToPoint(entity.getBoundingBox(), center);
            if (dist > front || !this.blasted.add(target.getUuid())) {
                continue;
            }
            float falloff = MathHelper.lerp((float) MathHelper.clamp(dist / radius, 0.0, 1.0), 1.0F, cfg.edgeDamage);
            if (this.hurt(world, owner, target, full * falloff)) {
                Vec3d push = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(center);
                push = push.lengthSquared() < 1.0E-4 ? new Vec3d(0, 1, 0) : push.normalize();
                double strength = cfg.knockback * falloff;
                target.addVelocity(push.x * strength, 0.25 * strength + push.y * 0.3 * strength, push.z * strength);
                target.velocityModified = true;
                target.setOnFireFor(Math.round(cfg.blastBurnSeconds * (0.5F + 0.5F * this.getSun())));
            }
        }
    }

    // heat ticks: corona, trail, lingering fireball
    private void heat(ServerWorld world, LivingEntity owner, Vec3d center, float radius, Vec3d trail,
                      float orb, RhittaConfig.CruelSunSettings cfg) {
        float damage = this.scaled(owner, cfg.heatDamageScaling, cfg.heatSpellScaling);
        Box area = new Box(center, center).expand(radius + 2.0);
        if (trail != null) {
            area = area.union(new Box(center.add(trail), center.add(trail)).expand(orb * TRAIL_WIDTH + 2.0));
        }
        Set<LivingEntity> struck = new HashSet<>();
        for (Entity entity : world.getOtherEntities(this, area)) {
            LivingEntity target = rootLiving(entity);
            if (target == null || !canHit(target, owner) || struck.contains(target)) {
                continue;
            }
            Box box = entity.getBoundingBox();
            boolean inside = distanceToPoint(box, center) <= radius
                    || (trail != null && inTrail(box, center, trail, orb));
            if (!inside) {
                continue;
            }
            struck.add(target);
            if (this.hurt(world, owner, target, damage) && cfg.heatBurnSeconds > 0) {
                target.setOnFireFor(Math.round(cfg.heatBurnSeconds));
            }
        }
    }

    // spell power, gems and runics come in through scaleAbilityDamage, the sun is applied once on top
    private float scaled(LivingEntity owner, float damageScaling, float spellScaling) {
        float base = owner == null ? damageScaling
                : SimplySwordsAPI.scaleAbilityDamage(SpellScalingProfile.ARCANE, owner, this.stack, damageScaling, spellScaling);
        return base * this.getPower();
    }

    private boolean hurt(ServerWorld world, LivingEntity owner, LivingEntity target, float amount) {
        if (amount <= 0) {
            return false;
        }
        dealing = true;
        try {
            if (target instanceof EnderDragonEntity dragon) {
                return dragon.damagePart(dragon.head, owner == null ? world.getDamageSources().magic()
                        : SimplySwordsAPI.getWeaponDamageSource(owner), amount);
            }
            if (owner == null) {
                return target.damage(world.getDamageSources().magic(), amount);
            }
            return SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, this.stack, target, amount,
                    SpellScalingProfile.ARCANE);
        } finally {
            dealing = false;
        }
    }

    // real fire, only with surroundingsCatchFire on
    private void setSurroundingsAlight(ServerWorld world, LivingEntity owner, float orb) {
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return;
        }
        double reach = Math.max(3.0, orb * 1.8);
        for (int i = 0; i < 3; i++) {
            double a = this.random.nextDouble() * MathHelper.TAU;
            double r = 1.5 + this.random.nextDouble() * reach;
            BlockPos.Mutable pos = BlockPos.ofFloored(owner.getX() + Math.cos(a) * r, owner.getY() + 3,
                    owner.getZ() + Math.sin(a) * r).mutableCopy();
            for (int d = 0; d < 7 && world.getBlockState(pos).isAir(); d++) {
                pos.move(0, -1, 0);
            }
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || !state.isFlammable(world, pos, Direction.UP)) {
                continue;
            }
            if (state.isReplaceable()) {
                world.setBlockState(pos.toImmutable(), AbstractFireBlock.getState(world, pos));
            } else if (world.getBlockState(pos.up()).isAir()) {
                world.setBlockState(pos.up(), AbstractFireBlock.getState(world, pos.up()));
            }
        }
    }

    // only with explosionBreaksBlocks / explosionStartsFires on
    private void explosionBlocks(ServerWorld world, LivingEntity owner, RhittaConfig.CruelSunSettings cfg) {
        if ((!cfg.explosionBreaksBlocks && !cfg.explosionStartsFires)
                || !world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return;
        }
        BlockPos center = this.getBlockPos();
        if (cfg.explosionBreaksBlocks) {
            int r = (int) Math.min(this.getBlastRadius() * 0.5F, 12.0F);
            for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
                if (pos.getSquaredDistance(center) > r * r) {
                    continue;
                }
                BlockState state = world.getBlockState(pos);
                float hardness = state.getHardness(world, pos);
                if (!state.isAir() && hardness >= 0 && hardness < 50) {
                    world.breakBlock(pos.toImmutable(), true, owner);
                }
            }
        }
        if (cfg.explosionStartsFires) {
            int r = (int) Math.min(this.getBlastRadius() * 0.6F, 14.0F);
            for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
                if (this.random.nextInt(5) != 0 || pos.getSquaredDistance(center) > r * r
                        || !world.getBlockState(pos).isAir()
                        || !world.getBlockState(pos.down()).isOpaqueFullCube(world, pos.down())) {
                    continue;
                }
                world.setBlockState(pos.toImmutable(), AbstractFireBlock.getState(world, pos));
            }
        }
    }

    // same taper as the drawn trail
    private static boolean inTrail(Box box, Vec3d center, Vec3d trail, float orb) {
        double length = trail.length();
        if (length < 1.0E-4) {
            return false;
        }
        Vec3d dir = trail.multiply(1.0 / length);
        double along = MathHelper.clamp(box.getCenter().subtract(center).dotProduct(dir), 0.0, length);
        float half = orb * MathHelper.lerp((float) (along / length), TRAIL_WIDTH, TRAIL_TIP);
        return distanceToPoint(box, center.add(dir.multiply(along))) <= half;
    }

    @Nullable
    private static LivingEntity rootLiving(Entity entity) {
        Entity root = entity instanceof PartEntity<?> part ? part.getParent() : entity;
        return root instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static boolean canHit(LivingEntity target, LivingEntity owner) {
        if (owner == null) {
            return true;
        }
        return target != owner && SimplySwordsAPI.isValidAbilityTarget(target, owner);
    }

    private static double distanceToPoint(Box box, Vec3d point) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean holdingAxe(LivingEntity entity) {
        return entity.getMainHandStack().getItem() instanceof DivineAxeRhittaItem
                || entity.getOffHandStack().getItem() instanceof DivineAxeRhittaItem;
    }

    private void stopSounds(ServerWorld world, List<RegistrySupplier<SoundEvent>> sounds) {
        for (RegistrySupplier<SoundEvent> sound : sounds) {
            StopSoundS2CPacket stop = new StopSoundS2CPacket(sound.getId(), SoundCategory.PLAYERS);
            for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(this) < 192 * 192)) {
                player.networkHandler.sendPacket(stop);
            }
        }
    }

    private static void playSound(ServerWorld world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        if (volume > 0) {
            world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    private void clientTick() {
        LivingEntity owner = this.getOwner();
        int phase = this.getPhase();
        CLIENT_ACTIVE.add(this);
        if (phase != this.lastPhase) {
            this.lastPhase = phase;
            this.clientPhaseStart = this.age;
        }
        if (owner != null) {
            boolean posing = phase == PHASE_CHARGE || (phase == PHASE_FLIGHT && this.getPhaseTicks(0.0F) < 10);
            if (posing) {
                CLIENT_BY_OWNER.put(owner.getId(), this);
            } else {
                CLIENT_BY_OWNER.remove(owner.getId(), this);
            }
            if (phase == PHASE_CHARGE) {
                // keep the body turned with the head so the raised pose follows the aim
                owner.prevBodyYaw = owner.prevHeadYaw;
                owner.bodyYaw = owner.headYaw;
            }
        }
        if (phase == PHASE_FLIGHT && !this.flightSoundStarted) {
            this.flightSoundStarted = true;
            RhittaClientHooks.startFlightSound(this);
        }
        RhittaClientHooks.sunParticles(this, owner);
        if (this.age % 2 == 0) {
            EffectLights.set(this, this.lightSpots(owner));
        }
    }

    // spots for EffectLights
    private List<Vec3d> lightSpots(LivingEntity owner) {
        List<Vec3d> spots = new ArrayList<>();
        int phase = this.getPhase();
        float ticks = this.getPhaseTicks(0.0F);
        if (phase == PHASE_CHARGE) {
            if (owner == null || ticks < IGNITE_TICKS) {
                return spots;
            }
            float orb = this.getOrbRadius(0.0F);
            Vec3d center = chargeCenter(owner, 1.0F, orb);
            spots.add(center.add(0, -orb, 0));
            for (int i = 0; i < 4; i++) {
                float a = i * MathHelper.HALF_PI;
                spots.add(owner.getPos().add(MathHelper.cos(a) * orb, 2.0, MathHelper.sin(a) * orb));
            }
        } else if (phase == PHASE_FLIGHT) {
            spots.add(this.getPos().add(0, -this.getOrbRadius(0.0F), 0));
            spots.add(this.getPos());
        } else if (phase == PHASE_BLAST && blastFade(ticks) < 0.7F) {
            float front = blastFront(this.getBlastRadius(), ticks);
            spots.add(this.getPos().add(0, 2.0, 0));
            for (int i = 0; i < 8; i++) {
                float a = i * MathHelper.TAU / 8.0F;
                float r = front * (i % 2 == 0 ? 0.5F : 0.95F);
                spots.add(this.getPos().add(MathHelper.cos(a) * r, 2.0, MathHelper.sin(a) * r));
            }
        }
        return spots;
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (this.getWorld().isClient()) {
            CLIENT_BY_OWNER.values().removeIf(sun -> sun == this);
            CLIENT_ACTIVE.remove(this);
            EffectLights.clear(this);
        } else {
            CHARGING.values().removeIf(sun -> sun == this);
        }
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PHASE, PHASE_CHARGE);
        this.dataTracker.startTracking(PHASE_START_AGE, 0);
        this.dataTracker.startTracking(PHASE_LENGTH, 1);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(POWER, 1.0F);
        this.dataTracker.startTracking(SUN, 0.0F);
        this.dataTracker.startTracking(ORB_BASE, 1.0F);
        this.dataTracker.startTracking(BLAST_RADIUS, 6.0F);
        this.dataTracker.startTracking(DIRECTION, new Vector3f(0, 0, 1));
    }

    private void setPhase(int phase, int length) {
        this.dataTracker.set(PHASE, phase);
        this.dataTracker.set(PHASE_START_AGE, this.age);
        this.dataTracker.set(PHASE_LENGTH, Math.max(1, length));
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    // the client counts from when it saw the phase change, the tracked start age can be a tick off
    public float getPhaseTicks(float tickDelta) {
        int start = this.getWorld().isClient() ? this.clientPhaseStart : this.dataTracker.get(PHASE_START_AGE);
        return this.age - start + tickDelta;
    }

    public float getPhaseProgress(float tickDelta) {
        return MathHelper.clamp(this.getPhaseTicks(tickDelta) / this.dataTracker.get(PHASE_LENGTH), 0.0F, 1.0F);
    }

    public float getPower() {
        return this.dataTracker.get(POWER);
    }

    // 0 at night, 1 at noon
    public float getSun() {
        return this.dataTracker.get(SUN);
    }

    public float getBlastRadius() {
        return this.dataTracker.get(BLAST_RADIUS);
    }

    public Vec3d getDirection() {
        Vector3f d = this.dataTracker.get(DIRECTION);
        return new Vec3d(d.x(), d.y(), d.z());
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID)) instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private LivingEntity owner(ServerWorld world) {
        return this.ownerUuid != null && world.getEntity(this.ownerUuid) instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean shouldRender(double distance) {
        return true;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
