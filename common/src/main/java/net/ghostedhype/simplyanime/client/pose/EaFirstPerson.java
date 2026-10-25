package net.ghostedhype.simplyanime.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.entity.EaVisualEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class EaFirstPerson {

    public static boolean apply(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                float partialTick, float equipProgress, float swingProgress) {
        // The reason for this rename is that `cast` has a specific meaning with respect to OOP
        EaVisualEntity visualEntity = EaVisualEntity.CLIENT_BY_OWNER.get(player.getId());
        if (visualEntity == null || visualEntity.isRemoved() || !isCastingHand(player, arm, stack)) {
            return false;
        }
        float sideSignChange = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float clientTickProgress = visualEntity.getPhaseProgress(partialTick);
        float ticks = visualEntity.getPhaseTicks(partialTick);

        // same starting point vanilla uses for a held item
        poseStack.translate(sideSignChange * 0.56F, -0.52F, -0.72F);

        switch (visualEntity.getPhase()) {
            case EaVisualEntity.PHASE_CHARGE -> {
                float ease = Ease.out(Math.min(1.0F, ticks / 8.0F));
                float shake = Mth.sin(ticks * 2.3F) * (0.004F + 0.012F * clientTickProgress);
                poseStack.translate(-sideSignChange * 0.12F * ease + shake, 0.32F * ease, 0.1F * ease);
                poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * ease));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-sideSignChange * 12.0F * ease));
            }
            case EaVisualEntity.PHASE_RELEASE -> {
                float ease = Ease.out(clientTickProgress);
                thrust(poseStack, sideSignChange, ease, 1.0F - ease);
            }
            case EaVisualEntity.PHASE_BEAM -> {
                thrust(poseStack, sideSignChange, 1.0F, 0.0F);
                poseStack.translate(Mth.sin(ticks * 3.7F) * 0.012F, Mth.cos(ticks * 4.1F) * 0.012F, 0.0F);
            }
            case EaVisualEntity.PHASE_FADE -> thrust(poseStack, sideSignChange, 1.0F - Ease.inOut(clientTickProgress), 0.0F);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void thrust(PoseStack poseStack, float sideSign, float ease, float raised) {
        poseStack.translate(-sideSign * 0.18F * ease, 0.06F * ease + 0.32F * raised, -0.42F * ease + 0.1F * raised);
        poseStack.mulPose(Axis.XP.rotationDegrees(-62.0F * ease + 28.0F * raised));
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 8.0F * ease));
    }

    // with one in each hand only the main hand moves
    private static boolean isCastingHand(LocalPlayer player, HumanoidArm arm, ItemStack stack) {
        return arm == player.getMainArm() || player.getMainHandItem().getItem() != stack.getItem();
    }
}
