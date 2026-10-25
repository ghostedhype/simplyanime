package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void simplyanime$noSwing(CallbackInfoReturnable<Boolean> cir) {
        if (SimplyAnimeClientCommon.blocksSwing()) {
            cir.setReturnValue(false);
        }
    }
}
