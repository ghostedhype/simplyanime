package net.hussain.simplyanime.entity;

import net.hussain.simplyanime.registry.EntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class HeavenChainVisualEntity extends Entity {

    public static final int PHASE_WINDUP = 0;
    public static final int PHASE_SPIN = 1;
    public static final int PHASE_FLING = 2;
    public static final int PHASE_RETRACT = 3;
    // Twin Heaven Strike reuses this entity so clients know which pose to play
    public static final int PHASE_DASH_READY = 4;
    public static final int PHASE_DASH = 5;
    public static final int PHASE_JAB = 6;
    public static final int PHASE_SLASH = 7;
    public static final int PHASE_RECOVER = 8;

    // one turn every 9 ticks
    public static final float SPIN_SPEED = MathHelper.TAU / 9.0F;

    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_START_AGE =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_LENGTH =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPIN_DURATION =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> BASE_YAW =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> BLADE =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<ItemStack> STACK =
            DataTracker.registerData(HeavenChainVisualEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

    // client only, owner id -> chain
    public static final Map<Integer, HeavenChainVisualEntity> CLIENT_BY_OWNER = new HashMap<>();

    public Vec3d prevBlade;
    public Vec3d lastBlade;
    public final Deque<Vec3d> history = new ArrayDeque<>();

    public HeavenChainVisualEntity(EntityType<? extends HeavenChainVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public HeavenChainVisualEntity(World world, LivingEntity owner, ItemStack stack, int spinDuration, float radius) {
        this(EntityRegistry.HEAVEN_CHAIN_VISUAL.get(), world);
        this.setPosition(owner.getX(), owner.getY(), owner.getZ());
        this.dataTracker.set(OWNER_ID, owner.getId());
        this.dataTracker.set(STACK, stack.copyWithCount(1));
        this.dataTracker.set(SPIN_DURATION, spinDuration);
        this.dataTracker.set(RADIUS, radius);
        this.dataTracker.set(BASE_YAW, (owner.getYaw() + 90.0F) * MathHelper.RADIANS_PER_DEGREE);
        this.setBlade(owner.getPos().add(0, owner.getHeight() * 0.5, 0));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PHASE, PHASE_WINDUP);
        this.dataTracker.startTracking(PHASE_START_AGE, 0);
        this.dataTracker.startTracking(PHASE_LENGTH, 1);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(SPIN_DURATION, 50);
        this.dataTracker.startTracking(RADIUS, 4.5F);
        this.dataTracker.startTracking(BASE_YAW, 0.0F);
        this.dataTracker.startTracking(BLADE, new Vector3f());
        this.dataTracker.startTracking(STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            CLIENT_BY_OWNER.put(this.getOwnerId(), this);
            this.prevBlade = this.lastBlade == null ? this.getBlade() : this.lastBlade;
            this.lastBlade = this.getBlade();
            this.history.addFirst(this.lastBlade);
            while (this.history.size() > 6) {
                this.history.removeLast();
            }
            return;
        }
        // orphaned
        if (this.age > 400) {
            this.discard();
        }
    }

    // used by both the hit check and the renderer so they line up
    public static Vec3d orbitOffset(float spinTime, int spinDuration, float radius, float baseYaw) {
        float progress = MathHelper.clamp(spinTime / Math.max(1, spinDuration), 0.0F, 1.0F);
        float grow = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, progress * 2.2F), 3);
        float r = 1.4F + (radius - 1.4F) * grow;
        float angle = baseYaw + spinTime * SPIN_SPEED;
        float tilt = MathHelper.sin(spinTime * 0.33F) * 0.6F;
        double y = 1.0 + MathHelper.sin(angle) * tilt * 1.1F;
        return new Vec3d(MathHelper.cos(angle) * r, y, MathHelper.sin(angle) * r);
    }

    public Vec3d orbitOffset(float spinTime) {
        return orbitOffset(spinTime, this.getSpinDuration(), this.getRadius(), this.getBaseYaw());
    }

    public Vec3d bladePos(Entity owner, float tickDelta) {
        if (this.getPhase() == PHASE_SPIN) {
            float phaseTime = this.age + tickDelta - this.getPhaseStartAge();
            return owner.getLerpedPos(tickDelta).add(this.orbitOffset(Math.min(phaseTime, this.getSpinDuration())));
        }
        if (this.prevBlade != null && this.lastBlade != null) {
            return this.prevBlade.lerp(this.lastBlade, tickDelta);
        }
        return this.getBlade();
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (this.getWorld().isClient()) {
            CLIENT_BY_OWNER.remove(this.getOwnerId(), this);
        }
    }

    public void setPhase(int phase) {
        this.setPhase(phase, 1);
    }

    public void setPhase(int phase, int length) {
        this.dataTracker.set(PHASE, phase);
        this.dataTracker.set(PHASE_START_AGE, this.age);
        this.dataTracker.set(PHASE_LENGTH, Math.max(1, length));
    }

    public float getPhaseProgress(float tickDelta) {
        float time = this.age + tickDelta - this.getPhaseStartAge();
        return MathHelper.clamp(time / this.dataTracker.get(PHASE_LENGTH), 0.0F, 1.0F);
    }

    public boolean isChainOut() {
        int phase = this.getPhase();
        return phase == PHASE_SPIN || phase == PHASE_FLING || phase == PHASE_RETRACT;
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public int getPhaseStartAge() {
        return this.dataTracker.get(PHASE_START_AGE);
    }

    public int getOwnerId() {
        return this.dataTracker.get(OWNER_ID);
    }

    public int getSpinDuration() {
        return this.dataTracker.get(SPIN_DURATION);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public float getBaseYaw() {
        return this.dataTracker.get(BASE_YAW);
    }

    public ItemStack getStack() {
        return this.dataTracker.get(STACK);
    }

    public Vec3d getBlade() {
        Vector3f blade = this.dataTracker.get(BLADE);
        return new Vec3d(blade.x(), blade.y(), blade.z());
    }

    public void setBlade(Vec3d pos) {
        this.dataTracker.set(BLADE, new Vector3f((float) pos.x, (float) pos.y, (float) pos.z));
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
