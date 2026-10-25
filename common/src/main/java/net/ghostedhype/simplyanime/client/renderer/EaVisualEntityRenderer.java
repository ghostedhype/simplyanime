package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ghostedhype.simplyanime.entity.EaVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class EaVisualEntityRenderer extends EntityRenderer<EaVisualEntity> {

    private static final int[] WHITE = {255, 255, 244};
    private static final int[] GOLD = {255, 206, 120};
    private static final int[] ORANGE = {255, 64, 26};
    private static final int[] RED = {255, 34, 18};
    private static final int[] CRIMSON = {128, 4, 12};
    private static final int[] ABYSS = {34, 0, 6};

    private static final double STEP = 1.5;
    private static final int AROUND = 18;

    public EaVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EaVisualEntity entity) {
        return SimplyAnimeRenderTypes.WHITE;
    }

    @Override
    public boolean shouldRender(EaVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(EaVisualEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        if (Later.take(entity, poseStack, (pose, late) -> this.render(entity, yaw, partialTick, pose, late, light))) {
            return;
        }
        // light doesn't cast shadows
        if (ShaderCompat.shadowPass()) {
            return;
        }
        Matrix4f matrix = poseStack.last().pose();
        Vec3 base = entity.getPosition(partialTick);
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(base);
        Quaternionf rotation = this.entityRenderDispatcher.cameraOrientation();
        Vec3 camRight = VfxBuffer.rotate(rotation, 1, 0, 0);
        Vec3 camUp = VfxBuffer.rotate(rotation, 0, 1, 0);
        float time = entity.tickCount + partialTick;
        float t = entity.getPhaseProgress(partialTick);

        switch (entity.getPhase()) {
            case EaVisualEntity.PHASE_CHARGE -> {
                LivingEntity owner = entity.getOwner();
                if (owner != null) {
                    renderCharge(entity, owner, base, matrix, buffers, camRight, camUp, partialTick, time, t);
                }
            }
            case EaVisualEntity.PHASE_RELEASE -> renderFlash(matrix, buffers, camRight, camUp, entity, t);
            case EaVisualEntity.PHASE_BEAM -> {
                float grow = EaVisualEntity.growth(entity.getPhaseTicks(partialTick));
                renderBeam(entity, matrix, buffers, camera, camRight, camUp, time, grow, 0.0F);
            }
            case EaVisualEntity.PHASE_FADE -> renderBeam(entity, matrix, buffers, camera, camRight, camUp, time, 1.0F, t);
            default -> {
            }
        }
    }

    private void renderCharge(EaVisualEntity entity, LivingEntity owner, Vec3 base, Matrix4f matrix,
                              MultiBufferSource buffers, Vec3 camRight, Vec3 camUp, float partialTick,
                              float time, float t) {
        Vec3 tip = EaVisualEntity.chargeTip(owner, partialTick).subtract(base);
        Vec3 feet = owner.getPosition(partialTick).subtract(base).add(0, 0.06, 0);
        Vec3 axis = tip.subtract(feet.add(0, owner.getBbHeight() * 0.8, 0)).normalize();
        Vec3 u = EaVisualEntity.perpendicular(axis);
        Vec3 v = axis.cross(u);
        float rise = Math.min(1.0F, entity.getPhaseTicks(partialTick) / 10.0F);

        VfxBuffer shade = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.shade());
        for (int i = 0; i < 3; i++) {
            Vec3 center = tip.subtract(axis.scale(0.35 + i * 0.5));
            float radius = (0.5F + i * 0.28F) * (0.6F + 0.7F * t) * rise;
            spinningBand(shade, matrix, center, axis, u, v, radius * 1.08F, 0.09F, CRIMSON, 0.55F * rise,
                    time * (0.35F + i * 0.1F), 0.4F);
        }

        VfxBuffer glow = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.glow());
        for (int i = 0; i < 3; i++) {
            Vec3 center = tip.subtract(axis.scale(0.35 + i * 0.5));
            float radius = (0.5F + i * 0.28F) * (0.6F + 0.7F * t) * rise;
            spinningBand(glow, matrix, center, axis, u, v, radius, 0.07F, RED, 0.9F * rise, -time * (0.4F + i * 0.12F), 0.3F);
        }

        // orb
        float orb = (0.25F + 1.5F * t * t) * rise;
        int[] heart = t > 0.6F ? WHITE : GOLD;
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 2.6F, RED, 0.55F);
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 1.4F, ORANGE, 0.8F);
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 0.7F, heart, 1.0F);

        // ground sigil
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 south = new Vec3(0, 0, 1);
        float sigil = (2.0F + 1.2F * t) * rise;
        flatRing(glow, matrix, feet, east, south, sigil, 0.14F, RED, 0.75F * rise, time * 0.08F, 10);
        flatRing(glow, matrix, feet, east, south, sigil * 0.62F, 0.1F, ORANGE, 0.6F * rise, -time * 0.12F, 6);
        disc(glow, matrix, feet.add(up.scale(0.01)), east, south, sigil * 1.1F, RED, 0.25F * t, 0.0F);
    }

    private void renderFlash(Matrix4f matrix, MultiBufferSource buffers, Vec3 camRight, Vec3 camUp,
                             EaVisualEntity entity, float t) {
        VfxBuffer glow = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.glow());
        float e = 1.0F - (1.0F - t) * (1.0F - t);
        float radius = Math.min(entity.getRadius(), 4.0F);
        Vec3 at = Vec3.ZERO;
        billboardGlow(glow, matrix, at, camRight, camUp, radius * (1.5F + 3.5F * e), GOLD, 0.9F);
        billboardGlow(glow, matrix, at, camRight, camUp, radius * (0.8F + 1.6F * e), WHITE, 1.0F);
        // star streaks off the flash
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8 + 0.2F;
            Vec3 dir = camRight.scale(Mth.cos(a)).add(camUp.scale(Mth.sin(a)));
            Vec3 side = camRight.scale(-Mth.sin(a)).add(camUp.scale(Mth.cos(a)));
            float len = radius * (2.0F + 4.0F * e) * (i % 2 == 0 ? 1.0F : 0.6F);
            spike(glow, matrix, at, dir, side, len, 0.18F * radius, WHITE, 0.9F);
        }
    }

    private void renderBeam(EaVisualEntity entity, Matrix4f matrix, MultiBufferSource buffers,
                            Vec3 camera, Vec3 camRight, Vec3 camUp, float time, float grow, float fade) {
        Vec3 dir = entity.getAim();
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        Vec3 u = EaVisualEntity.perpendicular(dir);
        Vec3 v = dir.cross(u);
        float fullRadius = entity.getRadius();
        float thin = EaVisualEntity.fadeThickness(fade);
        float alpha = 1.0F - fade;
        double length = entity.getLength() * grow;
        if (length < 0.2) {
            return;
        }
        int segments = Math.max(1, (int) Math.ceil(length / STEP));

        // shells fade when the camera is inside the beam
        Vec3 toCam = camera;
        double along = Mth.clamp(toCam.dot(dir), 0.0, length);
        double axisDistance = toCam.subtract(dir.scale(along)).length();
        float inside = (float) Mth.clamp(axisDistance / (fullRadius * 1.3), 0.3, 1.0);

        if (ShaderCompat.packInUse()) {
            VfxBuffer depth = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.depth(SimplyAnimeRenderTypes.WHITE));
            for (int i = 0; i < segments; i++) {
                double s0 = length * i / segments;
                double s1 = length * (i + 1) / segments;
                Vec3 p0 = dir.scale(s0);
                Vec3 p1 = dir.scale(s1);
                Vec3 w0 = facing(dir, p0, camera, u);
                Vec3 w1 = facing(dir, p1, camera, u);
                Vec3 b0 = p0.subtract(camera.subtract(p0).normalize().scale(0.3));
                Vec3 b1 = p1.subtract(camera.subtract(p1).normalize().scale(0.3));
                ribbon(depth, matrix, b0, b1, w0, w1, radius(s0, time, fullRadius) * thin * 0.4F,
                        radius(s1, time, fullRadius) * thin * 0.4F, WHITE, 1.0F, 1.0F);
            }
        }
        VfxBuffer shade = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.shade());
        for (int k = 0; k < 4; k++) {
            int[] color = k % 2 == 0 ? ABYSS : CRIMSON;
            float strandAlpha = (k % 2 == 0 ? 0.7F : 0.55F) * alpha * inside;
            swirl(shade, matrix, dir, u, v, camera, length, fullRadius, thin, time, k * Mth.HALF_PI, color, strandAlpha);
        }

        VfxBuffer glow = VfxBuffer.of(buffers, SimplyAnimeRenderTypes.glow());

        // ribbons for the bloom
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            Vec3 p0 = dir.scale(s0);
            Vec3 p1 = dir.scale(s1);
            Vec3 w0 = facing(dir, p0, camera, u);
            Vec3 w1 = facing(dir, p1, camera, u);
            float edgeOn0 = sideOn(dir, p0, camera);
            float edgeOn1 = sideOn(dir, p1, camera);
            float r0 = radius(s0, time, fullRadius) * thin;
            float r1 = radius(s1, time, fullRadius) * thin;
            float tipFade0 = tipFade(s0, length);
            float tipFade1 = tipFade(s1, length);
            ribbon(glow, matrix, p0, p1, w0, w1, r0 * 2.4F, r1 * 2.4F, RED, 0.6F * alpha * edgeOn0 * tipFade0, 0.6F * alpha * edgeOn1 * tipFade1);
            ribbon(glow, matrix, p0, p1, w0, w1, r0 * 1.3F, r1 * 1.3F, RED, 0.8F * alpha * edgeOn0 * tipFade0, 0.8F * alpha * edgeOn1 * tipFade1);
            ribbon(glow, matrix, p0, p1, w0, w1, r0 * 0.7F, r1 * 0.7F, ORANGE, 0.7F * alpha * edgeOn0, 0.7F * alpha * edgeOn1);
            ribbon(glow, matrix, p0, p1, w0, w1, r0 * 0.4F, r1 * 0.4F, GOLD, 0.8F * alpha * edgeOn0, 0.8F * alpha * edgeOn1);
            ribbon(glow, matrix, p0, p1, w0, w1, r0 * 0.18F, r1 * 0.18F, WHITE, alpha * edgeOn0, alpha * edgeOn1);
        }

        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.95F, RED, 0.22F * alpha * inside);
        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.55F, RED, 0.2F * alpha * inside);
        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.22F, GOLD, 0.3F * alpha);

        // muzzle flare + rupture rings
        Vec3 muzzle = dir.scale(0.2);
        float pulse = 1.0F + 0.08F * Mth.sin(time * 1.7F);
        float muzzleSize = EaVisualEntity.muzzleRadius(fullRadius);
        disc(glow, matrix, muzzle, u, v, muzzleSize * pulse * thin, GOLD, 0.9F * alpha, 0.0F);
        billboardGlow(glow, matrix, muzzle, camRight, camUp, muzzleSize * 0.5F * pulse * thin, WHITE, 0.85F * alpha);
        for (int i = 0; i < 3; i++) {
            double at = fullRadius * (0.5 + i * 0.8);
            if (at > length) {
                break;
            }
            float r = radius(at, time, fullRadius) * 1.45F * thin * pulse;
            spinningBand(glow, matrix, dir.scale(at), dir, u, v, r, 0.05F + r * 0.02F, RED, 0.9F * alpha,
                    time * (i % 2 == 0 ? 0.3F : -0.22F), 0.35F);
        }

        // shock rings
        for (int k = 0; k < 12; k++) {
            float age = (time % 5.0F) + k * 5.0F;
            double s = age * 4.5;
            if (s > length) {
                continue;
            }
            float r = radius(s, time, fullRadius) * 1.3F * thin;
            float ringAlpha = 0.85F * alpha * (1.0F - (float) (s / Math.max(length, 1.0)));
            band(glow, matrix, dir.scale(s), dir, u, v, r, 0.12F + r * 0.03F, RED, ringAlpha);
        }

        // beam front
        Vec3 end = dir.scale(length);
        if (grow < 1.0F) {
            billboardGlow(glow, matrix, end, camRight, camUp, fullRadius * 1.5F, WHITE, 0.9F);
        }

        // ground shockwave
        float wave = entity.getPhase() == EaVisualEntity.PHASE_BEAM
                ? Math.min(1.0F, entity.getPhaseTicks(0.0F) / 14.0F) : 1.0F;
        if (wave < 1.0F) {
            Vec3 ground = dir.scale(-1.6).add(0, -1.3, 0);
            Vec3 east = new Vec3(1, 0, 0);
            Vec3 south = new Vec3(0, 0, 1);
            float spread = fullRadius * 3.0F * (1.0F - (1.0F - wave) * (1.0F - wave));
            flatRing(glow, matrix, ground, east, south, spread, 0.35F + spread * 0.03F, ORANGE, 0.9F * (1.0F - wave),
                    time * 0.02F, 16);
            disc(glow, matrix, ground.add(0, 0.02, 0), east, south, spread, RED, 0.35F * (1.0F - wave), 0.0F);
        }

        if (entity.isHitTerrain() && grow >= 1.0F) {
            float blastAlpha = alpha * (0.85F + 0.15F * Mth.sin(time * 2.1F));
            // toned down up close or the whole screen goes white
            float near = (float) Mth.clamp(end.distanceTo(camera) / (fullRadius * 4.0), 0.35, 1.0);
            float size = EaVisualEntity.impactSize(fullRadius, fade);
            billboardGlow(glow, matrix, end, camRight, camUp, size * 2.6F, RED, 0.75F * blastAlpha);
            billboardGlow(glow, matrix, end, camRight, camUp, size * 1.4F, ORANGE, 0.8F * blastAlpha * near);
            billboardGlow(glow, matrix, end, camRight, camUp, size * 0.6F, WHITE, 0.9F * blastAlpha * near);
        }

        // threads
        for (int k = 0; k < 3; k++) {
            thread(glow, matrix, dir, u, v, length, fullRadius, thin, time, k * Mth.TWO_PI / 3 + 0.5F, RED, 0.8F * alpha);
        }
    }

    private static float radius(double s, float time, float full) {
        float ripple = 1.0F + 0.07F * Mth.sin((float) s * 0.35F - time * 1.6F);
        return EaVisualEntity.profile(s, full) * ripple;
    }

    private static float tipFade(double s, double length) {
        return (float) Mth.clamp((length - s) / 2.5 + 0.35, 0.35, 1.0);
    }

    // sideways vector that keeps a ribbon facing the camera
    private static Vec3 facing(Vec3 dir, Vec3 point, Vec3 camera, Vec3 fallback) {
        Vec3 w = dir.cross(point.subtract(camera));
        return w.lengthSqr() < 1.0E-6 ? fallback : w.normalize();
    }

    private static float sideOn(Vec3 dir, Vec3 point, Vec3 camera) {
        Vec3 view = point.subtract(camera);
        double len = view.length();
        if (len < 1.0E-4) {
            return 0.0F;
        }
        return (float) Mth.clamp(dir.cross(view.scale(1.0 / len)).length() * 1.4, 0.0, 1.0);
    }

    private static void ribbon(VfxBuffer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 w0, Vec3 w1,
                               float half0, float half1, int[] color, float a0, float a1) {
        if (a0 <= 0.01F && a1 <= 0.01F) {
            return;
        }
        Vec3 l0 = p0.add(w0.scale(-half0));
        Vec3 r0 = p0.add(w0.scale(half0));
        Vec3 l1 = p1.add(w1.scale(-half1));
        Vec3 r1 = p1.add(w1.scale(half1));
        vertex(vc, m, l0, color, 0.0F);
        vertex(vc, m, p0, color, a0);
        vertex(vc, m, p1, color, a1);
        vertex(vc, m, l1, color, 0.0F);

        vertex(vc, m, p0, color, a0);
        vertex(vc, m, r0, color, 0.0F);
        vertex(vc, m, r1, color, 0.0F);
        vertex(vc, m, p1, color, a1);
    }

    private static void tube(VfxBuffer vc, Matrix4f m, Vec3 dir, Vec3 u, Vec3 v, double length, float full,
                             float thin, float time, float scale, int[] color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        int segments = Math.max(1, (int) Math.ceil(length / STEP));
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            float r0 = radius(s0, time, full) * scale * thin;
            float r1 = radius(s1, time, full) * scale * thin;
            for (int j = 0; j < AROUND; j++) {
                float a0 = j * Mth.TWO_PI / AROUND;
                float a1 = (j + 1) * Mth.TWO_PI / AROUND;
                Vec3 o0 = u.scale(Mth.cos(a0)).add(v.scale(Mth.sin(a0)));
                Vec3 o1 = u.scale(Mth.cos(a1)).add(v.scale(Mth.sin(a1)));
                vertex(vc, m, dir.scale(s0).add(o0.scale(r0)), color, alpha);
                vertex(vc, m, dir.scale(s0).add(o1.scale(r0)), color, alpha);
                vertex(vc, m, dir.scale(s1).add(o1.scale(r1)), color, alpha);
                vertex(vc, m, dir.scale(s1).add(o0.scale(r1)), color, alpha);
            }
        }
    }

    private static void swirl(VfxBuffer vc, Matrix4f m, Vec3 dir, Vec3 u, Vec3 v, Vec3 camera, double length,
                              float full, float thin, float time, float offset, int[] color, float alpha) {
        int segments = Math.max(1, (int) Math.ceil(length / 0.75));
        float width = 0.34F;
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            float a0 = (float) s0 * 0.42F + offset + time * 0.5F;
            float a1 = (float) s1 * 0.42F + offset + time * 0.5F;
            float r0 = radius(s0, time, full) * 1.04F * thin;
            float r1 = radius(s1, time, full) * 1.04F * thin;
            Vec3 o0 = u.scale(Mth.cos(a0)).add(v.scale(Mth.sin(a0)));
            Vec3 o1 = u.scale(Mth.cos(a1)).add(v.scale(Mth.sin(a1)));
            Vec3 mid = dir.scale(s0).add(o0.scale(r0));
            if (o0.dot(camera.subtract(mid)) <= 0) {
                continue;
            }
            Vec3 q0 = u.scale(Mth.cos(a0 + width)).add(v.scale(Mth.sin(a0 + width)));
            Vec3 q1 = u.scale(Mth.cos(a1 + width)).add(v.scale(Mth.sin(a1 + width)));
            float fade = tipFade(s0, length);
            vertex(vc, m, dir.scale(s0).add(o0.scale(r0)), color, alpha * fade);
            vertex(vc, m, dir.scale(s0).add(q0.scale(r0)), color, alpha * fade);
            vertex(vc, m, dir.scale(s1).add(q1.scale(r1)), color, alpha * fade);
            vertex(vc, m, dir.scale(s1).add(o1.scale(r1)), color, alpha * fade);
        }
    }

    private static void thread(VfxBuffer vc, Matrix4f m, Vec3 dir, Vec3 u, Vec3 v, double length, float full,
                               float thin, float time, float offset, int[] color, float alpha) {
        int segments = Math.max(1, (int) Math.ceil(length / 0.75));
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            float a0 = (float) s0 * -0.3F + offset - time * 0.35F;
            float a1 = (float) s1 * -0.3F + offset - time * 0.35F;
            float r0 = radius(s0, time, full) * 1.12F * thin;
            float r1 = radius(s1, time, full) * 1.12F * thin;
            Vec3 o0 = u.scale(Mth.cos(a0)).add(v.scale(Mth.sin(a0)));
            Vec3 o1 = u.scale(Mth.cos(a1)).add(v.scale(Mth.sin(a1)));
            Vec3 p0 = dir.scale(s0).add(o0.scale(r0));
            Vec3 p1 = dir.scale(s1).add(o1.scale(r1));
            Vec3 w = o0.scale(0.08 + r0 * 0.02);
            float fade = tipFade(s0, length);
            vertex(vc, m, p0.subtract(w), color, alpha * fade);
            vertex(vc, m, p0.add(w), color, alpha * fade);
            vertex(vc, m, p1.add(w), color, alpha * fade);
            vertex(vc, m, p1.subtract(w), color, alpha * fade);
        }
    }

    // short open cylinder round an axis
    private static void band(VfxBuffer vc, Matrix4f m, Vec3 center, Vec3 axis, Vec3 u, Vec3 v, float radius,
                             float halfWidth, int[] color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vec3 back = axis.scale(-halfWidth);
        Vec3 front = axis.scale(halfWidth);
        for (int j = 0; j < AROUND * 2; j++) {
            float a0 = j * Mth.TWO_PI / (AROUND * 2);
            float a1 = (j + 1) * Mth.TWO_PI / (AROUND * 2);
            Vec3 o0 = u.scale(Mth.cos(a0) * radius).add(v.scale(Mth.sin(a0) * radius));
            Vec3 o1 = u.scale(Mth.cos(a1) * radius).add(v.scale(Mth.sin(a1) * radius));
            vertex(vc, m, center.add(o0).add(back), color, alpha);
            vertex(vc, m, center.add(o1).add(back), color, alpha);
            vertex(vc, m, center.add(o1).add(front), color, alpha);
            vertex(vc, m, center.add(o0).add(front), color, alpha);
        }
    }

    private static void spinningBand(VfxBuffer vc, Matrix4f m, Vec3 center, Vec3 axis, Vec3 u, Vec3 v,
                                     float radius, float halfWidth, int[] color, float alpha, float spin, float floor) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        Vec3 back = axis.scale(-halfWidth);
        Vec3 front = axis.scale(halfWidth);
        int steps = AROUND * 2;
        for (int j = 0; j < steps; j++) {
            float a0 = j * Mth.TWO_PI / steps;
            float a1 = (j + 1) * Mth.TWO_PI / steps;
            float k0 = floor + (1.0F - floor) * Mth.frac(j / (float) steps + spin);
            float k1 = floor + (1.0F - floor) * Mth.frac((j + 1) / (float) steps + spin);
            if (k1 < k0) {
                k1 = 1.0F;
            }
            Vec3 o0 = u.scale(Mth.cos(a0) * radius).add(v.scale(Mth.sin(a0) * radius));
            Vec3 o1 = u.scale(Mth.cos(a1) * radius).add(v.scale(Mth.sin(a1) * radius));
            vertex(vc, m, center.add(o0).add(back), color, alpha * k0);
            vertex(vc, m, center.add(o1).add(back), color, alpha * k1);
            vertex(vc, m, center.add(o1).add(front), color, alpha * k1);
            vertex(vc, m, center.add(o0).add(front), color, alpha * k0);
        }
    }

    // flat dashed ring lying in the u/v plane
    private static void flatRing(VfxBuffer vc, Matrix4f m, Vec3 center, Vec3 u, Vec3 v, float radius,
                                 float width, int[] color, float alpha, float spin, int dashes) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        int steps = dashes * 6;
        for (int j = 0; j < steps; j++) {
            if (j % 6 == 5) {
                continue;
            }
            float a0 = j * Mth.TWO_PI / steps + spin;
            float a1 = (j + 1) * Mth.TWO_PI / steps + spin;
            Vec3 d0 = u.scale(Mth.cos(a0)).add(v.scale(Mth.sin(a0)));
            Vec3 d1 = u.scale(Mth.cos(a1)).add(v.scale(Mth.sin(a1)));
            vertex(vc, m, center.add(d0.scale(radius - width)), color, alpha);
            vertex(vc, m, center.add(d1.scale(radius - width)), color, alpha);
            vertex(vc, m, center.add(d1.scale(radius + width)), color, alpha);
            vertex(vc, m, center.add(d0.scale(radius + width)), color, alpha);
        }
    }

    // filled circle
    private static void disc(VfxBuffer vc, Matrix4f m, Vec3 center, Vec3 u, Vec3 v, float radius,
                             int[] color, float alpha, float edgeAlpha) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        int steps = 24;
        for (int j = 0; j < steps; j++) {
            float a0 = j * Mth.TWO_PI / steps;
            float a1 = (j + 1) * Mth.TWO_PI / steps;
            Vec3 p0 = center.add(u.scale(Mth.cos(a0) * radius)).add(v.scale(Mth.sin(a0) * radius));
            Vec3 p1 = center.add(u.scale(Mth.cos(a1) * radius)).add(v.scale(Mth.sin(a1) * radius));
            vertex(vc, m, center, color, alpha);
            vertex(vc, m, center, color, alpha);
            vertex(vc, m, p1, color, edgeAlpha);
            vertex(vc, m, p0, color, edgeAlpha);
        }
    }

    private static void billboardGlow(VfxBuffer vc, Matrix4f m, Vec3 center, Vec3 camRight, Vec3 camUp,
                                      float radius, int[] color, float alpha) {
        disc(vc, m, center, camRight, camUp, radius, color, alpha, 0.0F);
    }

    private static void spike(VfxBuffer vc, Matrix4f m, Vec3 from, Vec3 dir, Vec3 side, float length,
                              float width, int[] color, float alpha) {
        Vec3 tip = from.add(dir.scale(length));
        vertex(vc, m, from.add(side.scale(width)), color, alpha);
        vertex(vc, m, from.subtract(side.scale(width)), color, alpha);
        vertex(vc, m, tip, color, 0.0F);
        vertex(vc, m, tip, color, 0.0F);
    }

    private static void vertex(VfxBuffer vc, Matrix4f m, Vec3 p, int[] color, float alpha) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z, color[0], color[1], color[2],
                (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255), 0.5F, 0.5F);
    }
}
