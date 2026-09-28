package net.hussain.simplyanime.client.renderer;

import net.hussain.simplyanime.entity.HeavenChainVisualEntity;
import net.hussain.simplyanime.item.InvertedSpearItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// the chain wound round the arm while the spear is just being held
public class ArmChainFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final float TURNS = 2.5F;
    private static final float LINK = 1.3F;

    private final boolean slim;

    public ArmChainFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
                                   boolean slim) {
        super(context);
        this.slim = slim;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        HeavenChainVisualEntity chain = HeavenChainVisualEntity.CLIENT_BY_OWNER.get(player.getId());
        if (chain != null && !chain.isRemoved() && chain.getWorld() == player.getWorld() && chain.isChainOut()) {
            return;
        }

        Arm arm;
        if (player.getMainHandStack().getItem() instanceof InvertedSpearItem) {
            arm = player.getMainArm();
        } else if (player.getOffHandStack().getItem() instanceof InvertedSpearItem) {
            arm = player.getMainArm().getOpposite();
        } else {
            return;
        }

        matrices.push();
        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getContextModel();
        (arm == Arm.RIGHT ? model.rightArm : model.leftArm).rotate(matrices);
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE));
        wrap(vertices, matrices.peek().getPositionMatrix(), matrices.peek().getNormalMatrix(), arm, light);
        matrices.pop();
    }

    private void wrap(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Arm arm, int light) {
        // arm box in model pixels, see PlayerEntityModel
        float halfX = this.slim ? 1.5F : 2.0F;
        float centerX = (arm == Arm.RIGHT ? -1.0F : 1.0F) * (this.slim ? 0.5F : 1.0F);
        float rx = halfX + 0.55F;
        float rz = 2.55F;
        float top = -0.5F;
        float bottom = 9.6F;

        int samples = 240;
        Vec3d previous = spiral(0, centerX, rx, rz, top, bottom);
        float travelled = 0;
        int link = 0;
        Vec3d linkStart = previous;
        for (int i = 1; i <= samples; i++) {
            Vec3d point = spiral(i / (float) samples, centerX, rx, rz, top, bottom);
            travelled += (float) point.distanceTo(previous);
            previous = point;
            if (travelled < LINK) {
                continue;
            }
            drawLink(vertices, matrix, normal, linkStart, point, centerX, link++, light);
            linkStart = point;
            travelled = 0;
        }
    }

    private static Vec3d spiral(float t, float centerX, float rx, float rz, float top, float bottom) {
        float angle = t * TURNS * MathHelper.TAU;
        return new Vec3d(centerX + MathHelper.cos(angle) * rx, MathHelper.lerp(t, top, bottom), MathHelper.sin(angle) * rz);
    }

    private static void drawLink(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d from, Vec3d to,
                                 float centerX, int index, int light) {
        Vec3d along = to.subtract(from);
        Vec3d mid = from.add(to).multiply(0.5);
        Vec3d out = new Vec3d(mid.x - centerX, 0, mid.z).normalize();
        Vec3d stretch = along.multiply(0.6);
        // alternate links lie flat on the arm and stand up off it
        boolean flat = (index & 1) == 0;
        Vec3d across = flat ? along.crossProduct(out).normalize().multiply(0.38) : out.multiply(0.42);
        Vec3d center = flat ? mid : mid.add(out.multiply(0.3));
        int shade = flat ? 196 : 128;

        Vec3d a = center.subtract(stretch).subtract(across);
        Vec3d b = center.add(stretch).subtract(across);
        Vec3d c = center.add(stretch).add(across);
        Vec3d d = center.subtract(stretch).add(across);
        Vec3d facing = flat ? out : along.crossProduct(out).normalize();
        vertex(vertices, matrix, normal, a, facing, shade, light);
        vertex(vertices, matrix, normal, b, facing, shade, light);
        vertex(vertices, matrix, normal, c, facing, shade, light);
        vertex(vertices, matrix, normal, d, facing, shade, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d pixel, Vec3d facing,
                               int shade, int light) {
        vertices.vertex(matrix, (float) pixel.x / 16.0F, (float) pixel.y / 16.0F, (float) pixel.z / 16.0F)
                .color(shade, shade, shade + 10, 255)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, (float) facing.x, (float) facing.y, (float) facing.z)
                .next();
    }
}
