package net.hussain.simplyanime.client.renderer;

import net.hussain.simplyanime.client.InvertedSpearPoses;
import net.hussain.simplyanime.entity.HeavenChainVisualEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class HeavenChainVisualEntityRenderer extends EntityRenderer<HeavenChainVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int TRAIL_SAMPLES = 30;
    private static final float TRAIL_TICKS = 5.5F;
    private static final float LINK_LENGTH = 0.2F;
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private final ItemRenderer itemRenderer;

    public HeavenChainVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(HeavenChainVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(HeavenChainVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(HeavenChainVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // dash phases only drive poses, nothing to draw
        if (entity.getPhase() >= HeavenChainVisualEntity.PHASE_DASH_READY) {
            return;
        }
        Entity ownerEntity = entity.getWorld().getEntityById(entity.getOwnerId());
        if (!(ownerEntity instanceof LivingEntity owner)) {
            return;
        }

        Vec3d origin = entity.getLerpedPos(tickDelta);
        Vec3d ownerPos = owner.getLerpedPos(tickDelta);
        float phaseTime = entity.age + tickDelta - entity.getPhaseStartAge();
        int phase = entity.getPhase();
        Vec3d blade = entity.bladePos(owner, tickDelta);
        Vec3d hand = handAnchor(owner, entity, phase != HeavenChainVisualEntity.PHASE_WINDUP, tickDelta);

        VertexConsumer glow = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        if (phase == HeavenChainVisualEntity.PHASE_SPIN) {
            float t = Math.min(phaseTime, entity.getSpinDuration());
            renderSpinTrail(glow, matrix, normal, entity, origin, ownerPos, t, entity.getRadius(), entity.getBaseYaw(), 1.0F);
            // second fainter streak, one looked empty
            renderSpinTrail(glow, matrix, normal, entity, origin, ownerPos, t, entity.getRadius() * 0.75F,
                    entity.getBaseYaw() + MathHelper.PI * 0.9F, 0.55F);
        } else if (phase != HeavenChainVisualEntity.PHASE_WINDUP) {
            renderHistoryTrail(glow, matrix, normal, entity, origin, blade);
        }

        if (phase != HeavenChainVisualEntity.PHASE_WINDUP) {
            renderChain(glow, matrix, normal, hand.subtract(origin), blade.subtract(origin), light);
            renderBlade(entity, matrices, vertexConsumers, blade.subtract(origin), blade.subtract(hand),
                    phase == HeavenChainVisualEntity.PHASE_SPIN ? phaseTime : 0.0F, light);
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderSpinTrail(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                        HeavenChainVisualEntity entity, Vec3d origin, Vec3d ownerPos,
                                        float headTime, float radius, float baseYaw, float strength) {
        List<Vec3d> points = new ArrayList<>();
        List<Vec3d> widths = new ArrayList<>();
        for (int i = 0; i <= TRAIL_SAMPLES; i++) {
            float time = headTime - i * (TRAIL_TICKS / TRAIL_SAMPLES);
            if (time < 0) {
                break;
            }
            Vec3d offset = HeavenChainVisualEntity.orbitOffset(time, entity.getSpinDuration(), radius, baseYaw);
            Vec3d radial = new Vec3d(offset.x, 0, offset.z);
            radial = radial.lengthSquared() < 1.0E-4 ? UP : radial.normalize();
            points.add(ownerPos.add(offset).subtract(origin));
            widths.add(radial.multiply(0.55).add(UP.multiply(0.85)).normalize());
        }
        if (points.size() < 2) {
            return;
        }

        int count = points.size();
        for (int i = 0; i < count - 1; i++) {
            float fadeA = 1.0F - (float) i / (count - 1);
            float fadeB = 1.0F - (float) (i + 1) / (count - 1);
            float widthA = 0.75F * (float) Math.pow(fadeA, 0.6) * strength;
            float widthB = 0.75F * (float) Math.pow(fadeB, 0.6) * strength;
            Vec3d a = points.get(i);
            Vec3d b = points.get(i + 1);
            Vec3d wa = widths.get(i).multiply(widthA);
            Vec3d wb = widths.get(i + 1).multiply(widthB);

            // bands can't overlap or they z-fight
            int darkA = (int) (220 * fadeA * strength);
            int darkB = (int) (220 * fadeB * strength);
            quad(vertices, matrix, normal,
                    a.add(wa), b.add(wb), b.add(wb.multiply(0.45)), a.add(wa.multiply(0.45)),
                    10, 10, darkA, darkB, FULL_BRIGHT);

            int shadeA = (int) MathHelper.lerp(1.0F - fadeA, 255, 40);
            int shadeB = (int) MathHelper.lerp(1.0F - fadeB, 255, 40);
            quad(vertices, matrix, normal,
                    a.add(wa.multiply(0.45)), b.add(wb.multiply(0.45)),
                    b.subtract(wb.multiply(0.45)), a.subtract(wa.multiply(0.45)),
                    shadeA, shadeB, (int) (245 * fadeA * strength), (int) (245 * fadeB * strength), FULL_BRIGHT);

            quad(vertices, matrix, normal,
                    a.subtract(wa.multiply(0.45)), b.subtract(wb.multiply(0.45)),
                    b.subtract(wb.multiply(0.65)), a.subtract(wa.multiply(0.65)),
                    10, 10, darkA / 2, darkB / 2, FULL_BRIGHT);
        }
    }

    private static void renderHistoryTrail(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                           HeavenChainVisualEntity entity, Vec3d origin, Vec3d blade) {
        List<Vec3d> points = new ArrayList<>();
        points.add(blade);
        points.addAll(entity.history);
        for (int i = 0; i < points.size() - 1; i++) {
            float fadeA = 1.0F - (float) i / points.size();
            float fadeB = 1.0F - (float) (i + 1) / points.size();
            Vec3d a = points.get(i).subtract(origin);
            Vec3d b = points.get(i + 1).subtract(origin);
            quad(vertices, matrix, normal,
                    a.add(0, 0.18 * fadeA, 0), b.add(0, 0.18 * fadeB, 0),
                    b.subtract(0, 0.18 * fadeB, 0), a.subtract(0, 0.18 * fadeA, 0),
                    (int) (230 * fadeA), (int) (230 * fadeB), (int) (200 * fadeA), (int) (200 * fadeB), FULL_BRIGHT);
        }
    }

    private static void renderChain(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                    Vec3d from, Vec3d to, int light) {
        Vec3d span = to.subtract(from);
        double length = span.length();
        if (length < 0.1) {
            return;
        }
        Vec3d dir = span.multiply(1.0 / length);
        Vec3d side = dir.crossProduct(UP);
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
        Vec3d side2 = dir.crossProduct(side).normalize();

        int links = MathHelper.ceil(length / LINK_LENGTH);
        for (int k = 0; k < links; k++) {
            double start = k * LINK_LENGTH - 0.02;
            double end = Math.min(length, (k + 1) * LINK_LENGTH + 0.02);
            Vec3d a = from.add(dir.multiply(start));
            Vec3d b = from.add(dir.multiply(end));
            Vec3d across = (k & 1) == 0 ? side : side2;
            int shade = (k & 1) == 0 ? 165 : 120;
            for (int bar = -1; bar <= 1; bar += 2) {
                Vec3d offset = across.multiply(0.045 * bar);
                Vec3d thickness = across.multiply(0.016);
                quad(vertices, matrix, normal,
                        a.add(offset).add(thickness), b.add(offset).add(thickness),
                        b.add(offset).subtract(thickness), a.add(offset).subtract(thickness),
                        shade, shade, 255, 255, light);
            }
        }
    }

    private void renderBlade(HeavenChainVisualEntity entity, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers, Vec3d pos, Vec3d pointing, float spinTime, int light) {
        ItemStack stack = entity.getStack();
        if (stack.isEmpty()) {
            return;
        }
        Vec3d dir = pointing.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : pointing.normalize();
        float yawDeg = (float) (MathHelper.atan2(dir.x, dir.z) * MathHelper.DEGREES_PER_RADIAN);
        float pitchDeg = (float) (-Math.asin(MathHelper.clamp(dir.y, -1.0, 1.0)) * MathHelper.DEGREES_PER_RADIAN);

        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
        matrices.translate(0.0, 0.0, 0.32);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinTime * 35.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F));
        matrices.scale(0.85F, 0.85F, 0.85F);
        this.itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, vertexConsumers, entity.getWorld(), entity.getId());
        matrices.pop();
    }

    private static Vec3d handAnchor(LivingEntity owner, HeavenChainVisualEntity chain, boolean armOut, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (owner == client.getCameraEntity() && client.options.getPerspective().isFirstPerson()) {
            Vec3d eye = owner.getCameraPosVec(tickDelta);
            Vec3d look = owner.getRotationVec(tickDelta);
            Vec3d right = look.crossProduct(UP);
            right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
            return eye.add(look.multiply(0.5)).add(right.multiply(0.3)).add(0, -0.35, 0);
        }
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevBodyYaw, owner.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        double side = owner.getMainArm() == Arm.LEFT ? -1.0 : 1.0;
        Vec3d right = new Vec3d(-MathHelper.cos(bodyYaw), 0, -MathHelper.sin(bodyYaw)).multiply(side);
        Vec3d pos = owner.getLerpedPos(tickDelta);
        if (!armOut) {
            return pos.add(right.multiply(0.38)).add(0, owner.getHeight() * 0.42, 0);
        }
        return InvertedSpearPoses.spinHandPos(owner, chain, tickDelta);
    }

    // both windings, culling was eating half of it
    static void quad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                     int shadeAD, int shadeBC, int alphaAD, int alphaBC, int light) {
        vertex(vertices, matrix, normal, a, shadeAD, alphaAD, light);
        vertex(vertices, matrix, normal, b, shadeBC, alphaBC, light);
        vertex(vertices, matrix, normal, c, shadeBC, alphaBC, light);
        vertex(vertices, matrix, normal, d, shadeAD, alphaAD, light);

        vertex(vertices, matrix, normal, d, shadeAD, alphaAD, light);
        vertex(vertices, matrix, normal, c, shadeBC, alphaBC, light);
        vertex(vertices, matrix, normal, b, shadeBC, alphaBC, light);
        vertex(vertices, matrix, normal, a, shadeAD, alphaAD, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d pos,
                               int shade, int alpha, int light) {
        int a = MathHelper.clamp(alpha, 0, 255);
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(shade, shade, shade, a)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .next();
    }
}
