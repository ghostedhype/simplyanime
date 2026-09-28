package net.hussain.simplyanime.mixin.client;

import net.hussain.simplyanime.client.EnumaElishPoses;
import net.hussain.simplyanime.client.InvertedSpearPoses;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void simplyanime$poseAbilityArms(T entity, float limbAngle, float limbDistance, float animationProgress,
                                                    float headYaw, float headPitch, CallbackInfo ci) {
        InvertedSpearPoses.apply(entity, animationProgress, (BipedEntityModel<?>) (Object) this);
        EnumaElishPoses.apply(entity, animationProgress, (BipedEntityModel<?>) (Object) this);
    }
}
