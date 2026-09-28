package net.hussain.simplyanime.client;

import net.hussain.simplyanime.entity.HeavenChainVisualEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class InvertedSpearPoses {

    private InvertedSpearPoses() {
    }

    public static void apply(LivingEntity entity, float animationProgress, BipedEntityModel<?> model) {
        HeavenChainVisualEntity ability = HeavenChainVisualEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (ability == null || ability.isRemoved() || ability.getWorld() != entity.getWorld()) {
            return;
        }
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0F, 1.0F);
        float t = ability.getPhaseProgress(tickDelta);

        boolean left = entity.getMainArm() == Arm.LEFT;
        float s = left ? -1.0F : 1.0F;
        ModelPart main = left ? model.leftArm : model.rightArm;
        ModelPart off = left ? model.rightArm : model.leftArm;
        ModelPart frontLeg = left ? model.rightLeg : model.leftLeg;
        ModelPart backLeg = left ? model.leftLeg : model.rightLeg;

        switch (ability.getPhase()) {
            case HeavenChainVisualEntity.PHASE_WINDUP -> {
                float e = easeOut(t);
                main.pitch = MathHelper.lerp(e, main.pitch, -2.8F);
                main.yaw = MathHelper.lerp(e, main.yaw, 0.0F);
                main.roll = MathHelper.lerp(e, main.roll, s * 0.15F);
            }
            case HeavenChainVisualEntity.PHASE_SPIN, HeavenChainVisualEntity.PHASE_FLING,
                    HeavenChainVisualEntity.PHASE_RETRACT -> {
                Vec3d local = spinArmDirection(entity, ability, tickDelta);
                pointArm(main, local);
                off.pitch = -0.2F;
                off.yaw = 0.0F;
                off.roll = -s * 0.55F;
                model.rightLeg.roll = 0.12F;
                model.leftLeg.roll = -0.12F;
                lean(model, 0.25F);
            }
            case HeavenChainVisualEntity.PHASE_DASH_READY -> {
                float e = easeOut(t);
                lean(model, 0.8F * e);
                main.pitch = 0.7F * e;
                main.yaw = 0.0F;
                main.roll = s * 0.25F * e;
                off.pitch = -0.9F * e;
                off.yaw = 0.0F;
                off.roll = -s * 0.1F * e;
                frontLeg.pitch = -0.45F * e;
                backLeg.pitch = 0.4F * e;
            }
            case HeavenChainVisualEntity.PHASE_DASH -> {
                lean(model, 1.0F);
                main.pitch = -1.5F;
                main.yaw = -s * 0.15F;
                main.roll = 0.0F;
                off.pitch = 0.9F;
                off.yaw = 0.0F;
                off.roll = -s * 0.2F;
                frontLeg.pitch = -0.7F;
                backLeg.pitch = 0.75F;
            }
            case HeavenChainVisualEntity.PHASE_JAB -> {
                lean(model, MathHelper.lerp(t, 0.8F, 0.65F));
                main.pitch = -1.6F + 0.35F * t;
                main.yaw = -s * 0.1F;
                main.roll = 0.0F;
                off.pitch = 0.6F;
                off.yaw = 0.0F;
                off.roll = -s * 0.2F;
                frontLeg.pitch = -0.55F;
                backLeg.pitch = 0.5F;
            }
            case HeavenChainVisualEntity.PHASE_SLASH -> {
                // high on the weapon side, then down and across
                float e = easeOut(Math.min(1.0F, t * 1.4F));
                lean(model, MathHelper.lerp(e, 0.65F, 0.85F));
                main.pitch = MathHelper.lerp(e, -2.7F, -0.55F);
                main.yaw = MathHelper.lerp(e, s * 0.55F, -s * 0.95F);
                main.roll = MathHelper.lerp(e, s * 0.35F, -s * 0.15F);
                off.pitch = 0.5F;
                off.yaw = 0.0F;
                off.roll = -s * 0.35F;
                frontLeg.pitch = -0.6F;
                backLeg.pitch = 0.55F;
            }
            case HeavenChainVisualEntity.PHASE_RECOVER -> {
                float k = 1.0F - easeInOut(t);
                lean(model, 0.85F * k);
                main.pitch = MathHelper.lerp(k, main.pitch, -0.55F);
                main.yaw = MathHelper.lerp(k, main.yaw, -s * 0.95F);
                main.roll = MathHelper.lerp(k, main.roll, -s * 0.15F);
                off.pitch = MathHelper.lerp(k, off.pitch, 0.5F);
                off.roll = MathHelper.lerp(k, off.roll, -s * 0.35F);
                frontLeg.pitch = MathHelper.lerp(k, frontLeg.pitch, -0.6F);
                backLeg.pitch = MathHelper.lerp(k, backLeg.pitch, 0.55F);
            }
            default -> {
                return;
            }
        }
        model.hat.copyTransform(model.head);
    }

    // world position of the raised fist while spinning, the chain renderer starts there
    public static Vec3d spinHandPos(LivingEntity entity, HeavenChainVisualEntity ability, float tickDelta) {
        Vec3d local = spinArmDirection(entity, ability, tickDelta);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        Vec3d right = new Vec3d(-MathHelper.cos(bodyYaw), 0, -MathHelper.sin(bodyYaw));
        Vec3d forward = new Vec3d(-MathHelper.sin(bodyYaw), 0, MathHelper.cos(bodyYaw));
        float scale = entity.getHeight() / 1.8F;
        double side = entity.getMainArm() == Arm.LEFT ? -1.0 : 1.0;
        Vec3d shoulder = entity.getLerpedPos(tickDelta)
                .add(right.multiply(side * 0.31 * scale))
                .add(0, 1.375 * scale, 0);
        Vec3d arm = right.multiply(local.x).add(0, local.y, 0).add(forward.multiply(local.z));
        return shoulder.add(arm.multiply(0.62 * scale));
    }

    // arm direction as (right, up, forward) in the body's frame: mostly straight up,
    // leaning toward the blade so the fist circles overhead with it
    private static Vec3d spinArmDirection(LivingEntity entity, HeavenChainVisualEntity ability, float tickDelta) {
        Vec3d pos = entity.getLerpedPos(tickDelta);
        Vec3d blade = ability.bladePos(entity, tickDelta);
        Vec3d flat = new Vec3d(blade.x - pos.x, 0, blade.z - pos.z);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        Vec3d right = new Vec3d(-MathHelper.cos(bodyYaw), 0, -MathHelper.sin(bodyYaw));
        Vec3d forward = new Vec3d(-MathHelper.sin(bodyYaw), 0, MathHelper.cos(bodyYaw));
        double toRight = 0;
        double toFront = 0;
        if (flat.lengthSquared() > 1.0E-4) {
            flat = flat.normalize();
            toRight = flat.dotProduct(right);
            toFront = flat.dotProduct(forward);
        }
        double side = entity.getMainArm() == Arm.LEFT ? -1.0 : 1.0;
        return new Vec3d(toRight * 0.35 - side * 0.15, 0.95, toFront * 0.35 + 0.08).normalize();
    }

    // aims a hanging arm along (right, up, forward). Model space has y down, -z forward, -x right.
    private static void pointArm(ModelPart arm, Vec3d local) {
        double mx = -local.x;
        double my = -local.y;
        double mz = -local.z;
        double c = Math.sqrt(Math.max(0.0, 1.0 - mz * mz));
        float pitch = (float) Math.atan2(mz, -c);
        if (pitch > 0) {
            pitch -= MathHelper.TAU;
        }
        arm.pitch = pitch;
        arm.yaw = 0.0F;
        arm.roll = (float) Math.atan2(mx, -my);
    }

    // same offsets vanilla uses for sneaking, scaled by amount
    static void lean(BipedEntityModel<?> model, float amount) {
        model.body.pitch = 0.5F * amount;
        model.body.pivotY = 3.2F * amount;
        model.head.pivotY = 4.2F * amount;
        model.rightArm.pivotY = 2.0F + 3.2F * amount;
        model.leftArm.pivotY = 2.0F + 3.2F * amount;
        model.rightLeg.pivotY = 12.0F + 0.2F * amount;
        model.leftLeg.pivotY = 12.0F + 0.2F * amount;
        model.rightLeg.pivotZ = 0.1F + 3.9F * amount;
        model.leftLeg.pivotZ = 0.1F + 3.9F * amount;
    }

    static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    static float easeInOut(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
