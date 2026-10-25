package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"),
            require = 0)
    private float simplyanime$pullBack(float distance) {
        return distance + (float) SimplyAnimeClientCommon.cameraPullBack();
    }
}
