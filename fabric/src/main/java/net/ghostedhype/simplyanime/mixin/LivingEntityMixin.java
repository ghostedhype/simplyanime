package net.ghostedhype.simplyanime.mixin;

import net.ghostedhype.simplyanime.CommonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float simplyanime$scaleDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.level().isClientSide() ? amount : CommonEvents.scaleDamage(self, source, amount);
    }
}
