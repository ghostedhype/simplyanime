package net.hussain.simplyanime.client.renderer;

import net.hussain.simplyanime.entity.HeavenMarkVisualEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.sweenus.simplyswords.client.api.AbilityTargetHighlightStyle;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class HeavenMarkVisualEntityRenderer extends EntityRenderer<HeavenMarkVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final AbilityTargetHighlightStyle LOCK_ON_STYLE = new AbilityTargetHighlightStyle(0xE8201E, 0xFF8A80);

    public HeavenMarkVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(HeavenMarkVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(HeavenMarkVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(HeavenMarkVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float progress = entity.getProgress(tickDelta);
        VertexConsumer vertices = entity.getMode() == HeavenMarkVisualEntity.MODE_LOCK_ON ? null
                : vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE));

        switch (entity.getMode()) {
            case HeavenMarkVisualEntity.MODE_CRACK -> renderCrack(entity, vertices, matrices, progress);
            case HeavenMarkVisualEntity.MODE_LOCK_ON -> renderLockOn(entity, matrices, vertexConsumers, tickDelta);
            case HeavenMarkVisualEntity.MODE_SHOCKWAVE -> renderShockwave(entity, vertices, matrices, progress);
            case HeavenMarkVisualEntity.MODE_DARK_ARC -> renderDarkArc(entity, vertices, matrices, progress);
            case HeavenMarkVisualEntity.MODE_DASH_TRAIL -> renderDashTrail(entity, vertices, matrices, progress);
            default -> {
            }
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderCrack(HeavenMarkVisualEntity entity, VertexConsumer vertices, MatrixStack matrices, float progress) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        Random random = Random.create(entity.getSeed());
        float size = entity.getSize();
        float spread = Math.min(1.0F, progress * 5.0F);
        int alpha = (int) (235 * (progress < 0.6F ? 1.0F : 1.0F - (progress - 0.6F) / 0.4F));

        int arms = 6 + random.nextInt(3);
        for (int arm = 0; arm < arms; arm++) {
            float angle = arm * MathHelper.TAU / arms + (random.nextFloat() - 0.5F) * 0.4F;
            float length = size * (0.6F + random.nextFloat() * 0.4F) * spread;
            drawCrackLine(vertices, matrix, normal, random, angle, 0.0F, length, 0.11F, alpha);
            float branchStart = length * (0.35F + random.nextFloat() * 0.3F);
            float branchAngle = angle + (random.nextBoolean() ? 0.6F : -0.6F);
            Vec3d branchOrigin = new Vec3d(MathHelper.cos(angle) * branchStart, 0, MathHelper.sin(angle) * branchStart);
            drawCrackLine(vertices, matrix, normal, random, branchAngle, 0.0F, length * 0.35F, 0.05F, alpha, branchOrigin);
        }

        float flash = Math.max(0.0F, 1.0F - progress * 3.0F);
        if (flash > 0.0F) {
            disc(vertices, matrix, normal, size * 0.35F * (1.0F + progress), 0.03F, 255, 255, 255, (int) (200 * flash));
        }
    }

    private static void drawCrackLine(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Random random,
                                      float angle, float start, float length, float width, int alpha) {
        drawCrackLine(vertices, matrix, normal, random, angle, start, length, width, alpha, Vec3d.ZERO);
    }

    private static void drawCrackLine(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Random random,
                                      float angle, float start, float length, float width, int alpha, Vec3d origin) {
        if (length <= 0.05F) {
            return;
        }
        int segments = 5;
        Vec3d previous = origin.add(MathHelper.cos(angle) * start, 0.02, MathHelper.sin(angle) * start);
        for (int i = 1; i <= segments; i++) {
            float along = start + length * i / segments;
            float jag = (random.nextFloat() - 0.5F) * 0.35F;
            Vec3d next = origin.add(MathHelper.cos(angle + jag / Math.max(0.5F, along)) * along, 0.02,
                    MathHelper.sin(angle + jag / Math.max(0.5F, along)) * along);
            float w = width * (1.0F - (i - 1) / (float) segments) + 0.015F;
            Vec3d dir = next.subtract(previous);
            Vec3d side = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(w);
            flatQuad(vertices, matrix, normal, previous.add(side), next.add(side), next.subtract(side),
                    previous.subtract(side), 18, 18, 22, alpha, alpha);
            previous = next;
        }
    }

    private void renderLockOn(HeavenMarkVisualEntity entity, MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers, float tickDelta) {
        Entity targetEntity = entity.getWorld().getEntityById(entity.getTargetId());
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        Vec3d offset = target.getLerpedPos(tickDelta).subtract(entity.getLerpedPos(tickDelta));
        SimplySwordsClientAPI.renderAbilityTargetHighlight(matrices, vertexConsumers, entity.age, offset,
                target.getWidth(), LOCK_ON_STYLE);
        // highlight switches layers, need our buffer again
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE));

        float time = entity.age + tickDelta;
        float closeIn = 1.0F - Math.min(1.0F, time / 6.0F);
        float radius = 0.28F + 0.45F * closeIn;
        float pulse = 0.8F + 0.2F * MathHelper.sin(time * 0.6F);

        matrices.push();
        matrices.translate(offset.x, offset.y + target.getHeight() + 0.6, offset.z);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 6.0F));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        int alpha = (int) (235 * pulse);
        for (int i = 0; i < 4; i++) {
            float angle = i * MathHelper.HALF_PI;
            chevron(vertices, matrix, normal, angle, radius, alpha);
        }
        float dot = 0.07F;
        coloredQuad(vertices, matrix, normal, new Vec3d(0, dot, 0), new Vec3d(dot, 0, 0), new Vec3d(0, -dot, 0),
                new Vec3d(-dot, 0, 0), 255, 40, 40, alpha, alpha);
        matrices.pop();
    }

    private static void chevron(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float angle, float radius, int alpha) {
        Vec3d out = new Vec3d(MathHelper.cos(angle), MathHelper.sin(angle), 0);
        Vec3d across = new Vec3d(-out.y, out.x, 0);
        Vec3d tip = out.multiply(radius);
        Vec3d wingA = out.multiply(radius + 0.16).add(across.multiply(0.14));
        Vec3d wingB = out.multiply(radius + 0.16).subtract(across.multiply(0.14));
        Vec3d thick = out.multiply(0.05);
        coloredQuad(vertices, matrix, normal, tip, wingA, wingA.add(thick), tip.add(thick), 255, 30, 30, alpha, alpha);
        coloredQuad(vertices, matrix, normal, tip, wingB, wingB.add(thick), tip.add(thick), 255, 30, 30, alpha, alpha);
    }

    private void renderShockwave(HeavenMarkVisualEntity entity, VertexConsumer vertices, MatrixStack matrices, float progress) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
        float size = entity.getSize();
        int alpha = (int) (240 * Math.pow(1.0F - progress, 1.4));

        ring(vertices, matrix, normal, size * eased, 0.35F * (1.0F - progress) + 0.06F, 0.18F * (1.0F - progress), alpha);
        ring(vertices, matrix, normal, size * eased * 0.62F, 0.2F * (1.0F - progress) + 0.04F, 0.1F, (int) (alpha * 0.7F));

        Random random = Random.create(entity.getSeed());
        for (int i = 0; i < 14; i++) {
            float angle = random.nextFloat() * MathHelper.TAU;
            float inner = size * eased * 0.3F;
            float outer = size * eased * (0.75F + random.nextFloat() * 0.35F);
            Vec3d dir = new Vec3d(MathHelper.cos(angle), 0, MathHelper.sin(angle));
            Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(0.03);
            Vec3d a = dir.multiply(inner).add(0, 0.05, 0);
            Vec3d b = dir.multiply(outer).add(0, 0.05, 0);
            flatQuad(vertices, matrix, normal, a.add(side), b.add(side.multiply(0.2)), b.subtract(side.multiply(0.2)),
                    a.subtract(side), 255, 255, 255, alpha, 0);
        }
    }

    private void renderDarkArc(HeavenMarkVisualEntity entity, VertexConsumer vertices, MatrixStack matrices, float progress) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        float facing = (entity.getFacing() + 90.0F) * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(MathHelper.cos(facing), 0, MathHelper.sin(facing));
        Vec3d side = new Vec3d(-forward.z, 0, forward.x);
        Vec3d tilted = side.multiply(0.8).add(0, 0.6, 0).normalize();

        float radius = entity.getSize() * 0.55F;
        float sweep = Math.min(1.0F, progress * 2.2F);
        float head = MathHelper.lerp(1.0F - (1.0F - sweep) * (1.0F - sweep), -1.9F, 1.9F);
        float tailLength = 2.1F;
        int segments = 18;
        int fade = (int) (255 * (1.0F - Math.max(0.0F, progress - 0.45F) / 0.55F));

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = head - t0 * tailLength;
            float a1 = head - t1 * tailLength;
            Vec3d p0 = forward.multiply(MathHelper.cos(a0) * radius).add(tilted.multiply(MathHelper.sin(a0) * radius));
            Vec3d p1 = forward.multiply(MathHelper.cos(a1) * radius).add(tilted.multiply(MathHelper.sin(a1) * radius));
            Vec3d n0 = p0.normalize();
            Vec3d n1 = p1.normalize();
            float w0 = 0.32F * (1.0F - t0);
            float w1 = 0.32F * (1.0F - t1);
            int alpha0 = (int) (fade * (1.0F - t0));
            int alpha1 = (int) (fade * (1.0F - t1));
            coloredQuad(vertices, matrix, normal, p0.add(n0.multiply(w0)), p1.add(n1.multiply(w1)),
                    p1.subtract(n1.multiply(w1 * 0.3F)), p0.subtract(n0.multiply(w0 * 0.3F)),
                    12, 12, 16, alpha0, alpha1);
            coloredQuad(vertices, matrix, normal, p0.subtract(n0.multiply(w0 * 0.3F)), p1.subtract(n1.multiply(w1 * 0.3F)),
                    p1.subtract(n1.multiply(w1 * 0.45F)), p0.subtract(n0.multiply(w0 * 0.45F)),
                    235, 235, 240, alpha0, alpha1);
        }
    }

    private void renderDashTrail(HeavenMarkVisualEntity entity, VertexConsumer vertices, MatrixStack matrices, float progress) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        Vec3d path = entity.getEndOffset();
        double length = path.horizontalLength();
        if (length < 0.2) {
            return;
        }
        Vec3d dir = path.normalize();
        Vec3d side = new Vec3d(-dir.z, 0, dir.x);
        int alpha = (int) (230 * (1.0F - progress));

        Vec3d groundSide = side.multiply(0.3);
        flatQuad(vertices, matrix, normal, groundSide.multiply(0.3), path.add(groundSide),
                path.subtract(groundSide), groundSide.multiply(-0.3), 255, 255, 255, 0, (int) (alpha * 0.8F));

        Random random = Random.create(entity.getSeed());
        for (int i = 0; i < 9; i++) {
            double lateral = (random.nextDouble() - 0.5) * 1.1;
            double height = 0.1 + random.nextDouble() * 1.5;
            double from = random.nextDouble() * 0.4;
            double to = 0.6 + random.nextDouble() * 0.4;
            Vec3d base = side.multiply(lateral).add(0, height, 0);
            Vec3d a = base.add(path.multiply(from));
            Vec3d b = base.add(path.multiply(to));
            Vec3d thick = new Vec3d(0, 0.025, 0);
            coloredQuad(vertices, matrix, normal, a.add(thick), b.add(thick), b.subtract(thick), a.subtract(thick),
                    255, 255, 255, 0, alpha);
        }
    }

    private static void ring(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float radius, float width,
                             float lift, int alpha) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            float a0 = i * MathHelper.TAU / segments;
            float a1 = (i + 1) * MathHelper.TAU / segments;
            Vec3d d0 = new Vec3d(MathHelper.cos(a0), 0, MathHelper.sin(a0));
            Vec3d d1 = new Vec3d(MathHelper.cos(a1), 0, MathHelper.sin(a1));
            Vec3d outer0 = d0.multiply(radius + width).add(0, 0.04 + lift, 0);
            Vec3d outer1 = d1.multiply(radius + width).add(0, 0.04 + lift, 0);
            Vec3d inner0 = d0.multiply(Math.max(0.0F, radius - width)).add(0, 0.04, 0);
            Vec3d inner1 = d1.multiply(Math.max(0.0F, radius - width)).add(0, 0.04, 0);
            flatQuad(vertices, matrix, normal, inner0, inner1, outer1, outer0, 255, 255, 255, alpha, alpha);
        }
    }

    private static void disc(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float radius, float y,
                             int r, int g, int b, int alpha) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float a0 = i * MathHelper.TAU / segments;
            float a1 = (i + 1) * MathHelper.TAU / segments;
            Vec3d center = new Vec3d(0, y, 0);
            Vec3d p0 = new Vec3d(MathHelper.cos(a0) * radius, y, MathHelper.sin(a0) * radius);
            Vec3d p1 = new Vec3d(MathHelper.cos(a1) * radius, y, MathHelper.sin(a1) * radius);
            coloredQuad(vertices, matrix, normal, center, p0, p1, center, r, g, b, alpha, 0);
        }
    }

    private static void flatQuad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d a, Vec3d b, Vec3d c,
                                 Vec3d d, int r, int g, int bl, int alphaA, int alphaB) {
        coloredQuad(vertices, matrix, normal, a, b, c, d, r, g, bl, alphaA, alphaB);
    }

    // alphaA on a/d, alphaB on b/c
    private static void coloredQuad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d a, Vec3d b,
                                    Vec3d c, Vec3d d, int r, int g, int bl, int alphaA, int alphaB) {
        vertex(vertices, matrix, normal, a, r, g, bl, alphaA);
        vertex(vertices, matrix, normal, b, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, c, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, d, r, g, bl, alphaA);

        vertex(vertices, matrix, normal, d, r, g, bl, alphaA);
        vertex(vertices, matrix, normal, c, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, b, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, a, r, g, bl, alphaA);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3d pos,
                               int r, int g, int b, int alpha) {
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, MathHelper.clamp(alpha, 0, 255))
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .next();
    }
}
