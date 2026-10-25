package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.registry.EntityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class HeavenMarkVisualEntity extends SimplyAnimeEffectEntity {

    public static final int MODE_CRACK = 0;
    public static final int MODE_LOCK_ON = 1;
    public static final int MODE_SHOCKWAVE = 2;
    public static final int MODE_DARK_ARC = 3;
    public static final int MODE_DASH_TRAIL = 4;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FACING =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> END_OFFSET =
            SynchedEntityData.defineId(HeavenMarkVisualEntity.class, EntityDataSerializers.VECTOR3);

    public HeavenMarkVisualEntity(EntityType<? extends HeavenMarkVisualEntity> type, Level world) {
        super(type, world);
    }

    public HeavenMarkVisualEntity(Level world, int mode, Vec3 vec3, int lifetime, float size) {
        this(EntityRegistry.HEAVEN_MARK_VISUAL.get(), world);
        this.setPos(vec3.x, vec3.y, vec3.z);
        this.entityData.set(MODE, mode);
        this.entityData.set(LIFETIME, Math.max(1, lifetime));
        this.entityData.set(SIZE, size);
        this.entityData.set(SEED, world.getRandom().nextInt());
    }

    @Override
    protected void defineData(DataDefiner definer) {
        definer.define(MODE, MODE_CRACK);
        definer.define(LIFETIME, 20);
        definer.define(TARGET_ID, -1);
        definer.define(SEED, 0);
        definer.define(SIZE, 1.0F);
        definer.define(FACING, 0.0F);
        definer.define(END_OFFSET, new Vector3f());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.getLifetime()) {
            this.discard();
        }
    }

    public float getProgress(float partialTick) {
        return Math.min(1.0F, (this.tickCount + partialTick) / this.getLifetime());
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int targetId) {
        this.entityData.set(TARGET_ID, targetId);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public float getFacing() {
        return this.entityData.get(FACING);
    }

    public void setFacing(float facing) {
        this.entityData.set(FACING, facing);
    }

    public Vec3 getEndOffset() {
        return unpack(this.entityData.get(END_OFFSET));
    }

    public void setEndOffset(Vec3 offset) {
        this.entityData.set(END_OFFSET, pack(offset));
    }
}
