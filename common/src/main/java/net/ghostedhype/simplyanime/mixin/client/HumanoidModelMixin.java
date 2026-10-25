package net.ghostedhype.simplyanime.mixin.client;

import net.ghostedhype.simplyanime.client.pose.BladePoses;
import net.ghostedhype.simplyanime.client.pose.EaPoses;
import net.ghostedhype.simplyanime.client.pose.InvertedSpearPoses;
import net.ghostedhype.simplyanime.client.pose.RhittaPoses;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidModel.class, priority = 2000)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void simplyanime$poseAbilityArms(T entity, float limbAngle, float limbDistance, float animationProgress,
                                                    float headYaw, float headPitch, CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        InvertedSpearPoses.apply(entity, animationProgress, (HumanoidModel<?>) (Object) this);
        EaPoses.apply(entity, animationProgress, (HumanoidModel<?>) (Object) this);
        RhittaPoses.apply(entity, animationProgress, (HumanoidModel<?>) (Object) this);
        BladePoses.apply(entity, (HumanoidModel<?>) (Object) this, headYaw, headPitch);
    }
}
