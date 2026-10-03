package net.hussain.simplyanime.entity;

import net.hussain.simplyanime.client.EffectLights;
import net.hussain.simplyanime.client.EnumaElishPoses;
import net.hussain.simplyanime.registry.EntityRegistry;
import net.hussain.simplyanime.registry.ParticlesRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// one per cast. Follows the caster while charging, then sits at the muzzle while the beam is out
public class EnumaElishVisualEntity extends Entity {

    public static final int PHASE_CHARGE = 0;
    public static final int PHASE_RELEASE = 1;
    public static final int PHASE_BEAM = 2;
    public static final int PHASE_FADE = 3;

    // client side lookup for the arm poses, keyed by the caster's entity id
    public static final Map<Integer, EnumaElishVisualEntity> CLIENT_BY_OWNER = new HashMap<>();

    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_START_AGE =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_LENGTH =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> DIRECTION =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Float> LENGTH =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> HIT_TERRAIN =
            DataTracker.registerData(EnumaElishVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int lastPhase = -1;
    private int clientPhaseStart;

    public EnumaElishVisualEntity(EntityType<? extends EnumaElishVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public EnumaElishVisualEntity(World world, LivingEntity owner, int chargeLength, float radius) {
        this(EntityRegistry.ENUMA_ELISH_VISUAL.get(), world);
        this.setPosition(owner.getX(), owner.getY(), owner.getZ());
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(RADIUS, radius);
        this.dataTracker.set(PHASE_LENGTH, Math.max(1, chargeLength));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PHASE, PHASE_CHARGE);
        this.dataTracker.startTracking(PHASE_START_AGE, 0);
        this.dataTracker.startTracking(PHASE_LENGTH, 1);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(DIRECTION, new Vector3f(0, 0, 1));
        this.dataTracker.startTracking(LENGTH, 0.0F);
        this.dataTracker.startTracking(RADIUS, 3.0F);
        this.dataTracker.startTracking(HIT_TERRAIN, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            // the manager discards us, this is only a safety net if it never does
            if (this.age > 20 * 60) {
                this.discard();
            }
            return;
        }

        LivingEntity owner = this.getOwner();
        int phase = this.getPhase();
        if (owner != null) {
            CLIENT_BY_OWNER.put(owner.getId(), this);
            // vanilla leaves the body facing where the cast started, turn it with the head
            if (phase != PHASE_FADE) {
                owner.prevBodyYaw = owner.prevHeadYaw;
                owner.bodyYaw = owner.headYaw;
            }
        }
        if (phase != this.lastPhase) {
            this.lastPhase = phase;
            this.clientPhaseStart = this.age;
        }
        switch (phase) {
            case PHASE_CHARGE -> this.chargeParticles(owner);
            case PHASE_BEAM -> this.beamParticles();
            default -> {
            }
        }
        if (this.age % 2 == 0) {
            this.lights(owner, phase);
        }
    }

    private void lights(LivingEntity owner, int phase) {
        List<Vec3d> spots = new ArrayList<>();
        if (phase == PHASE_CHARGE && owner != null) {
            spots.add(EnumaElishPoses.chargeTip(owner, 1.0F));
        } else if (phase == PHASE_BEAM) {
            Vec3d dir = this.getDirection();
            double reach = this.getLength() * growth(this.getPhaseTicks(0.0F));
            for (double s = 2.0; s < reach && spots.size() < 14; s += 12.0) {
                spots.add(this.getPos().add(dir.multiply(s)));
            }
        }
        EffectLights.set(this, spots);
    }

    private void chargeParticles(LivingEntity owner) {
        if (owner == null) {
            return;
        }
        World world = this.getWorld();
        Random random = this.random;
        Vec3d tip = EnumaElishPoses.chargeTip(owner, 1.0F);
        float t = this.getPhaseProgress(0.0F);
        int count = 1 + (int) (t * 5);
        for (int i = 0; i < count; i++) {
            // embers pulled in
            double yaw = random.nextDouble() * MathHelper.TAU;
            double pitch = (random.nextDouble() - 0.5) * Math.PI;
            double dist = 2.5 + random.nextDouble() * 2.5;
            Vec3d from = tip.add(Math.cos(yaw) * Math.cos(pitch) * dist, Math.sin(pitch) * dist,
                    Math.sin(yaw) * Math.cos(pitch) * dist);
            Vec3d pull = tip.subtract(from).multiply(0.11 + t * 0.06);
            world.addParticle(ParticlesRegistry.RUPTURE_EMBER.get(), from.x, from.y, from.z, pull.x, pull.y, pull.z);
        }
        if (t > 0.55 && random.nextFloat() < t) {
            world.addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z,
                    (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15,
                    (random.nextDouble() - 0.5) * 0.15);
        }
        if (this.age % 3 == 0) {
            double a = random.nextDouble() * MathHelper.TAU;
            double r = 1.2 + random.nextDouble() * 1.6;
            world.addParticle(ParticleTypes.LARGE_SMOKE, owner.getX() + Math.cos(a) * r, owner.getY() + 0.1,
                    owner.getZ() + Math.sin(a) * r, 0, 0.03 + t * 0.05, 0);
        }
    }

    private void beamParticles() {
        World world = this.getWorld();
        Random random = this.random;
        Vec3d dir = this.getDirection();
        float length = this.getLength();
        float radius = this.getRadius();
        for (int i = 0; i < 24; i++) {
            double along = random.nextDouble() * length;
            double a = random.nextDouble() * MathHelper.TAU;
            Vec3d side = perpendicular(dir);
            Vec3d up = dir.crossProduct(side);
            Vec3d out = side.multiply(Math.cos(a)).add(up.multiply(Math.sin(a)));
            Vec3d p = this.getPos().add(dir.multiply(along)).add(out.multiply(profile(along, radius) * (0.9 + random.nextDouble() * 0.5)));
            Vec3d v = out.multiply(0.12 + random.nextDouble() * 0.2).add(dir.multiply(0.4));
            world.addParticle(ParticlesRegistry.RUPTURE_EMBER.get(), p.x, p.y, p.z, v.x, v.y, v.z);
        }
        if (this.isHitTerrain()) {
            Vec3d end = this.getPos().add(dir.multiply(length));
            if (this.age % 5 == 0) {
                world.addParticle(ParticleTypes.EXPLOSION, end.x + (random.nextDouble() - 0.5) * radius * 2,
                        end.y + (random.nextDouble() - 0.5) * radius * 2,
                        end.z + (random.nextDouble() - 0.5) * radius * 2, 0, 0, 0);
            }
            if (this.age % 2 == 0) {
                world.addParticle(ParticleTypes.LAVA, end.x, end.y, end.z, 0, 0, 0);
            }
            if (this.age % 3 == 0) {
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

    // how far out the beam has travelled, 0..1, it shoots out over the first few ticks
    public static float growth(float beamTicks) {
        float g = Math.min(1.0F, beamTicks / GROW_TICKS);
        return 1.0F - (1.0F - g) * (1.0F - g);
    }

    // how thick the beam still is during the fade, 1 when it starts and 0 at the end
    public static float fadeThickness(float fade) {
        return (1.0F - fade) * (1.0F - fade) * (1.0F - fade * 0.5F);
    }

    // the gold disc at the muzzle
    public static float muzzleRadius(float radius) {
        return Math.min(radius, 4.0F) * 1.4F;
    }

    // the fireball where the beam meets terrain, it swells as the beam fades
    public static float impactSize(float radius, float fade) {
        return radius * 1.2F * (1.0F + fade * 0.9F);
    }

    public static Vec3d perpendicular(Vec3d dir) {
        Vec3d helper = Math.abs(dir.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        return dir.crossProduct(helper).normalize();
    }

    public LivingEntity getOwner() {
        return this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID)) instanceof LivingEntity living ? living : null;
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase, int length) {
        this.dataTracker.set(PHASE, phase);
        this.dataTracker.set(PHASE_START_AGE, this.age);
        this.dataTracker.set(PHASE_LENGTH, Math.max(1, length));
    }

    // 0..1 through the current phase. The client counts from when it saw the phase change,
    // the tracked start age can be a tick or two off from the client's own age
    public float getPhaseProgress(float tickDelta) {
        int start = this.getWorld().isClient() ? this.clientPhaseStart : this.dataTracker.get(PHASE_START_AGE);
        return MathHelper.clamp((this.age - start + tickDelta) / this.dataTracker.get(PHASE_LENGTH), 0.0F, 1.0F);
    }

    public float getPhaseTicks(float tickDelta) {
        int start = this.getWorld().isClient() ? this.clientPhaseStart : this.dataTracker.get(PHASE_START_AGE);
        return this.age - start + tickDelta;
    }

    public void aim(Vec3d origin, Vec3d direction, float length, boolean hitTerrain) {
        this.setPosition(origin.x, origin.y, origin.z);
        this.dataTracker.set(DIRECTION, new Vector3f((float) direction.x, (float) direction.y, (float) direction.z));
        this.dataTracker.set(LENGTH, length);
        this.dataTracker.set(HIT_TERRAIN, hitTerrain);
    }

    public Vec3d getDirection() {
        Vector3f dir = this.dataTracker.get(DIRECTION);
        return new Vec3d(dir.x(), dir.y(), dir.z());
    }

    public float getLength() {
        return this.dataTracker.get(LENGTH);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public boolean isHitTerrain() {
        return this.dataTracker.get(HIT_TERRAIN);
    }

    @Override
    public boolean shouldRender(double distance) {
        return true;
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (this.getWorld().isClient()) {
            CLIENT_BY_OWNER.values().removeIf(visual -> visual == this);
            EffectLights.clear(this);
        }
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
