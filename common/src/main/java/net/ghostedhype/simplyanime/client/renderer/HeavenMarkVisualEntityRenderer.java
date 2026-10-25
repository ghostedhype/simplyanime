package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.entity.HeavenMarkVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplyswords.client.api.AbilityTargetHighlightStyle;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class HeavenMarkVisualEntityRenderer extends EntityRenderer<HeavenMarkVisualEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final AbilityTargetHighlightStyle LOCK_ON_STYLE = new AbilityTargetHighlightStyle(0xE8201E, 0xFF8A80);

    public HeavenMarkVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenMarkVisualEntity entity) {
        return SimplyAnimeRenderTypes.WHITE;
    }

    @Override
    public boolean shouldRender(HeavenMarkVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(HeavenMarkVisualEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        if (Later.take(entity, poseStack, (pose, late) -> this.render(entity, yaw, partialTick, pose, late, light))) {
            return;
        }
        if (ShaderCompat.shadowPass()) {
            return;
        }
        float progress = entity.getProgress(partialTick);
        VertexConsumer vertices = entity.getMode() == HeavenMarkVisualEntity.MODE_LOCK_ON ? null
                : buffers.getBuffer(SimplyAnimeRenderTypes.entityTranslucent(SimplyAnimeRenderTypes.WHITE));

        switch (entity.getMode()) {
            case HeavenMarkVisualEntity.MODE_CRACK -> renderCrack(entity, vertices, poseStack, progress);
            case HeavenMarkVisualEntity.MODE_LOCK_ON -> renderLockOn(entity, poseStack, buffers, partialTick);
            case HeavenMarkVisualEntity.MODE_SHOCKWAVE -> renderShockwave(entity, vertices, poseStack, progress);
            case HeavenMarkVisualEntity.MODE_DARK_ARC -> renderDarkArc(entity, vertices, poseStack, progress);
            case HeavenMarkVisualEntity.MODE_DASH_TRAIL -> renderDashTrail(entity, vertices, poseStack, progress);
            default -> {
            }
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private void renderCrack(HeavenMarkVisualEntity entity, VertexConsumer vertices, PoseStack poseStack, float progress) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        RandomSource random = RandomSource.create(entity.getSeed());
        float size = entity.getSize();
        float spread = Math.min(1.0F, progress * 5.0F);
        int alpha = (int) (235 * (progress < 0.6F ? 1.0F : 1.0F - (progress - 0.6F) / 0.4F));

        int arms = 6 + random.nextInt(3);
        for (int arm = 0; arm < arms; arm++) {
            float angle = arm * Mth.TWO_PI / arms + (random.nextFloat() - 0.5F) * 0.4F;
            float length = size * (0.6F + random.nextFloat() * 0.4F) * spread;
            drawCrackLine(vertices, matrix, normal, random, angle, 0.0F, length, 0.11F, alpha);
            float branchStart = length * (0.35F + random.nextFloat() * 0.3F);
            float branchAngle = angle + (random.nextBoolean() ? 0.6F : -0.6F);
            Vec3 branchOrigin = new Vec3(Mth.cos(angle) * branchStart, 0, Mth.sin(angle) * branchStart);
            drawCrackLine(vertices, matrix, normal, random, branchAngle, 0.0F, length * 0.35F, 0.05F, alpha, branchOrigin);
        }

        float flash = Math.max(0.0F, 1.0F - progress * 3.0F);
        if (flash > 0.0F) {
            disc(vertices, matrix, normal, size * 0.35F * (1.0F + progress), 0.03F, 255, 255, 255, (int) (200 * flash));
        }
    }

    private static void drawCrackLine(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, RandomSource random,
                                      float angle, float start, float length, float width, int alpha) {
        drawCrackLine(vertices, matrix, normal, random, angle, start, length, width, alpha, Vec3.ZERO);
    }

    private static void drawCrackLine(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, RandomSource random,
                                      float angle, float start, float length, float width, int alpha, Vec3 origin) {
        if (length <= 0.05F) {
            return;
        }
        int segments = 5;
        Vec3 previous = origin.add(Mth.cos(angle) * start, 0.02, Mth.sin(angle) * start);
        for (int i = 1; i <= segments; i++) {
            float along = start + length * i / segments;
            float jag = (random.nextFloat() - 0.5F) * 0.35F;
            Vec3 next = origin.add(Mth.cos(angle + jag / Math.max(0.5F, along)) * along, 0.02,
                    Mth.sin(angle + jag / Math.max(0.5F, along)) * along);
            float w = width * (1.0F - (i - 1) / (float) segments) + 0.015F;
            Vec3 dir = next.subtract(previous);
            Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize().scale(w);
            flatQuad(vertices, matrix, normal, previous.add(side), next.add(side), next.subtract(side),
                    previous.subtract(side), 18, 18, 22, alpha, alpha);
            previous = next;
        }
    }

    private void renderLockOn(HeavenMarkVisualEntity entity, PoseStack poseStack,
                              MultiBufferSource buffers, float partialTick) {
        Entity targetEntity = entity.level().getEntity(entity.getTargetId());
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        Vec3 offset = target.getPosition(partialTick).subtract(entity.getPosition(partialTick));
        SimplySwordsClientAPI.renderAbilityTargetHighlight(poseStack, buffers, entity.tickCount, offset,
                target.getBbWidth(), LOCK_ON_STYLE);
        // highlight switches layers, need our buffer again
        VertexConsumer vertices = buffers.getBuffer(SimplyAnimeRenderTypes.entityTranslucent(SimplyAnimeRenderTypes.WHITE));

        float time = entity.tickCount + partialTick;
        float closeIn = 1.0F - Math.min(1.0F, time / 6.0F);
        float radius = 0.28F + 0.45F * closeIn;
        float pulse = 0.8F + 0.2F * Mth.sin(time * 0.6F);

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y + target.getBbHeight() + 0.6, offset.z);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 6.0F));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int alpha = (int) (235 * pulse);
        for (int i = 0; i < 4; i++) {
            float angle = i * Mth.HALF_PI;
            chevron(vertices, matrix, normal, angle, radius, alpha);
        }
        float dot = 0.07F;
        coloredQuad(vertices, matrix, normal, new Vec3(0, dot, 0), new Vec3(dot, 0, 0), new Vec3(0, -dot, 0),
                new Vec3(-dot, 0, 0), 255, 40, 40, alpha, alpha);
        poseStack.popPose();
    }

    private static void chevron(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float angle, float radius, int alpha) {
        Vec3 out = new Vec3(Mth.cos(angle), Mth.sin(angle), 0);
        Vec3 across = new Vec3(-out.y, out.x, 0);
        Vec3 tip = out.scale(radius);
        Vec3 wingA = out.scale(radius + 0.16).add(across.scale(0.14));
        Vec3 wingB = out.scale(radius + 0.16).subtract(across.scale(0.14));
        Vec3 thick = out.scale(0.05);
        coloredQuad(vertices, matrix, normal, tip, wingA, wingA.add(thick), tip.add(thick), 255, 30, 30, alpha, alpha);
        coloredQuad(vertices, matrix, normal, tip, wingB, wingB.add(thick), tip.add(thick), 255, 30, 30, alpha, alpha);
    }

    private void renderShockwave(HeavenMarkVisualEntity entity, VertexConsumer vertices, PoseStack poseStack, float progress) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
        float size = entity.getSize();
        int alpha = (int) (240 * Math.pow(1.0F - progress, 1.4));

        ring(vertices, matrix, normal, size * eased, 0.35F * (1.0F - progress) + 0.06F, 0.18F * (1.0F - progress), alpha);
        ring(vertices, matrix, normal, size * eased * 0.62F, 0.2F * (1.0F - progress) + 0.04F, 0.1F, (int) (alpha * 0.7F));

        RandomSource random = RandomSource.create(entity.getSeed());
        for (int i = 0; i < 14; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float inner = size * eased * 0.3F;
            float outer = size * eased * (0.75F + random.nextFloat() * 0.35F);
            Vec3 dir = new Vec3(Mth.cos(angle), 0, Mth.sin(angle));
            Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(0.03);
            Vec3 a = dir.scale(inner).add(0, 0.05, 0);
            Vec3 b = dir.scale(outer).add(0, 0.05, 0);
            flatQuad(vertices, matrix, normal, a.add(side), b.add(side.scale(0.2)), b.subtract(side.scale(0.2)),
                    a.subtract(side), 255, 255, 255, alpha, 0);
        }
    }

    private void renderDarkArc(HeavenMarkVisualEntity entity, VertexConsumer vertices, PoseStack poseStack, float progress) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float facing = (entity.getFacing() + 90.0F) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(Mth.cos(facing), 0, Mth.sin(facing));
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        Vec3 tilted = side.scale(0.8).add(0, 0.6, 0).normalize();

        float radius = entity.getSize() * 0.55F;
        float sweep = Math.min(1.0F, progress * 2.2F);
        float head = Mth.lerp(1.0F - (1.0F - sweep) * (1.0F - sweep), -1.9F, 1.9F);
        float tailLength = 2.1F;
        int segments = 18;
        int fade = (int) (255 * (1.0F - Math.max(0.0F, progress - 0.45F) / 0.55F));

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = head - t0 * tailLength;
            float a1 = head - t1 * tailLength;
            Vec3 p0 = forward.scale(Mth.cos(a0) * radius).add(tilted.scale(Mth.sin(a0) * radius));
            Vec3 p1 = forward.scale(Mth.cos(a1) * radius).add(tilted.scale(Mth.sin(a1) * radius));
            Vec3 n0 = p0.normalize();
            Vec3 n1 = p1.normalize();
            float w0 = 0.32F * (1.0F - t0);
            float w1 = 0.32F * (1.0F - t1);
            int alpha0 = (int) (fade * (1.0F - t0));
            int alpha1 = (int) (fade * (1.0F - t1));
            coloredQuad(vertices, matrix, normal, p0.add(n0.scale(w0)), p1.add(n1.scale(w1)),
                    p1.subtract(n1.scale(w1 * 0.3F)), p0.subtract(n0.scale(w0 * 0.3F)),
                    12, 12, 16, alpha0, alpha1);
            coloredQuad(vertices, matrix, normal, p0.subtract(n0.scale(w0 * 0.3F)), p1.subtract(n1.scale(w1 * 0.3F)),
                    p1.subtract(n1.scale(w1 * 0.45F)), p0.subtract(n0.scale(w0 * 0.45F)),
                    235, 235, 240, alpha0, alpha1);
        }
    }

    private void renderDashTrail(HeavenMarkVisualEntity entity, VertexConsumer vertices, PoseStack poseStack, float progress) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        Vec3 path = entity.getEndOffset();
        double length = path.horizontalDistance();
        if (length < 0.2) {
            return;
        }
        Vec3 dir = path.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        int alpha = (int) (230 * (1.0F - progress));

        Vec3 groundSide = side.scale(0.3);
        flatQuad(vertices, matrix, normal, groundSide.scale(0.3), path.add(groundSide),
                path.subtract(groundSide), groundSide.scale(-0.3), 255, 255, 255, 0, (int) (alpha * 0.8F));

        RandomSource random = RandomSource.create(entity.getSeed());
        for (int i = 0; i < 9; i++) {
            double lateral = (random.nextDouble() - 0.5) * 1.1;
            double height = 0.1 + random.nextDouble() * 1.5;
            double from = random.nextDouble() * 0.4;
            double to = 0.6 + random.nextDouble() * 0.4;
            Vec3 base = side.scale(lateral).add(0, height, 0);
            Vec3 a = base.add(path.scale(from));
            Vec3 b = base.add(path.scale(to));
            Vec3 thick = new Vec3(0, 0.025, 0);
            coloredQuad(vertices, matrix, normal, a.add(thick), b.add(thick), b.subtract(thick), a.subtract(thick),
                    255, 255, 255, 0, alpha);
        }
    }

    private static void ring(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float radius, float width,
                             float lift, int alpha) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            float a0 = i * Mth.TWO_PI / segments;
            float a1 = (i + 1) * Mth.TWO_PI / segments;
            Vec3 d0 = new Vec3(Mth.cos(a0), 0, Mth.sin(a0));
            Vec3 d1 = new Vec3(Mth.cos(a1), 0, Mth.sin(a1));
            Vec3 outer0 = d0.scale(radius + width).add(0, 0.04 + lift, 0);
            Vec3 outer1 = d1.scale(radius + width).add(0, 0.04 + lift, 0);
            Vec3 inner0 = d0.scale(Math.max(0.0F, radius - width)).add(0, 0.04, 0);
            Vec3 inner1 = d1.scale(Math.max(0.0F, radius - width)).add(0, 0.04, 0);
            flatQuad(vertices, matrix, normal, inner0, inner1, outer1, outer0, 255, 255, 255, alpha, alpha);
        }
    }

    private static void disc(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, float radius, float y,
                             int r, int g, int b, int alpha) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float a0 = i * Mth.TWO_PI / segments;
            float a1 = (i + 1) * Mth.TWO_PI / segments;
            Vec3 center = new Vec3(0, y, 0);
            Vec3 p0 = new Vec3(Mth.cos(a0) * radius, y, Mth.sin(a0) * radius);
            Vec3 p1 = new Vec3(Mth.cos(a1) * radius, y, Mth.sin(a1) * radius);
            coloredQuad(vertices, matrix, normal, center, p0, p1, center, r, g, b, alpha, 0);
        }
    }

    private static void flatQuad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3 a, Vec3 b, Vec3 c,
                                 Vec3 d, int r, int g, int bl, int alphaA, int alphaB) {
        coloredQuad(vertices, matrix, normal, a, b, c, d, r, g, bl, alphaA, alphaB);
    }

    // alphaA on a/d, alphaB on b/c
    private static void coloredQuad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3 a, Vec3 b,
                                    Vec3 c, Vec3 d, int r, int g, int bl, int alphaA, int alphaB) {
        vertex(vertices, matrix, normal, a, r, g, bl, alphaA);
        vertex(vertices, matrix, normal, b, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, c, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, d, r, g, bl, alphaA);

        vertex(vertices, matrix, normal, d, r, g, bl, alphaA);
        vertex(vertices, matrix, normal, c, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, b, r, g, bl, alphaB);
        vertex(vertices, matrix, normal, a, r, g, bl, alphaA);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3 pos,
                               int r, int g, int b, int alpha) {
        vertices.addVertex(matrix, (float) pos.x / 16.0F, (float) pos.y / 16.0F, (float) pos.z / 16.0F)
                .setColor(r, g, b + 10, 255)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }
}
