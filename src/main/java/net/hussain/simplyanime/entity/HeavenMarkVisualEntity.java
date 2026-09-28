package net.hussain.simplyanime.entity;

import net.hussain.simplyanime.registry.EntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class HeavenMarkVisualEntity extends Entity {

    public static final int MODE_CRACK = 0;
    public static final int MODE_LOCK_ON = 1;
    public static final int MODE_SHOCKWAVE = 2;
    public static final int MODE_DARK_ARC = 3;
    public static final int MODE_DASH_TRAIL = 4;

    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SIZE =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> FACING =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> END_OFFSET =
            DataTracker.registerData(HeavenMarkVisualEntity.class, TrackedDataHandlerRegistry.VECTOR3F);

    public HeavenMarkVisualEntity(EntityType<? extends HeavenMarkVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public HeavenMarkVisualEntity(World world, int mode, Vec3d pos, int lifetime, float size) {
        this(EntityRegistry.HEAVEN_MARK_VISUAL.get(), world);
        this.setPosition(pos.x, pos.y, pos.z);
        this.dataTracker.set(MODE, mode);
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
        this.dataTracker.set(SIZE, size);
        this.dataTracker.set(SEED, world.getRandom().nextInt());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(MODE, MODE_CRACK);
        this.dataTracker.startTracking(LIFETIME, 20);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(SIZE, 1.0F);
        this.dataTracker.startTracking(FACING, 0.0F);
        this.dataTracker.startTracking(END_OFFSET, new Vector3f());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age >= this.getLifetime()) {
            this.discard();
        }
    }

    public float getProgress(float tickDelta) {
        return Math.min(1.0F, (this.age + tickDelta) / this.getLifetime());
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getTargetId() {
        return this.dataTracker.get(TARGET_ID);
    }

    public void setTargetId(int targetId) {
        this.dataTracker.set(TARGET_ID, targetId);
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public float getSize() {
        return this.dataTracker.get(SIZE);
    }

    public float getFacing() {
        return this.dataTracker.get(FACING);
    }

    public void setFacing(float facing) {
        this.dataTracker.set(FACING, facing);
    }

    public Vec3d getEndOffset() {
        Vector3f end = this.dataTracker.get(END_OFFSET);
        return new Vec3d(end.x(), end.y(), end.z());
    }

    public void setEndOffset(Vec3d offset) {
        this.dataTracker.set(END_OFFSET, new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
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
