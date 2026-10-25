package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.registry.EntityRegistry;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class EaVisualEntity extends PhasedEffectEntity {

    public static final int PHASE_CHARGE = 0;
    public static final int PHASE_RELEASE = 1;
    public static final int PHASE_BEAM = 2;
    public static final int PHASE_FADE = 3;

    public static final float RAISED = -2.15F;

    public static final Map<Integer, EaVisualEntity> CLIENT_BY_OWNER = new HashMap<>();

    private static final EntityDataAccessor<Vector3f> DIRECTION =
            SynchedEntityData.defineId(EaVisualEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(EaVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(EaVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HIT_TERRAIN =
            SynchedEntityData.defineId(EaVisualEntity.class, EntityDataSerializers.BOOLEAN);

    public EaVisualEntity(EntityType<? extends EaVisualEntity> type, Level world) {
        super(type, world);
    }

    public EaVisualEntity(Level world, LivingEntity owner, int chargeLength, float radius) {
        this(EntityRegistry.EA_VISUAL.get(), world);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setOwner(owner);
        this.entityData.set(RADIUS, radius);
        this.setPhaseLength(chargeLength);
    }

    @Override
    protected void defineData(DataDefiner definer) {
        super.defineData(definer);
        definer.define(DIRECTION, new Vector3f(0, 0, 1));
        definer.define(LENGTH, 0.0F);
        definer.define(RADIUS, 3.0F);
        definer.define(HIT_TERRAIN, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.tickCount > 20 * 60) {
                this.discard();
            }
            return;
        }

        LivingEntity owner = this.getOwner();
        int phase = this.getPhase();
        if (owner != null) {
            CLIENT_BY_OWNER.put(owner.getId(), this);
            if (phase != PHASE_FADE) {
                owner.yBodyRotO = owner.yHeadRotO;
                owner.yBodyRot = owner.yHeadRot;
            }
        }
        switch (phase) {
            case PHASE_CHARGE -> this.chargeParticles(owner);
            case PHASE_BEAM -> this.beamParticles();
            default -> {
            }
        }
        if (this.tickCount % 2 == 0) {
            this.lights(owner, phase);
        }
    }

    private void lights(LivingEntity owner, int phase) {
        List<Vec3> spots = new ArrayList<>();
        if (phase == PHASE_CHARGE && owner != null) {
            spots.add(chargeTip(owner, 1.0F));
        } else if (phase == PHASE_BEAM) {
            Vec3 direction = this.getAim();
            double reach = this.getLength() * growth(this.getPhaseTicks(0.0F));
            for (double distance = 2.0; distance < reach && spots.size() < 14; distance += 12.0) {
                spots.add(this.position().add(direction.scale(distance)));
            }
        }
        SimplyAnimeEffectLights.set(this, spots);
    }

    private void chargeParticles(LivingEntity owner) {
        if (owner == null) {
            return;
        }
        Level world = this.level();
        RandomSource random = this.random;
        Vec3 tip = chargeTip(owner, 1.0F);
        float tickProgress = this.getPhaseProgress(0.0F);
        int count = 1 + (int) (tickProgress * 5);
        for (int i = 0; i < count; i++) {
            // embers pulled in
            double yaw = random.nextDouble() * Mth.TWO_PI;
            double pitch = (random.nextDouble() - 0.5) * Math.PI;
            double dist = 2.5 + random.nextDouble() * 2.5;
            Vec3 from = tip.add(Math.cos(yaw) * Math.cos(pitch) * dist, Math.sin(pitch) * dist,
                    Math.sin(yaw) * Math.cos(pitch) * dist);
            Vec3 pull = tip.subtract(from).scale(0.11 + tickProgress * 0.06);
            world.addParticle(ParticlesRegistry.RUPTURE_EMBER.get(), from.x, from.y, from.z, pull.x, pull.y, pull.z);
        }
        if (tickProgress > 0.55 && random.nextFloat() < tickProgress) {
            world.addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z,
                    (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15,
                    (random.nextDouble() - 0.5) * 0.15);
        }
        if (this.tickCount % 3 == 0) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double r = 1.2 + random.nextDouble() * 1.6;
            world.addParticle(ParticleTypes.LARGE_SMOKE, owner.getX() + Math.cos(a) * r, owner.getY() + 0.1,
                    owner.getZ() + Math.sin(a) * r, 0, 0.03 + tickProgress * 0.05, 0);
        }
    }

    private void beamParticles() {
        Level world = this.level();
        RandomSource random = this.random;
        Vec3 direction = this.getAim();
        float length = this.getLength();
        float radius = this.getRadius();
        for (int i = 0; i < 24; i++) {
            double along = random.nextDouble() * length;
            double a = random.nextDouble() * Mth.TWO_PI;
            Vec3 side = perpendicular(direction);
            Vec3 upward = direction.cross(side);
            Vec3 out = side.scale(Math.cos(a)).add(upward.scale(Math.sin(a)));
            Vec3 point = this.position().add(direction.scale(along)).add(out.scale(profile(along, radius) * (0.9 + random.nextDouble() * 0.5)));
            Vec3 velocityVec = out.scale(0.12 + random.nextDouble() * 0.2).add(direction.scale(0.4));
            world.addParticle(ParticlesRegistry.RUPTURE_EMBER.get(), point.x, point.y, point.z, velocityVec.x, velocityVec.y, velocityVec.z);
        }
        if (this.isHitTerrain()) {
            Vec3 end = this.position().add(direction.scale(length));
            if (this.tickCount % 5 == 0) {
                world.addParticle(ParticleTypes.EXPLOSION, end.x + (random.nextDouble() - 0.5) * radius * 2,
                        end.y + (random.nextDouble() - 0.5) * radius * 2,
                        end.z + (random.nextDouble() - 0.5) * radius * 2, 0, 0, 0);
            }
            if (this.tickCount % 2 == 0) {
                world.addParticle(ParticleTypes.LAVA, end.x, end.y, end.z, 0, 0, 0);
            }
            if (this.tickCount % 3 == 0) {
                world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, end.x + (random.nextDouble() - 0.5) * radius * 2.5,
                        end.y + random.nextDouble() * radius, end.z + (random.nextDouble() - 0.5) * radius * 2.5,
                        0, 0.08 + random.nextDouble() * 0.06, 0);
            }
        }
    }

    // beam radius at a distance from the muzzle, cone shaped
    public static float profile(double along, float radius) {
        double open = Math.min(1.0, along / (radius * 2.5));
        open = open * open * (3.0 - 2.0 * open);
        return (float) (radius * (0.15 + 0.85 * open));
    }

    // shared with the renderer so hits match what's drawn
    public static final float EDGE = 1.6F;
    public static final float GROW_TICKS = 3.0F;

    public static float growth(float beamTicks) {
        float g = Math.min(1.0F, beamTicks / GROW_TICKS);
        return 1.0F - (1.0F - g) * (1.0F - g);
    }

    public static float fadeThickness(float fade) {
        return (1.0F - fade) * (1.0F - fade) * (1.0F - fade * 0.5F);
    }

    // the gold disc at the muzzle
    public static float muzzleRadius(float radius) {
        return Math.min(radius, 4.0F) * 1.4F;
    }

    public static float impactSize(float radius, float fade) {
        return radius * 1.2F * (1.0F + fade * 0.9F);
    }

    public static Vec3 chargeTip(LivingEntity entity, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0, -Mth.sin(bodyYaw));
        Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
        float scale = entity.getBbHeight() / 1.8F;
        double side = entity.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
        Vec3 shoulder = entity.getPosition(partialTick)
                .add(right.scale(side * 0.31 * scale))
                .add(0, 1.375 * scale, 0);
        Vec3 arm = forward.scale(-Mth.sin(RAISED)).add(0, -Mth.cos(RAISED), 0);
        Vec3 blade = forward.scale(Mth.cos(RAISED)).add(0, -Mth.sin(RAISED), 0);
        return shoulder.add(arm.scale(0.62 * scale)).add(blade.scale(1.8 * scale));
    }

    public static Vec3 perpendicular(Vec3 direction) {
        // This is the same functionality as SlashWave#perpendicular
        Vec3 helper = Math.abs(direction.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return direction.cross(helper).normalize();
    }

    public void aim(Vec3 origin, Vec3 direction, float length, boolean hitTerrain) {
        this.setPos(origin.x, origin.y, origin.z);
        this.entityData.set(DIRECTION, pack(direction));
        this.entityData.set(LENGTH, length);
        this.entityData.set(HIT_TERRAIN, hitTerrain);
    }

    public Vec3 getAim() {
        return unpack(this.entityData.get(DIRECTION));
    }

    public float getLength() {
        return this.entityData.get(LENGTH);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean isHitTerrain() {
        return this.entityData.get(HIT_TERRAIN);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        CLIENT_BY_OWNER.values().removeIf(visual -> visual == this);
        SimplyAnimeEffectLights.clear(this);
    }
}
