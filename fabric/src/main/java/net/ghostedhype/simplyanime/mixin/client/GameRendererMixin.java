package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void simplyanime$fov(Camera camera, float partialTick, boolean useConfiguredFov, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(SimplyAnimeClientCommon.fov(cir.getReturnValueD(), partialTick, useConfiguredFov));
    }
}
