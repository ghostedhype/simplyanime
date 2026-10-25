package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ghostedhype.simplyanime.client.pose.InvertedSpearPoses;
import net.ghostedhype.simplyanime.entity.HeavenChainVisualEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class HeavenChainVisualEntityRenderer extends EntityRenderer<HeavenChainVisualEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int TRAIL_SAMPLES = 30;
    private static final float TRAIL_TICKS = 5.5F;
    private static final float LINK_LENGTH = 0.2F;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private final ItemRenderer itemRenderer;

    public HeavenChainVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenChainVisualEntity entity) {
        return SimplyAnimeRenderTypes.WHITE;
    }

    @Override
    public boolean shouldRender(HeavenChainVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(HeavenChainVisualEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        if (Later.take(entity, poseStack, (pose, late) -> this.render(entity, yaw, partialTick, pose, late, light))) {
            return;
        }
        // dash phases only drive poses, nothing to draw
        if (entity.getPhase() >= HeavenChainVisualEntity.PHASE_DASH_READY) {
            return;
        }
        Entity ownerEntity = entity.level().getEntity(entity.getOwnerId());
        if (!(ownerEntity instanceof LivingEntity owner)) {
            return;
        }

        Vec3 origin = entity.getPosition(partialTick);
        Vec3 ownerPos = owner.getPosition(partialTick);
        float phaseTime = entity.tickCount + partialTick - entity.getPhaseStartAge();
        int phase = entity.getPhase();
        Vec3 blade = entity.bladePos(owner, partialTick);
        Vec3 hand = handAnchor(owner, entity, phase != HeavenChainVisualEntity.PHASE_WINDUP, partialTick);

        VertexConsumer glow = buffers.getBuffer(SimplyAnimeRenderTypes.entityTranslucent(SimplyAnimeRenderTypes.WHITE));
        PoseStack.Pose entry = poseStack.last();
        Matrix4f matrix = entry.pose();
        Matrix3f normal = entry.normal();

        if (!ShaderCompat.shadowPass()) {
            if (phase == HeavenChainVisualEntity.PHASE_SPIN) {
                float t = Math.min(phaseTime, entity.getSpinDuration());
                renderSpinTrail(glow, matrix, normal, entity, origin, ownerPos, t, entity.getRadius(), entity.getBaseYaw(), 1.0F);
                // second fainter streak, one looked empty
                renderSpinTrail(glow, matrix, normal, entity, origin, ownerPos, t, entity.getRadius() * 0.75F,
                        entity.getBaseYaw() + Mth.PI * 0.9F, 0.55F);
            } else if (phase != HeavenChainVisualEntity.PHASE_WINDUP) {
                renderHistoryTrail(glow, matrix, normal, entity, origin, blade);
            }
        }

        if (phase != HeavenChainVisualEntity.PHASE_WINDUP) {
            VertexConsumer links = buffers.getBuffer(RenderType.entityTranslucent(SimplyAnimeRenderTypes.WHITE));
            renderChain(links, matrix, normal, hand.subtract(origin), blade.subtract(origin), light);
            renderBlade(entity, poseStack, buffers, blade.subtract(origin), blade.subtract(hand),
                    phase == HeavenChainVisualEntity.PHASE_SPIN ? phaseTime : 0.0F, light);
        }

        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private static void renderSpinTrail(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                        HeavenChainVisualEntity entity, Vec3 origin, Vec3 ownerPos,
                                        float headTime, float radius, float baseYaw, float strength) {
        List<Vec3> points = new ArrayList<>();
        List<Vec3> widths = new ArrayList<>();
        for (int i = 0; i <= TRAIL_SAMPLES; i++) {
            float time = headTime - i * (TRAIL_TICKS / TRAIL_SAMPLES);
            if (time < 0) {
                break;
            }
            Vec3 offset = HeavenChainVisualEntity.orbitOffset(time, entity.getSpinDuration(), radius, baseYaw);
            Vec3 radial = new Vec3(offset.x, 0, offset.z);
            radial = radial.lengthSqr() < 1.0E-4 ? UP : radial.normalize();
            points.add(ownerPos.add(offset).subtract(origin));
            widths.add(radial.scale(0.55).add(UP.scale(0.85)).normalize());
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
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            Vec3 wa = widths.get(i).scale(widthA);
            Vec3 wb = widths.get(i + 1).scale(widthB);

            // bands can't overlap or they z-fight
            int darkA = (int) (220 * fadeA * strength);
            int darkB = (int) (220 * fadeB * strength);
            quad(vertices, matrix, normal,
                    a.add(wa), b.add(wb), b.add(wb.scale(0.45)), a.add(wa.scale(0.45)),
                    10, 10, darkA, darkB, FULL_BRIGHT);

            int shadeA = (int) Mth.lerpInt(1.0F - fadeA, 255, 40);
            int shadeB = (int) Mth.lerpInt(1.0F - fadeB, 255, 40);
            quad(vertices, matrix, normal,
                    a.add(wa.scale(0.45)), b.add(wb.scale(0.45)),
                    b.subtract(wb.scale(0.45)), a.subtract(wa.scale(0.45)),
                    shadeA, shadeB, (int) (245 * fadeA * strength), (int) (245 * fadeB * strength), FULL_BRIGHT);

            quad(vertices, matrix, normal,
                    a.subtract(wa.scale(0.45)), b.subtract(wb.scale(0.45)),
                    b.subtract(wb.scale(0.65)), a.subtract(wa.scale(0.65)),
                    10, 10, darkA / 2, darkB / 2, FULL_BRIGHT);
        }
    }

    private static void renderHistoryTrail(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                           HeavenChainVisualEntity entity, Vec3 origin, Vec3 blade) {
        List<Vec3> points = new ArrayList<>();
        points.add(blade);
        points.addAll(entity.history);
        for (int i = 0; i < points.size() - 1; i++) {
            float fadeA = 1.0F - (float) i / points.size();
            float fadeB = 1.0F - (float) (i + 1) / points.size();
            Vec3 a = points.get(i).subtract(origin);
            Vec3 b = points.get(i + 1).subtract(origin);
            quad(vertices, matrix, normal,
                    a.add(0, 0.18 * fadeA, 0), b.add(0, 0.18 * fadeB, 0),
                    b.subtract(0, 0.18 * fadeB, 0), a.subtract(0, 0.18 * fadeA, 0),
                    (int) (230 * fadeA), (int) (230 * fadeB), (int) (200 * fadeA), (int) (200 * fadeB), FULL_BRIGHT);
        }
    }

    private static void renderChain(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal,
                                    Vec3 from, Vec3 to, int light) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 0.1) {
            return;
        }
        Vec3 dir = span.scale(1.0 / length);
        Vec3 side = dir.cross(UP);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 side2 = dir.cross(side).normalize();

        int links = Mth.ceil(length / LINK_LENGTH);
        for (int k = 0; k < links; k++) {
            double start = k * LINK_LENGTH - 0.02;
            double end = Math.min(length, (k + 1) * LINK_LENGTH + 0.02);
            Vec3 a = from.add(dir.scale(start));
            Vec3 b = from.add(dir.scale(end));
            Vec3 across = (k & 1) == 0 ? side : side2;
            int shade = (k & 1) == 0 ? 165 : 120;
            for (int bar = -1; bar <= 1; bar += 2) {
                Vec3 offset = across.scale(0.045 * bar);
                Vec3 thickness = across.scale(0.016);
                quad(vertices, matrix, normal,
                        a.add(offset).add(thickness), b.add(offset).add(thickness),
                        b.add(offset).subtract(thickness), a.add(offset).subtract(thickness),
                        shade, shade, 255, 255, light);
            }
        }
    }

    private void renderBlade(HeavenChainVisualEntity entity, PoseStack poseStack,
                             MultiBufferSource buffers, Vec3 pos, Vec3 pointing, float spinTime, int light) {
        ItemStack stack = entity.getStack();
        if (stack.isEmpty()) {
            return;
        }
        Vec3 dir = pointing.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : pointing.normalize();
        float yawDeg = (float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG);
        float pitchDeg = (float) (-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.translate(0.0, 0.0, 0.32);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinTime * 35.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
    }

    private static Vec3 handAnchor(LivingEntity owner, HeavenChainVisualEntity chain, boolean armOut, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (owner == client.getCameraEntity() && client.options.getCameraType().isFirstPerson()) {
            Vec3 eye = owner.getEyePosition(partialTick);
            Vec3 look = owner.getViewVector(partialTick);
            Vec3 right = look.cross(UP);
            right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
            return eye.add(look.scale(0.5)).add(right.scale(0.3)).add(0, -0.35, 0);
        }
        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double side = owner.getMainArm() == HumanoidArm.LEFT ? -1.0 : 1.0;
        Vec3 right = new Vec3(-Mth.cos(bodyYaw), 0, -Mth.sin(bodyYaw)).scale(side);
        Vec3 pos = owner.getPosition(partialTick);
        if (!armOut) {
            return pos.add(right.scale(0.38)).add(0, owner.getBbHeight() * 0.42, 0);
        }
        return InvertedSpearPoses.spinHandPos(owner, chain, partialTick);
    }

    // both windings, culling was eating half of it
    static void quad(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
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

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Matrix3f normal, Vec3 pos,
                               int shade, int alpha, int light) {
        int a = Mth.clamp(alpha, 0, 255);
        entityVertex(vertices, matrix, normal, (float) pos.x, (float) pos.y, (float) pos.z,
                shade, shade, shade, a, 0.5F, 0.5F, light, 0.0F, 1.0F, 0.0F);
    }

    public static void entityVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z,
                                    int r, int g, int b, int a, float u, float v, int light, float nx, float ny, float nz) {
        Vector3f n = normal.transform(new Vector3f(nx, ny, nz));
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(n.x(), n.y(), n.z());
    }
}
