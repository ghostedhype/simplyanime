package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.registry.EntityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class HeavenChainVisualEntity extends PhasedEffectEntity {

    public static final int PHASE_WINDUP = 0;
    public static final int PHASE_SPIN = 1;
    public static final int PHASE_FLING = 2;
    public static final int PHASE_RETRACT = 3;
    public static final int PHASE_DASH_READY = 4;
    public static final int PHASE_DASH = 5;
    public static final int PHASE_JAB = 6;
    public static final int PHASE_SLASH = 7;
    public static final int PHASE_RECOVER = 8;

    // one turn every 9 ticks
    public static final float SPIN_SPEED = Mth.TWO_PI / 9.0F;

    private static final EntityDataAccessor<Integer> SPIN_DURATION =
            SynchedEntityData.defineId(HeavenChainVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(HeavenChainVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_YAW =
            SynchedEntityData.defineId(HeavenChainVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> BLADE =
            SynchedEntityData.defineId(HeavenChainVisualEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(HeavenChainVisualEntity.class, EntityDataSerializers.ITEM_STACK);

    // client only, owner id -> chain
    public static final Map<Integer, HeavenChainVisualEntity> CLIENT_BY_OWNER = new HashMap<>();

    public Vec3 prevBlade;
    public Vec3 lastBlade;
    public final Deque<Vec3> history = new ArrayDeque<>();

    public HeavenChainVisualEntity(EntityType<? extends HeavenChainVisualEntity> type, Level world) {
        super(type, world);
    }

    public HeavenChainVisualEntity(Level world, LivingEntity owner, ItemStack stack, int spinDuration, float radius) {
        this(EntityRegistry.HEAVEN_CHAIN_VISUAL.get(), world);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setOwner(owner);
        this.entityData.set(STACK, stack.copyWithCount(1));
        this.entityData.set(SPIN_DURATION, spinDuration);
        this.entityData.set(RADIUS, radius);
        this.entityData.set(BASE_YAW, (owner.getYRot() + 90.0F) * Mth.DEG_TO_RAD);
        this.setBlade(owner.position().add(0, owner.getBbHeight() * 0.5, 0));
    }

    @Override
    protected void defineData(DataDefiner definer) {
        super.defineData(definer);
        definer.define(SPIN_DURATION, 50);
        definer.define(RADIUS, 4.5F);
        definer.define(BASE_YAW, 0.0F);
        definer.define(BLADE, new Vector3f());
        definer.define(STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
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
        if (this.tickCount > 400) {
            this.discard();
        }
    }

    public static Vec3 orbitOffset(float spinTime, int spinDuration, float radius, float baseYaw) {
        float progress = Mth.clamp(spinTime / Math.max(1, spinDuration), 0.0F, 1.0F);
        float grow = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, progress * 2.2F), 3);
        float r = 1.4F + (radius - 1.4F) * grow;
        float angle = baseYaw + spinTime * SPIN_SPEED;
        float tilt = Mth.sin(spinTime * 0.33F) * 0.6F;
        double y = 1.0 + Mth.sin(angle) * tilt * 1.1F;
        return new Vec3(Mth.cos(angle) * r, y, Mth.sin(angle) * r);
    }

    public Vec3 orbitOffset(float spinTime) {
        return orbitOffset(spinTime, this.getSpinDuration(), this.getRadius(), this.getBaseYaw());
    }

    public Vec3 bladePos(Entity owner, float partialTick) {
        if (this.getPhase() == PHASE_SPIN) {
            float phaseTime = this.tickCount + partialTick - this.getPhaseStartAge();
            return owner.getPosition(partialTick).add(this.orbitOffset(Math.min(phaseTime, this.getSpinDuration())));
        }
        if (this.prevBlade != null && this.lastBlade != null) {
            return this.prevBlade.lerp(this.lastBlade, partialTick);
        }
        return this.getBlade();
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        CLIENT_BY_OWNER.remove(this.getOwnerId(), this);
    }

    public void setPhase(int phase) {
        this.setPhase(phase, 1);
    }

    @Override
    protected int phaseStart() {
        return this.getPhaseStartAge();
    }

    public boolean isChainOut() {
        int phase = this.getPhase();
        return phase == PHASE_SPIN || phase == PHASE_FLING || phase == PHASE_RETRACT;
    }

    public int getSpinDuration() {
        return this.entityData.get(SPIN_DURATION);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public float getBaseYaw() {
        return this.entityData.get(BASE_YAW);
    }

    public ItemStack getStack() {
        return this.entityData.get(STACK);
    }

    public Vec3 getBlade() {
        return unpack(this.entityData.get(BLADE));
    }

    public void setBlade(Vec3 vec3) {
        this.entityData.set(BLADE, pack(vec3));
    }
}
