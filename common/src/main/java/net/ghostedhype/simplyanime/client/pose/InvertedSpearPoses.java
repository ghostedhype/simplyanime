package net.ghostedhype.simplyanime.client.pose;

import net.ghostedhype.simplyanime.entity.HeavenChainVisualEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class InvertedSpearPoses {

    public static void apply(LivingEntity entity, float animationProgress, HumanoidModel<?> model) {
        HeavenChainVisualEntity ability = HeavenChainVisualEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (ability == null || ability.isRemoved() || ability.level() != entity.level()) {
            return;
        }
        float partialTick = Mth.clamp(animationProgress - entity.tickCount, 0.0F, 1.0F);
        float clientTickProgress = ability.getPhaseProgress(partialTick);

        boolean left = entity.getMainArm() == HumanoidArm.LEFT;
        float sideSignChange = left ? -1.0F : 1.0F;
        ModelPart main = left ? model.leftArm : model.rightArm;
        ModelPart off = left ? model.rightArm : model.leftArm;
        ModelPart frontLeg = left ? model.rightLeg : model.leftLeg;
        ModelPart backLeg = left ? model.leftLeg : model.rightLeg;

        switch (ability.getPhase()) {
            case HeavenChainVisualEntity.PHASE_WINDUP -> {
                float easeProgress = Ease.out(clientTickProgress);
                main.xRot = Mth.lerp(easeProgress, main.xRot, -2.8F);
                main.yRot = Mth.lerp(easeProgress, main.yRot, 0.0F);
                main.zRot = Mth.lerp(easeProgress, main.zRot, sideSignChange * 0.15F);
            }
            case HeavenChainVisualEntity.PHASE_SPIN, HeavenChainVisualEntity.PHASE_FLING,
                    HeavenChainVisualEntity.PHASE_RETRACT -> {
                Vec3 local = spinArmDirection(entity, ability, partialTick);
                pointArm(main, local);
                off.xRot = -0.2F;
                off.yRot = 0.0F;
                off.zRot = -sideSignChange * 0.55F;
                model.rightLeg.zRot = 0.12F;
                model.leftLeg.zRot = -0.12F;
                lean(model, 0.25F);
            }
            case HeavenChainVisualEntity.PHASE_DASH_READY -> {
                float easeProgress = Ease.out(clientTickProgress);
                lean(model, 0.8F * easeProgress);
                main.xRot = 0.7F * easeProgress;
                main.yRot = 0.0F;
                main.zRot = sideSignChange * 0.25F * easeProgress;
                off.xRot = -0.9F * easeProgress;
                off.yRot = 0.0F;
                off.zRot = -sideSignChange * 0.1F * easeProgress;
                frontLeg.xRot = -0.45F * easeProgress;
                backLeg.xRot = 0.4F * easeProgress;
            }
            case HeavenChainVisualEntity.PHASE_DASH -> {
                lean(model, 1.0F);
                main.xRot = -1.5F;
                main.yRot = -sideSignChange * 0.15F;
                main.zRot = 0.0F;
                off.xRot = 0.9F;
                off.yRot = 0.0F;
                off.zRot = -sideSignChange * 0.2F;
                frontLeg.xRot = -0.7F;
                backLeg.xRot = 0.75F;
            }
            case HeavenChainVisualEntity.PHASE_JAB -> {
                lean(model, Mth.lerp(clientTickProgress, 0.8F, 0.65F));
                main.xRot = -1.6F + 0.35F * clientTickProgress;
                main.yRot = -sideSignChange * 0.1F;
                main.zRot = 0.0F;
                off.xRot = 0.6F;
                off.yRot = 0.0F;
                off.zRot = -sideSignChange * 0.2F;
                frontLeg.xRot = -0.55F;
                backLeg.xRot = 0.5F;
            }
            case HeavenChainVisualEntity.PHASE_SLASH -> {
                // high on the weapon side, then down and across
                float easeProgress = Ease.out(Math.min(1.0F, clientTickProgress * 1.4F));
                lean(model, Mth.lerp(easeProgress, 0.65F, 0.85F));
                main.xRot = Mth.lerp(easeProgress, -2.7F, -0.55F);
                main.yRot = Mth.lerp(easeProgress, sideSignChange * 0.55F, -sideSignChange * 0.95F);
                main.zRot = Mth.lerp(easeProgress, sideSignChange * 0.35F, -sideSignChange * 0.15F);
                off.xRot = 0.5F;
                off.yRot = 0.0F;
                off.zRot = -sideSignChange * 0.35F;
                frontLeg.xRot = -0.6F;
                backLeg.xRot = 0.55F;
            }
            case HeavenChainVisualEntity.PHASE_RECOVER -> {
                float easeProgress = 1.0F - Ease.inOut(clientTickProgress);
                lean(model, 0.85F * easeProgress);
                main.xRot = Mth.lerp(easeProgress, main.xRot, -0.55F);
                main.yRot = Mth.lerp(easeProgress, main.yRot, -sideSignChange * 0.95F);
                main.zRot = Mth.lerp(easeProgress, main.zRot, -sideSignChange * 0.15F);
                off.xRot = Mth.lerp(easeProgress, off.xRot, 0.5F);
                off.zRot = Mth.lerp(easeProgress, off.zRot, -sideSignChange * 0.35F);
                frontLeg.xRot = Mth.lerp(easeProgress, frontLeg.xRot, -0.6F);
                backLeg.xRot = Mth.lerp(easeProgress, backLeg.xRot, 0.55F);
            }
            default -> {
                return;
            }
        }
        model.hat.copyFrom(model.head);
    }

    public static Vec3 spinHandPos(LivingEntity entity, HeavenChainVisualEntity ability, float partialTick) {
        Vec3 armDirectionVec = spinArmDirection(entity, ability, partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0, -Mth.sin(bodyYaw));
        Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
        float scale = entity.getBbHeight() / 1.8F;
        double side = entity.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
        Vec3 shoulder = entity.getPosition(partialTick)
                .add(right.scale(side * 0.31 * scale))
                .add(0, 1.375 * scale, 0);
        Vec3 arm = right.scale(armDirectionVec.x).add(0, armDirectionVec.y, 0).add(forward.scale(armDirectionVec.z));
        return shoulder.add(arm.scale(0.62 * scale));
    }

    private static Vec3 spinArmDirection(LivingEntity entity, HeavenChainVisualEntity ability, float partialTick) {
        Vec3 pos = entity.getPosition(partialTick);
        Vec3 blade = ability.bladePos(entity, partialTick);
        Vec3 flat = new Vec3(blade.x - pos.x, 0, blade.z - pos.z);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0, -Mth.sin(bodyYaw));
        Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
        double toRight = 0;
        double toFront = 0;
        if (flat.lengthSqr() > 1.0E-4) {
            flat = flat.normalize();
            toRight = flat.dot(right);
            toFront = flat.dot(forward);
        }
        double side = entity.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
        return new Vec3(toRight * 0.35 - side * 0.15, 0.95, toFront * 0.35 + 0.08).normalize();
    }

    private static void pointArm(ModelPart arm, Vec3 local) {
        double mirroredX = -local.x;
        double mirroredY = -local.y;
        double mirroredZ = -local.z;
        double flatLength = Math.sqrt(Math.max(0.0, 1.0 - mirroredZ * mirroredZ));
        float pitch = (float) Math.atan2(mirroredZ, -flatLength);
        if (pitch > 0) {
            pitch -= Mth.TWO_PI;
        }
        arm.xRot = pitch;
        arm.yRot = 0.0F;
        arm.zRot = (float) Math.atan2(mirroredX, -mirroredY);
    }

    static void lean(HumanoidModel<?> model, float amount) {
        model.body.xRot = 0.5F * amount;
        model.body.y = 3.2F * amount;
        model.head.y = 4.2F * amount;
        model.rightArm.y = 2.0F + 3.2F * amount;
        model.leftArm.y = 2.0F + 3.2F * amount;
        model.rightLeg.y = 12.0F + 0.2F * amount;
        model.leftLeg.y = 12.0F + 0.2F * amount;
        model.rightLeg.z = 0.1F + 3.9F * amount;
        model.leftLeg.z = 0.1F + 3.9F * amount;
    }
}
