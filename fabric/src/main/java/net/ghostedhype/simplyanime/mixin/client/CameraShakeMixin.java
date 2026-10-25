package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void simplyanime$shake(Args args) {
        float[] shake = SimplyAnimeClientCommon.shake(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (shake != null) {
            args.set(0, (float) args.get(0) + shake[0]);
            args.set(1, (float) args.get(1) + shake[1]);
        }
    }
}
