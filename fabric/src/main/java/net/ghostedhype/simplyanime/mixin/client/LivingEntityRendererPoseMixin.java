package net.ghostedhype.simplyanime.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public abstract class LivingEntityRendererPoseMixin<T extends LivingEntity> {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void simplyanime$pushPose(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                      int light, CallbackInfo ci) {
        poseStack.pushPose();
        SimplyAnimeClientCommon.beforeRender(entity, poseStack, partialTick);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void simplyanime$popPose(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                     int light, CallbackInfo ci) {
        poseStack.popPose();
    }
}
