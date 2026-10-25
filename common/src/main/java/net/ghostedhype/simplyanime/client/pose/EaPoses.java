package net.ghostedhype.simplyanime.client.pose;

import net.ghostedhype.simplyanime.entity.EaVisualEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public class EaPoses {

    private static final float RAISE_TICKS = 8.0F;

    public static void apply(LivingEntity entity, float animationProgress, HumanoidModel<?> model) {
        EaVisualEntity cast = EaVisualEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (cast == null || cast.isRemoved() || cast.level() != entity.level()) {
            return;
        }
        float partialTick = Mth.clamp(animationProgress - entity.tickCount, 0.0F, 1.0F);
        float clientTickProgress = cast.getPhaseProgress(partialTick);
        float ticks = cast.getPhaseTicks(partialTick);

        boolean left = entity.getMainArm() == HumanoidArm.LEFT;
        float sideSignChange = left ? -1.0F : 1.0F;
        ModelPart main = left ? model.leftArm : model.rightArm;
        ModelPart off = left ? model.rightArm : model.leftArm;
        ModelPart frontLeg = left ? model.rightLeg : model.leftLeg;
        ModelPart backLeg = left ? model.leftLeg : model.rightLeg;
        float thrust = model.head.xRot - 0.3F;

        switch (cast.getPhase()) {
            case EaVisualEntity.PHASE_CHARGE -> {
                float ease = Ease.out(Math.min(1.0F, ticks / RAISE_TICKS));
                // the arms start shaking as the charge fills up
                float shake = Mth.sin(ticks * 2.3F) * (0.015F + 0.05F * clientTickProgress);
                main.xRot = Mth.lerp(ease, main.xRot, EaVisualEntity.RAISED) + shake;
                main.yRot = Mth.lerp(ease, main.yRot, -sideSignChange * 0.1F);
                main.zRot = Mth.lerp(ease, main.zRot, sideSignChange * 0.05F);
                off.xRot = Mth.lerp(ease, off.xRot, EaVisualEntity.RAISED + 0.15F) - shake;
                off.yRot = Mth.lerp(ease, off.yRot, sideSignChange * 0.45F);
                off.zRot = Mth.lerp(ease, off.zRot, 0.0F);
                model.rightLeg.zRot = 0.1F * ease;
                model.leftLeg.zRot = -0.1F * ease;
            }
            case EaVisualEntity.PHASE_RELEASE -> {
                float ease = Ease.out(clientTickProgress);
                thrustPose(model, main, off, frontLeg, backLeg, sideSignChange, ease, Mth.lerp(ease, EaVisualEntity.RAISED, thrust));
            }
            case EaVisualEntity.PHASE_BEAM -> {
                float recoil = Mth.sin(ticks * 3.1F) * 0.04F;
                thrustPose(model, main, off, frontLeg, backLeg, sideSignChange, 1.0F, thrust + recoil);
            }
            case EaVisualEntity.PHASE_FADE -> {
                float easeProgress = 1.0F - Ease.inOut(clientTickProgress);
                thrustPose(model, main, off, frontLeg, backLeg, sideSignChange, easeProgress, Mth.lerp(easeProgress, main.xRot, thrust));
            }
            default -> {
                return;
            }
        }
        model.hat.copyFrom(model.head);
    }

    private static void thrustPose(HumanoidModel<?> model, ModelPart main, ModelPart off, ModelPart frontLeg,
                                   ModelPart backLeg, float sideSign, float ease, float mainPitch) {
        InvertedSpearPoses.lean(model, 0.55F * ease);
        main.xRot = mainPitch;
        main.yRot = model.head.yRot * 0.6F * ease;
        main.zRot = 0.0F;
        // shoulder forward so it looks like a lunge, not a swing
        main.z -= 2.0F * ease;
        off.xRot = Mth.lerp(ease, off.xRot, 0.5F);
        off.yRot = 0.0F;
        off.zRot = -sideSign * 0.3F * ease;
        frontLeg.xRot = -0.55F * ease;
        backLeg.xRot = 0.5F * ease;
    }
}
