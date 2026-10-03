package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.rhitta.RhittaGrowth;
import net.hussain.simplyanime.rhitta.config.RhittaClientConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.WeakHashMap;

// Escanor's idle: axe over the shoulder, hand on hip. fades out for swings and the cast
public class RhittaPoses {

    // prev, current pose weight
    private static final Map<LivingEntity, float[]> WEIGHTS = new WeakHashMap<>();

    // arm swung back and up, positive so blending in from a normal pose goes backward and not over the top
    private static final float SHOULDER_PITCH = 1.65F;

    public static void tick(ClientWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            float[] w = WEIGHTS.computeIfAbsent(player, p -> new float[2]);
            w[0] = w[1];
            float target = idle(player) ? 1.0F : 0.0F;
            // eases in slowly, gets out of the way fast for a swing
            w[1] = target > w[1] ? Math.min(target, w[1] + 0.1F) : Math.max(target, w[1] - 0.34F);
        }
    }

    private static boolean idle(PlayerEntity player) {
        return RhittaClientConfig.get().idlePose && RhittaGrowth.holding(player) && !player.handSwinging
                && !player.isUsingItem() && !player.isSwimming() && !player.isFallFlying() && !player.hasVehicle()
                && !player.isSleeping() && !player.isSpectator()
                && (player.getPose() == EntityPose.STANDING || player.getPose() == EntityPose.CROUCHING)
                && !CruelSunEntity.CLIENT_BY_OWNER.containsKey(player.getId());
    }

    public static float weight(LivingEntity entity, float tickDelta) {
        float[] w = WEIGHTS.get(entity);
        if (w == null) {
            return 0.0F;
        }
        float k = MathHelper.lerp(tickDelta, w[0], w[1]);
        return k * k * (3.0F - 2.0F * k);
    }

    public static void apply(LivingEntity entity, float animationProgress, BipedEntityModel<?> model) {
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0F, 1.0F);
        boolean left = entity.getMainArm() == Arm.LEFT;
        float s = left ? -1.0F : 1.0F;
        ModelPart main = left ? model.leftArm : model.rightArm;
        ModelPart off = left ? model.rightArm : model.leftArm;

        CruelSunEntity cast = CruelSunEntity.CLIENT_BY_OWNER.get(entity.getId());
        if (cast != null && !cast.isRemoved() && cast.getWorld() == entity.getWorld()) {
            castPose(cast, tickDelta, main, off, s);
            model.hat.copyTransform(model.head);
            return;
        }

        float w = weight(entity, tickDelta);
        if (w <= 0.001F) {
            return;
        }
        // shaft across the back behind the head, tuned against the held item transform so nothing clips
        main.pitch = MathHelper.lerp(w, main.pitch, SHOULDER_PITCH);
        main.yaw = MathHelper.lerp(w, main.yaw, s * 1.125F);
        main.roll = MathHelper.lerp(w, main.roll, -s * 1.75F);
        // chest out, chin up
        model.body.pitch = MathHelper.lerp(w, model.body.pitch, model.body.pitch - 0.06F);
        model.head.pitch = MathHelper.lerp(w, model.head.pitch, model.head.pitch - 0.14F);
        model.hat.copyTransform(model.head);
    }

    private static void castPose(CruelSunEntity cast, float tickDelta, ModelPart main, ModelPart off, float s) {
        float ticks = cast.getPhaseTicks(tickDelta);
        if (cast.getPhase() == CruelSunEntity.PHASE_CHARGE) {
            // held straight up
            float e = easeOut(Math.min(1.0F, ticks / 8.0F));
            float shake = MathHelper.sin(ticks * 2.1F) * 0.03F * cast.getPhaseProgress(tickDelta);
            main.pitch = MathHelper.lerp(e, main.pitch, -3.05F) + shake;
            main.yaw = MathHelper.lerp(e, main.yaw, -s * 0.05F);
            main.roll = MathHelper.lerp(e, main.roll, s * 0.12F);
            off.pitch = MathHelper.lerp(e, off.pitch, -2.7F) - shake;
            off.yaw = MathHelper.lerp(e, off.yaw, s * 0.3F);
            off.roll = MathHelper.lerp(e, off.roll, -s * 0.1F);
        } else {
            // the throw
            float k = Math.min(1.0F, ticks / 4.0F);
            float back = MathHelper.clamp((ticks - 5.0F) / 5.0F, 0.0F, 1.0F);
            float pitch = MathHelper.lerp(easeOut(k), -3.05F, -1.1F);
            main.pitch = MathHelper.lerp(back, pitch, main.pitch);
            main.yaw = MathHelper.lerp(back, -s * 0.1F, main.yaw);
            main.roll = MathHelper.lerp(back, 0.0F, main.roll);
            off.pitch = MathHelper.lerp(back, 0.4F, off.pitch);
            off.roll = MathHelper.lerp(back, -s * 0.3F, off.roll);
        }
    }

    static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }
}
