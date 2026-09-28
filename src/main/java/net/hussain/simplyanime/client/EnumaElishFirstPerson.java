package net.hussain.simplyanime.client;

import net.hussain.simplyanime.entity.EnumaElishVisualEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

// first person version of the charge and thrust, the third person one lives in EnumaElishPoses
public final class EnumaElishFirstPerson implements IClientItemExtensions {

    public static final EnumaElishFirstPerson INSTANCE = new EnumaElishFirstPerson();

    private EnumaElishFirstPerson() {
    }

    @Override
    public boolean applyForgeHandTransform(MatrixStack matrices, ClientPlayerEntity player, Arm arm, ItemStack stack,
                                           float tickDelta, float equipProgress, float swingProgress) {
        EnumaElishVisualEntity cast = EnumaElishVisualEntity.CLIENT_BY_OWNER.get(player.getId());
        if (cast == null || cast.isRemoved() || !isCastingHand(player, arm, stack)) {
            return false;
        }
        float s = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float t = cast.getPhaseProgress(tickDelta);
        float ticks = cast.getPhaseTicks(tickDelta);

        // same starting point vanilla uses for a held item
        matrices.translate(s * 0.56F, -0.52F, -0.72F);

        switch (cast.getPhase()) {
            case EnumaElishVisualEntity.PHASE_CHARGE -> {
                float e = InvertedSpearPoses.easeOut(Math.min(1.0F, ticks / 8.0F));
                float shake = MathHelper.sin(ticks * 2.3F) * (0.004F + 0.012F * t);
                matrices.translate(-s * 0.12F * e + shake, 0.32F * e, 0.1F * e);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F * e));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-s * 12.0F * e));
            }
            case EnumaElishVisualEntity.PHASE_RELEASE -> {
                float e = InvertedSpearPoses.easeOut(t);
                thrust(matrices, s, e, 1.0F - e);
            }
            case EnumaElishVisualEntity.PHASE_BEAM -> {
                thrust(matrices, s, 1.0F, 0.0F);
                matrices.translate(MathHelper.sin(ticks * 3.7F) * 0.012F, MathHelper.cos(ticks * 4.1F) * 0.012F, 0.0F);
            }
            case EnumaElishVisualEntity.PHASE_FADE -> thrust(matrices, s, 1.0F - InvertedSpearPoses.easeInOut(t), 0.0F);
            default -> {
                return false;
            }
        }
        return true;
    }

    // e runs 0..1 into the thrust, raised is how much of the charge pose is still left
    private static void thrust(MatrixStack matrices, float s, float e, float raised) {
        matrices.translate(-s * 0.18F * e, 0.06F * e + 0.32F * raised, -0.42F * e + 0.1F * raised);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-62.0F * e + 28.0F * raised));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * 8.0F * e));
    }

    // with one in each hand only the main hand moves
    private static boolean isCastingHand(ClientPlayerEntity player, Arm arm, ItemStack stack) {
        return arm == player.getMainArm() || player.getMainHandStack().getItem() != stack.getItem();
    }
}
