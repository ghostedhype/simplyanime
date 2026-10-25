package net.ghostedhype.simplyanime.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class PhasedEffectEntity extends SimplyAnimeEffectEntity {

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(PhasedEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_START_AGE =
            SynchedEntityData.defineId(PhasedEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_LENGTH =
            SynchedEntityData.defineId(PhasedEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(PhasedEffectEntity.class, EntityDataSerializers.INT);

    private int lastPhase = -1;
    private int clientPhaseStart;

    protected PhasedEffectEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(DataDefiner definer) {
        definer.define(PHASE, 0);
        definer.define(PHASE_START_AGE, 0);
        definer.define(PHASE_LENGTH, 1);
        definer.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        int phase = this.getPhase();
        if (this.level().isClientSide() && phase != this.lastPhase) {
            this.lastPhase = phase;
            this.clientPhaseStart = this.tickCount;
        }
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPhase(int phase, int length) {
        this.entityData.set(PHASE, phase);
        this.entityData.set(PHASE_START_AGE, this.tickCount);
        this.setPhaseLength(length);
    }

    protected void setPhaseLength(int length) {
        this.entityData.set(PHASE_LENGTH, Math.max(1, length));
    }

    public int getPhaseStartAge() {
        return this.entityData.get(PHASE_START_AGE);
    }

    protected int phaseStart() {
        return this.level().isClientSide() ? this.clientPhaseStart : this.getPhaseStartAge();
    }

    public float getPhaseTicks(float partialTick) {
        return this.tickCount - this.phaseStart() + partialTick;
    }

    // 0..1 through the current phase
    public float getPhaseProgress(float partialTick) {
        return Mth.clamp(this.getPhaseTicks(partialTick) / this.entityData.get(PHASE_LENGTH), 0.0F, 1.0F);
    }

    protected void setOwner(LivingEntity owner) {
        this.entityData.set(OWNER_ID, owner.getId());
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public LivingEntity getOwner() {
        return this.level().getEntity(this.getOwnerId()) instanceof LivingEntity living ? living : null;
    }
}
