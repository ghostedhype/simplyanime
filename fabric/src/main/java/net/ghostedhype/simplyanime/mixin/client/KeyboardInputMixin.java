package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplyanime$holdInput(boolean sneaking, float sneakingSpeedMultiplier, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            SimplyAnimeClientCommon.holdInput(Minecraft.getInstance().player, this);
        }
    }
}
