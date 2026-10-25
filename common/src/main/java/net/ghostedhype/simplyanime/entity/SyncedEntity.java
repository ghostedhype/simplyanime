package net.ghostedhype.simplyanime.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class SyncedEntity extends Entity {

    protected SyncedEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected abstract void defineData(DataDefiner definer);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        this.defineData(builder::define);
    }
}
