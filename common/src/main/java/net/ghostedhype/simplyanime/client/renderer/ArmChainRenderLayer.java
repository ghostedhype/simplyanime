package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ghostedhype.simplyanime.entity.HeavenChainVisualEntity;
import net.ghostedhype.simplyanime.item.InvertedSpearItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ArmChainRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final float TURNS = 2.5F;
    private static final float LINK = 1.3F;

    public ArmChainRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
        super(context);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbAngle, float limbDistance, float partialTick,
                       float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        HeavenChainVisualEntity chain = HeavenChainVisualEntity.CLIENT_BY_OWNER.get(player.getId());
        if (chain != null && !chain.isRemoved() && chain.level() == player.level() && chain.isChainOut()) {
            return;
        }

        HumanoidArm arm;
        if (player.getMainHandItem().getItem() instanceof InvertedSpearItem) {
            arm = player.getMainArm();
        } else if (player.getOffhandItem().getItem() instanceof InvertedSpearItem) {
            arm = player.getMainArm().getOpposite();
        } else {
            return;
        }

        poseStack.pushPose();
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        (arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm).translateAndRotate(poseStack);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(SimplyAnimeRenderTypes.WHITE));
        wrap(vertices, poseStack.last().pose(), poseStack.last().normal(), arm, player.getSkin().model() == PlayerSkin.Model.SLIM, light);
        poseStack.popPose();
    }

    private void wrap(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, HumanoidArm arm, boolean slim, int light) {
        // arm box in model pixels, see PlayerEntityModel
        float halfX = slim ? 1.5F : 2.0F;
        float centerX = (arm == HumanoidArm.RIGHT ? -1.0F : 1.0F) * (slim ? 0.5F : 1.0F);
        float rx = halfX + 0.55F;
        float rz = 2.55F;
        float top = -0.5F;
        float bottom = 9.6F;

        int samples = 240;
        Vec3 previous = spiral(0, centerX, rx, rz, top, bottom);
        float travelled = 0;
        int link = 0;
        Vec3 linkStart = previous;
        for (int i = 1; i <= samples; i++) {
            Vec3 point = spiral(i / (float) samples, centerX, rx, rz, top, bottom);
            travelled += (float) point.distanceTo(previous);
            previous = point;
            if (travelled < LINK) {
                continue;
            }
            drawLink(vertices, matrix, linkStart, point, centerX, link++, light);
            linkStart = point;
            travelled = 0;
        }
    }

    private static Vec3 spiral(float t, float centerX, float rx, float rz, float top, float bottom) {
        float angle = t * TURNS * Mth.TWO_PI;
        return new Vec3(centerX + Mth.cos(angle) * rx, Mth.lerp(t, top, bottom), Mth.sin(angle) * rz);
    }

    private static void drawLink(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to,
                                 float centerX, int index, int light) {
        Vec3 along = to.subtract(from);
        Vec3 mid = from.add(to).scale(0.5);
        Vec3 out = new Vec3(mid.x - centerX, 0, mid.z).normalize();
        Vec3 stretch = along.scale(0.6);
        // alternate links lie flat on the arm and stand up off it
        boolean flat = (index & 1) == 0;
        Vec3 across = flat ? along.cross(out).normalize().scale(0.38) : out.scale(0.42);
        Vec3 center = flat ? mid : mid.add(out.scale(0.3));
        int shade = flat ? 196 : 128;

        Vec3 a = center.subtract(stretch).subtract(across);
        Vec3 b = center.add(stretch).subtract(across);
        Vec3 c = center.add(stretch).add(across);
        Vec3 d = center.subtract(stretch).add(across);
        Vec3 facing = flat ? out : along.cross(out).normalize();
        vertex(vertices, matrix, a, facing, shade, light);
        vertex(vertices, matrix, b, facing, shade, light);
        vertex(vertices, matrix, c, facing, shade, light);
        vertex(vertices, matrix, d, facing, shade, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,Vec3 pixel, Vec3 facing,
                               int shade, int light) {
        vertices.addVertex(matrix, (float) pixel.x / 16.0F, (float) pixel.y / 16.0F, (float) pixel.z / 16.0F)
                .setColor(shade, shade, shade + 10, 255)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal((float) facing.x, (float) facing.y, (float) facing.z);
    }
}