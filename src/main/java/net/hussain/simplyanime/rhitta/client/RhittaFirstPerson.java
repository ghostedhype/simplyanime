package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

// vanilla hold and swing with the shoulder rest blended on top, the cast has its own raise and throw
public class RhittaFirstPerson implements IClientItemExtensions {

    public static final RhittaFirstPerson INSTANCE = new RhittaFirstPerson();

    @Override
    public boolean applyForgeHandTransform(MatrixStack matrices, ClientPlayerEntity player, Arm arm, ItemStack stack,
                                           float tickDelta, float equipProgress, float swingProgress) {
        if (arm != player.getMainArm()) {
            return false;
        }
        float s = arm == Arm.RIGHT ? 1.0F : -1.0F;

        // what vanilla does for a held item
        matrices.translate(s * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        if (swingProgress > 0) {
            float f = MathHelper.sin(swingProgress * swingProgress * MathHelper.PI);
            float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * MathHelper.PI);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * (45.0F + f * -20.0F)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s * g * -20.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * -45.0F));
        }

        CruelSunEntity cast = CruelSunEntity.CLIENT_BY_OWNER.get(player.getId());
        if (cast != null && !cast.isRemoved()) {
            float ticks = cast.getPhaseTicks(tickDelta);
            if (cast.getPhase() == CruelSunEntity.PHASE_CHARGE) {
                float e = RhittaPoses.easeOut(Math.min(1.0F, ticks / 8.0F));
                float shake = MathHelper.sin(ticks * 2.1F) * 0.01F * cast.getPhaseProgress(tickDelta);
                matrices.translate(-s * 0.1F * e + shake, 0.5F * e, 0.15F * e);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * e));
            } else {
                float k = RhittaPoses.easeOut(Math.min(1.0F, ticks / 4.0F));
                float back = MathHelper.clamp((ticks - 5.0F) / 5.0F, 0.0F, 1.0F);
                float raise = (1.0F - k) * (1.0F - back);
                float throwing = k * (1.0F - back);
                matrices.translate(0, 0.5F * raise - 0.1F * throwing, 0.15F * raise - 0.3F * throwing);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * raise - 55.0F * throwing));
            }
            return true;
        }

        float w = RhittaPoses.weight(player, tickDelta);
        if (w > 0.001F) {
            // over the shoulder, slow sway
            float time = player.age + tickDelta;
            float sway = MathHelper.sin(time * 0.07F);
            matrices.translate(s * 0.08F * w, (0.14F + 0.01F * sway) * w, 0.2F * w);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((34.0F + 1.5F * sway) * w));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-s * 14.0F * w));
        }
        return true;
    }
}
