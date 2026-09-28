package net.hussain.simplyanime.mixin.client;

import net.hussain.simplyanime.client.EnumaElishPoses;
import net.hussain.simplyanime.client.InvertedSpearPoses;
import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.mob.HostileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// zombies redo their arms after super.setAngles
@Mixin(AbstractZombieModel.class)
public abstract class AbstractZombieModelMixin<T extends HostileEntity> {

    @Inject(method = "setAngles(Lnet/minecraft/entity/mob/HostileEntity;FFFFF)V", at = @At("TAIL"))
    private void simplyanime$poseAbilityArms(T entity, float limbAngle, float limbDistance, float animationProgress,
                                                    float headYaw, float headPitch, CallbackInfo ci) {
        InvertedSpearPoses.apply(entity, animationProgress, (BipedEntityModel<?>) (Object) this);
        EnumaElishPoses.apply(entity, animationProgress, (BipedEntityModel<?>) (Object) this);
    }
}
