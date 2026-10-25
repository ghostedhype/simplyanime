package net.ghostedhype.simplyanime.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public abstract class SimplyAnimeEffectEntity extends SyncedEntity {

    protected SimplyAnimeEffectEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // `vec` is fine here because it matches the class it is referencing. Therefore, it is understood.
    protected static Vector3f pack(Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    protected static Vec3 unpack(Vector3f vector3f) {
        return new Vec3(vector3f.x(), vector3f.y(), vector3f.z());
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
