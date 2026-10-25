package net.ghostedhype.simplyanime.mixin;

import net.ghostedhype.simplyanime.ability.RhittaGrowth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void simplyanime$growDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        float scale = RhittaGrowth.sizeScale((Entity) (Object) this);
        if (scale != 1.0F) {
            cir.setReturnValue(cir.getReturnValue().scale(scale));
        }
    }
}
