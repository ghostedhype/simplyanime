package net.hussain.simplyanime.client;

import net.hussain.simplyanime.entity.EnumaElishVisualEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class EnumaElishPoses {

    // arm raised forward and up, which leaves the blade pointing up and back over the head
    private static final float RAISED = -2.15F;
    private static final float RAISE_TICKS = 8.0F;

    private EnumaElishPoses() {
    }

    public static void apply(LivingEntity entity, float animationProgress, BipedEntityModel<?> model) {
        EnumaElishVisualEntity cast = EnumaElishVisualEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (cast == null || cast.isRemoved() || cast.getWorld() != entity.getWorld()) {
            return;
        }
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0F, 1.0F);
        float t = cast.getPhaseProgress(tickDelta);
        float ticks = cast.getPhaseTicks(tickDelta);

        boolean left = entity.getMainArm() == Arm.LEFT;
        float s = left ? -1.0F : 1.0F;
        ModelPart main = left ? model.leftArm : model.rightArm;
        ModelPart off = left ? model.rightArm : model.leftArm;
        ModelPart frontLeg = left ? model.rightLeg : model.leftLeg;
        ModelPart backLeg = left ? model.leftLeg : model.rightLeg;
        float thrust = model.head.pitch - 0.3F;

        switch (cast.getPhase()) {
            case EnumaElishVisualEntity.PHASE_CHARGE -> {
                float e = InvertedSpearPoses.easeOut(Math.min(1.0F, ticks / RAISE_TICKS));
                // the arms start shaking as the charge fills up
                float shake = MathHelper.sin(ticks * 2.3F) * (0.015F + 0.05F * t);
                main.pitch = MathHelper.lerp(e, main.pitch, RAISED) + shake;
                main.yaw = MathHelper.lerp(e, main.yaw, -s * 0.1F);
                main.roll = MathHelper.lerp(e, main.roll, s * 0.05F);
                off.pitch = MathHelper.lerp(e, off.pitch, RAISED + 0.15F) - shake;
                off.yaw = MathHelper.lerp(e, off.yaw, s * 0.45F);
                off.roll = MathHelper.lerp(e, off.roll, 0.0F);
                model.rightLeg.roll = 0.1F * e;
                model.leftLeg.roll = -0.1F * e;
            }
            case EnumaElishVisualEntity.PHASE_RELEASE -> {
                float e = InvertedSpearPoses.easeOut(t);
                thrustPose(model, main, off, frontLeg, backLeg, s, e, MathHelper.lerp(e, RAISED, thrust));
            }
            case EnumaElishVisualEntity.PHASE_BEAM -> {
                float recoil = MathHelper.sin(ticks * 3.1F) * 0.04F;
                thrustPose(model, main, off, frontLeg, backLeg, s, 1.0F, thrust + recoil);
            }
            case EnumaElishVisualEntity.PHASE_FADE -> {
                float k = 1.0F - InvertedSpearPoses.easeInOut(t);
                thrustPose(model, main, off, frontLeg, backLeg, s, k, MathHelper.lerp(k, main.pitch, thrust));
            }
            default -> {
                return;
            }
        }
        model.hat.copyTransform(model.head);
    }

    private static void thrustPose(BipedEntityModel<?> model, ModelPart main, ModelPart off, ModelPart frontLeg,
                                   ModelPart backLeg, float s, float e, float mainPitch) {
        InvertedSpearPoses.lean(model, 0.55F * e);
        main.pitch = mainPitch;
        main.yaw = model.head.yaw * 0.6F * e;
        main.roll = 0.0F;
        // push the sword shoulder forward so it reads as a lunge rather than a swing
        main.pivotZ -= 2.0F * e;
        off.pitch = MathHelper.lerp(e, off.pitch, 0.5F);
        off.yaw = 0.0F;
        off.roll = -s * 0.3F * e;
        frontLeg.pitch = -0.55F * e;
        backLeg.pitch = 0.5F * e;
    }

    // rough world position of the blade tip while it is held up, the charge effects gather there
    public static Vec3d chargeTip(LivingEntity entity, float tickDelta) {
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        Vec3d right = new Vec3d(-MathHelper.cos(bodyYaw), 0, -MathHelper.sin(bodyYaw));
        Vec3d forward = new Vec3d(-MathHelper.sin(bodyYaw), 0, MathHelper.cos(bodyYaw));
        float scale = entity.getHeight() / 1.8F;
        double side = entity.getMainArm() == Arm.LEFT ? -1.0 : 1.0;
        Vec3d shoulder = entity.getLerpedPos(tickDelta)
                .add(right.multiply(side * 0.31 * scale))
                .add(0, 1.375 * scale, 0);
        // arm hangs along -y in the model, pitching it by RAISED swings it up in front
        Vec3d arm = forward.multiply(-MathHelper.sin(RAISED)).add(0, -MathHelper.cos(RAISED), 0);
        Vec3d blade = forward.multiply(MathHelper.cos(RAISED)).add(0, -MathHelper.sin(RAISED), 0);
        return shoulder.add(arm.multiply(0.62 * scale)).add(blade.multiply(1.3 * scale));
    }
}
