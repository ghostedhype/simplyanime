package net.ghostedhype.simplyanime.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ghostedhype.simplyanime.item.DivineAxeRhittaItem;
import net.ghostedhype.simplyanime.item.EaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// first person hand poses for the weapons that have one
public class FirstPerson {

    public static boolean render(PoseStack poseStack, MultiBufferSource buffers, int light, InteractionHand hand, ItemStack stack,
                                 float partialTick, float equipProgress, float swingProgress) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.isScoping()) {
            return false;
        }
        if (BladePoses.inSkill(player)) {
            return true;
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        poseStack.pushPose();
        boolean posed = pose(poseStack, player, arm, stack, partialTick, equipProgress, swingProgress);
        if (posed) {
            boolean right = arm == HumanoidArm.RIGHT;
            client.gameRenderer.itemInHandRenderer.renderItem(player, stack,
                    right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    !right, poseStack, buffers, light);
        }
        poseStack.popPose();
        return posed;
    }

    private static boolean pose(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                float partialTick, float equipProgress, float swingProgress) {
        if (stack.getItem() instanceof EaItem) {
            return EaFirstPerson.apply(poseStack, player, arm, stack, partialTick, equipProgress, swingProgress);
        }
        if (stack.getItem() instanceof DivineAxeRhittaItem) {
            return RhittaFirstPerson.apply(poseStack, player, arm, stack, partialTick, equipProgress, swingProgress);
        }
        return false;
    }
}
