package net.ghostedhype.simplyanime.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ghostedhype.simplyanime.client.pose.FirstPerson;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void simplyanime$firstPerson(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                         float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                         MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (FirstPerson.render(poseStack, buffers, light, hand, stack, partialTick, equipProgress, swingProgress)) {
            ci.cancel();
        }
    }
}
