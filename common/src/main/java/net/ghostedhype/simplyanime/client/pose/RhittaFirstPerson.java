package net.ghostedhype.simplyanime.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class RhittaFirstPerson {

    public static boolean apply(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                float partialTick, float equipProgress, float swingProgress) {
        if (arm != player.getMainArm()) {
            return false;
        }
        float sideSignChange = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        // what vanilla does for a held item
        poseStack.translate(sideSignChange * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        if (swingProgress > 0) {
            float swingWindUp = Mth.sin(swingProgress * swingProgress * Mth.PI);
            float swingWindDown = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSignChange * (45.0F + swingWindUp * -20.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSignChange * swingWindDown * -20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(swingWindDown * -80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSignChange * -45.0F));
        }

        CruelSunEntity cast = CruelSunEntity.CLIENT_BY_OWNER.get(player.getId());
        if (cast != null && !cast.isRemoved()) {
            float ticks = cast.getPhaseTicks(partialTick);
            if (cast.getPhase() == CruelSunEntity.PHASE_CHARGE) {
                float ease = Ease.out(Math.min(1.0F, ticks / 8.0F));
                float shake = Mth.sin(ticks * 2.1F) * 0.01F * cast.getPhaseProgress(partialTick);
                poseStack.translate(-sideSignChange * 0.1F * ease + shake, 0.5F * ease, 0.15F * ease);
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * ease));
            } else {
                float k = Ease.out(Math.min(1.0F, ticks / 4.0F));
                float back = Mth.clamp((ticks - 5.0F) / 5.0F, 0.0F, 1.0F);
                float raise = (1.0F - k) * (1.0F - back);
                float throwing = k * (1.0F - back);
                poseStack.translate(0, 0.5F * raise - 0.1F * throwing, 0.15F * raise - 0.3F * throwing);
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * raise - 55.0F * throwing));
            }
            return true;
        }

        float weight = RhittaPoses.weight(player, partialTick);
        if (weight > 0.001F) {
            // over the shoulder, slow sway
            float time = player.tickCount + partialTick;
            float sway = Mth.sin(time * 0.07F);
            poseStack.translate(sideSignChange * 0.08F * weight, (0.14F + 0.01F * sway) * weight, 0.2F * weight);
            poseStack.mulPose(Axis.XP.rotationDegrees((34.0F + 1.5F * sway) * weight));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-sideSignChange * 14.0F * weight));
        }
        return true;
    }
}
