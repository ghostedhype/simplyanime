package net.hussain.simplyanime.client.renderer;

import net.hussain.simplyanime.client.EnumaElishPoses;
import net.hussain.simplyanime.entity.EnumaElishVisualEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EnumaElishVisualEntityRenderer extends EntityRenderer<EnumaElishVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");

    private static final int[] WHITE = {255, 255, 244};
    private static final int[] GOLD = {255, 206, 120};
    private static final int[] ORANGE = {255, 64, 26};
    private static final int[] RED = {255, 34, 18};
    private static final int[] CRIMSON = {128, 4, 12};
    private static final int[] ABYSS = {34, 0, 6};

    private static final double STEP = 1.5;
    private static final int AROUND = 18;
    private static final float GROW_TICKS = 3.0F;

    public EnumaElishVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(EnumaElishVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(EnumaElishVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(EnumaElishVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d base = entity.getLerpedPos(tickDelta);
        Vec3d camera = this.dispatcher.camera.getPos().subtract(base);
        Quaternionf rotation = this.dispatcher.getRotation();
        Vec3d camRight = rotate(rotation, 1, 0, 0);
        Vec3d camUp = rotate(rotation, 0, 1, 0);
        float time = entity.age + tickDelta;
        float t = entity.getPhaseProgress(tickDelta);

        switch (entity.getPhase()) {
            case EnumaElishVisualEntity.PHASE_CHARGE -> {
                LivingEntity owner = entity.getOwner();
                if (owner != null) {
                    renderCharge(entity, owner, base, matrix, consumers, camRight, camUp, tickDelta, time, t);
                }
            }
            case EnumaElishVisualEntity.PHASE_RELEASE -> renderFlash(matrix, consumers, camRight, camUp, entity, t);
            case EnumaElishVisualEntity.PHASE_BEAM -> {
                float grow = Math.min(1.0F, entity.getPhaseTicks(tickDelta) / GROW_TICKS);
                renderBeam(entity, matrix, consumers, camera, camRight, camUp, time, 1.0F - (1.0F - grow) * (1.0F - grow), 0.0F);
            }
            case EnumaElishVisualEntity.PHASE_FADE -> renderBeam(entity, matrix, consumers, camera, camRight, camUp, time, 1.0F, t);
            default -> {
            }
        }
    }

    // the build up: red wind wrapping the raised blade, an orb swelling at the tip, a sigil under the feet
    private void renderCharge(EnumaElishVisualEntity entity, LivingEntity owner, Vec3d base, Matrix4f matrix,
                              VertexConsumerProvider consumers, Vec3d camRight, Vec3d camUp, float tickDelta,
                              float time, float t) {
        Vec3d tip = EnumaElishPoses.chargeTip(owner, tickDelta).subtract(base);
        Vec3d feet = owner.getLerpedPos(tickDelta).subtract(base).add(0, 0.06, 0);
        Vec3d axis = tip.subtract(feet.add(0, owner.getHeight() * 0.8, 0)).normalize();
        Vec3d u = EnumaElishVisualEntity.perpendicular(axis);
        Vec3d v = axis.crossProduct(u);
        float rise = Math.min(1.0F, entity.getPhaseTicks(tickDelta) / 10.0F);

        VertexConsumer shade = consumers.getBuffer(AddonRenderLayers.SHADE);
        for (int i = 0; i < 3; i++) {
            Vec3d center = tip.subtract(axis.multiply(0.35 + i * 0.5));
            float radius = (0.5F + i * 0.28F) * (0.6F + 0.7F * t) * rise;
            spinningBand(shade, matrix, center, axis, u, v, radius * 1.08F, 0.09F, CRIMSON, 0.55F * rise,
                    time * (0.35F + i * 0.1F), 0.4F);
        }

        VertexConsumer glow = consumers.getBuffer(AddonRenderLayers.GLOW);
        for (int i = 0; i < 3; i++) {
            Vec3d center = tip.subtract(axis.multiply(0.35 + i * 0.5));
            float radius = (0.5F + i * 0.28F) * (0.6F + 0.7F * t) * rise;
            spinningBand(glow, matrix, center, axis, u, v, radius, 0.07F, RED, 0.9F * rise, -time * (0.4F + i * 0.12F), 0.3F);
        }

        // orb at the tip, reddish at first and white hot by the end
        float orb = (0.25F + 1.5F * t * t) * rise;
        int[] heart = t > 0.6F ? WHITE : GOLD;
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 2.6F, RED, 0.55F);
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 1.4F, ORANGE, 0.8F);
        billboardGlow(glow, matrix, tip, camRight, camUp, orb * 0.7F, heart, 1.0F);

        // ground sigil
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d east = new Vec3d(1, 0, 0);
        Vec3d south = new Vec3d(0, 0, 1);
        float sigil = (2.0F + 1.2F * t) * rise;
        flatRing(glow, matrix, feet, east, south, sigil, 0.14F, RED, 0.75F * rise, time * 0.08F, 10);
        flatRing(glow, matrix, feet, east, south, sigil * 0.62F, 0.1F, ORANGE, 0.6F * rise, -time * 0.12F, 6);
        disc(glow, matrix, feet.add(up.multiply(0.01)), east, south, sigil * 1.1F, RED, 0.25F * t, 0.0F);
    }

    private void renderFlash(Matrix4f matrix, VertexConsumerProvider consumers, Vec3d camRight, Vec3d camUp,
                             EnumaElishVisualEntity entity, float t) {
        VertexConsumer glow = consumers.getBuffer(AddonRenderLayers.GLOW);
        float e = 1.0F - (1.0F - t) * (1.0F - t);
        float radius = Math.min(entity.getRadius(), 4.0F);
        Vec3d at = Vec3d.ZERO;
        billboardGlow(glow, matrix, at, camRight, camUp, radius * (1.5F + 3.5F * e), GOLD, 0.9F);
        billboardGlow(glow, matrix, at, camRight, camUp, radius * (0.8F + 1.6F * e), WHITE, 1.0F);
        // star streaks off the flash
        for (int i = 0; i < 8; i++) {
            float a = i * MathHelper.TAU / 8 + 0.2F;
            Vec3d dir = camRight.multiply(MathHelper.cos(a)).add(camUp.multiply(MathHelper.sin(a)));
            Vec3d side = camRight.multiply(-MathHelper.sin(a)).add(camUp.multiply(MathHelper.cos(a)));
            float len = radius * (2.0F + 4.0F * e) * (i % 2 == 0 ? 1.0F : 0.6F);
            spike(glow, matrix, at, dir, side, len, 0.18F * radius, WHITE, 0.9F);
        }
    }

    private void renderBeam(EnumaElishVisualEntity entity, Matrix4f matrix, VertexConsumerProvider consumers,
                            Vec3d camera, Vec3d camRight, Vec3d camUp, float time, float grow, float fade) {
        Vec3d dir = entity.getDirection();
        if (dir.lengthSquared() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        Vec3d u = EnumaElishVisualEntity.perpendicular(dir);
        Vec3d v = dir.crossProduct(u);
        float fullRadius = entity.getRadius();
        float thin = (1.0F - fade) * (1.0F - fade) * (1.0F - fade * 0.5F);
        float alpha = 1.0F - fade;
        double length = entity.getLength() * grow;
        if (length < 0.2) {
            return;
        }
        int segments = Math.max(1, (int) Math.ceil(length / STEP));

        // how close the camera sits to the beam's axis, shells get quieter from inside
        Vec3d toCam = camera;
        double along = MathHelper.clamp(toCam.dotProduct(dir), 0.0, length);
        double axisDistance = toCam.subtract(dir.multiply(along)).length();
        float inside = (float) MathHelper.clamp(axisDistance / (fullRadius * 1.3), 0.3, 1.0);

        VertexConsumer glow = consumers.getBuffer(AddonRenderLayers.GLOW);

        // camera facing ribbons carry the soft bloom
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            Vec3d p0 = dir.multiply(s0);
            Vec3d p1 = dir.multiply(s1);
            Vec3d w0 = facing(dir, p0, camera, u);
            Vec3d w1 = facing(dir, p1, camera, u);
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

        // real cylinders so it still has a body when you look down its length
        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.95F, RED, 0.22F * alpha * inside);
        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.55F, RED, 0.2F * alpha * inside);
        tube(glow, matrix, dir, u, v, length, fullRadius, thin, time, 0.22F, GOLD, 0.3F * alpha);

        // muzzle flare and the rupture rings hanging just in front of it
        Vec3d muzzle = dir.multiply(0.2);
        float pulse = 1.0F + 0.08F * MathHelper.sin(time * 1.7F);
        float muzzleSize = Math.min(fullRadius, 4.0F);
        disc(glow, matrix, muzzle, u, v, muzzleSize * 1.4F * pulse * thin, GOLD, 0.9F * alpha, 0.0F);
        billboardGlow(glow, matrix, muzzle, camRight, camUp, muzzleSize * 0.7F * pulse * thin, WHITE, 0.85F * alpha);
        for (int i = 0; i < 3; i++) {
            double at = fullRadius * (0.5 + i * 0.8);
            if (at > length) {
                break;
            }
            float r = radius(at, time, fullRadius) * 1.45F * thin * pulse;
            spinningBand(glow, matrix, dir.multiply(at), dir, u, v, r, 0.05F + r * 0.02F, RED, 0.9F * alpha,
                    time * (i % 2 == 0 ? 0.3F : -0.22F), 0.35F);
        }

        // shock rings running up the beam
        for (int k = 0; k < 12; k++) {
            float age = (time % 5.0F) + k * 5.0F;
            double s = age * 4.5;
            if (s > length) {
                continue;
            }
            float r = radius(s, time, fullRadius) * 1.3F * thin;
            float ringAlpha = 0.85F * alpha * (1.0F - (float) (s / Math.max(length, 1.0)));
            band(glow, matrix, dir.multiply(s), dir, u, v, r, 0.12F + r * 0.03F, RED, ringAlpha);
        }

        // front of the beam while it is still travelling out
        Vec3d end = dir.multiply(length);
        if (grow < 1.0F) {
            billboardGlow(glow, matrix, end, camRight, camUp, fullRadius * 1.5F, WHITE, 0.9F);
        }

        // shockwave rolling out across the ground from the caster as it fires
        float wave = entity.getPhase() == EnumaElishVisualEntity.PHASE_BEAM
                ? Math.min(1.0F, entity.getPhaseTicks(0.0F) / 14.0F) : 1.0F;
        if (wave < 1.0F) {
            Vec3d ground = dir.multiply(-1.6).add(0, -1.3, 0);
            Vec3d east = new Vec3d(1, 0, 0);
            Vec3d south = new Vec3d(0, 0, 1);
            float spread = fullRadius * 3.0F * (1.0F - (1.0F - wave) * (1.0F - wave));
            flatRing(glow, matrix, ground, east, south, spread, 0.35F + spread * 0.03F, ORANGE, 0.9F * (1.0F - wave),
                    time * 0.02F, 16);
            disc(glow, matrix, ground.add(0, 0.02, 0), east, south, spread, RED, 0.35F * (1.0F - wave), 0.0F);
        }

        if (entity.isHitTerrain() && grow >= 1.0F) {
            float swell = 1.0F + fade * 0.9F;
            float blastAlpha = alpha * (0.85F + 0.15F * MathHelper.sin(time * 2.1F));
            // quieter when the camera is right on top of it, otherwise the whole screen goes white
            float near = (float) MathHelper.clamp(end.distanceTo(camera) / (fullRadius * 4.0), 0.35, 1.0);
            float size = fullRadius * 1.2F * swell;
            billboardGlow(glow, matrix, end, camRight, camUp, size * 2.6F, RED, 0.75F * blastAlpha);
            billboardGlow(glow, matrix, end, camRight, camUp, size * 1.4F, ORANGE, 0.8F * blastAlpha * near);
            billboardGlow(glow, matrix, end, camRight, camUp, size * 0.6F, WHITE, 0.9F * blastAlpha * near);
        }

        // dark vortex wrapped round the outside, front half only so it doesn't cross hatch the core
        VertexConsumer shade = consumers.getBuffer(AddonRenderLayers.SHADE);
        for (int k = 0; k < 4; k++) {
            int[] color = k % 2 == 0 ? ABYSS : CRIMSON;
            float strandAlpha = (k % 2 == 0 ? 0.7F : 0.55F) * alpha * inside;
            swirl(shade, matrix, dir, u, v, camera, length, fullRadius, thin, time, k * MathHelper.HALF_PI, color, strandAlpha);
        }

        // thin bright threads on top of the vortex
        glow = consumers.getBuffer(AddonRenderLayers.GLOW);
        for (int k = 0; k < 3; k++) {
            thread(glow, matrix, dir, u, v, length, fullRadius, thin, time, k * MathHelper.TAU / 3 + 0.5F, RED, 0.8F * alpha);
        }
    }

    private static float radius(double s, float time, float full) {
        float ripple = 1.0F + 0.07F * MathHelper.sin((float) s * 0.35F - time * 1.6F);
        return EnumaElishVisualEntity.profile(s, full) * ripple;
    }

    private static float tipFade(double s, double length) {
        return (float) MathHelper.clamp((length - s) / 2.5 + 0.35, 0.35, 1.0);
    }

    // sideways vector that keeps a ribbon facing the camera
    private static Vec3d facing(Vec3d dir, Vec3d point, Vec3d camera, Vec3d fallback) {
        Vec3d w = dir.crossProduct(point.subtract(camera));
        return w.lengthSquared() < 1.0E-6 ? fallback : w.normalize();
    }

    // 1 when the beam is seen side on, 0 when looking straight down it
    private static float sideOn(Vec3d dir, Vec3d point, Vec3d camera) {
        Vec3d view = point.subtract(camera);
        double len = view.length();
        if (len < 1.0E-4) {
            return 0.0F;
        }
        return (float) MathHelper.clamp(dir.crossProduct(view.multiply(1.0 / len)).length() * 1.4, 0.0, 1.0);
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3d p0, Vec3d p1, Vec3d w0, Vec3d w1,
                               float half0, float half1, int[] color, float a0, float a1) {
        if (a0 <= 0.01F && a1 <= 0.01F) {
            return;
        }
        // centre bright, edges fade to nothing
        Vec3d l0 = p0.add(w0.multiply(-half0));
        Vec3d r0 = p0.add(w0.multiply(half0));
        Vec3d l1 = p1.add(w1.multiply(-half1));
        Vec3d r1 = p1.add(w1.multiply(half1));
        vertex(vc, m, l0, color, 0.0F);
        vertex(vc, m, p0, color, a0);
        vertex(vc, m, p1, color, a1);
        vertex(vc, m, l1, color, 0.0F);

        vertex(vc, m, p0, color, a0);
        vertex(vc, m, r0, color, 0.0F);
        vertex(vc, m, r1, color, 0.0F);
        vertex(vc, m, p1, color, a1);
    }

    private static void tube(VertexConsumer vc, Matrix4f m, Vec3d dir, Vec3d u, Vec3d v, double length, float full,
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
                float a0 = j * MathHelper.TAU / AROUND;
                float a1 = (j + 1) * MathHelper.TAU / AROUND;
                Vec3d o0 = u.multiply(MathHelper.cos(a0)).add(v.multiply(MathHelper.sin(a0)));
                Vec3d o1 = u.multiply(MathHelper.cos(a1)).add(v.multiply(MathHelper.sin(a1)));
                vertex(vc, m, dir.multiply(s0).add(o0.multiply(r0)), color, alpha);
                vertex(vc, m, dir.multiply(s0).add(o1.multiply(r0)), color, alpha);
                vertex(vc, m, dir.multiply(s1).add(o1.multiply(r1)), color, alpha);
                vertex(vc, m, dir.multiply(s1).add(o0.multiply(r1)), color, alpha);
            }
        }
    }

    private static void swirl(VertexConsumer vc, Matrix4f m, Vec3d dir, Vec3d u, Vec3d v, Vec3d camera, double length,
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
            Vec3d o0 = u.multiply(MathHelper.cos(a0)).add(v.multiply(MathHelper.sin(a0)));
            Vec3d o1 = u.multiply(MathHelper.cos(a1)).add(v.multiply(MathHelper.sin(a1)));
            Vec3d mid = dir.multiply(s0).add(o0.multiply(r0));
            if (o0.dotProduct(camera.subtract(mid)) <= 0) {
                continue;
            }
            Vec3d q0 = u.multiply(MathHelper.cos(a0 + width)).add(v.multiply(MathHelper.sin(a0 + width)));
            Vec3d q1 = u.multiply(MathHelper.cos(a1 + width)).add(v.multiply(MathHelper.sin(a1 + width)));
            float fade = tipFade(s0, length);
            vertex(vc, m, dir.multiply(s0).add(o0.multiply(r0)), color, alpha * fade);
            vertex(vc, m, dir.multiply(s0).add(q0.multiply(r0)), color, alpha * fade);
            vertex(vc, m, dir.multiply(s1).add(q1.multiply(r1)), color, alpha * fade);
            vertex(vc, m, dir.multiply(s1).add(o1.multiply(r1)), color, alpha * fade);
        }
    }

    private static void thread(VertexConsumer vc, Matrix4f m, Vec3d dir, Vec3d u, Vec3d v, double length, float full,
                               float thin, float time, float offset, int[] color, float alpha) {
        int segments = Math.max(1, (int) Math.ceil(length / 0.75));
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments;
            double s1 = length * (i + 1) / segments;
            float a0 = (float) s0 * -0.3F + offset - time * 0.35F;
            float a1 = (float) s1 * -0.3F + offset - time * 0.35F;
            float r0 = radius(s0, time, full) * 1.12F * thin;
            float r1 = radius(s1, time, full) * 1.12F * thin;
            Vec3d o0 = u.multiply(MathHelper.cos(a0)).add(v.multiply(MathHelper.sin(a0)));
            Vec3d o1 = u.multiply(MathHelper.cos(a1)).add(v.multiply(MathHelper.sin(a1)));
            Vec3d p0 = dir.multiply(s0).add(o0.multiply(r0));
            Vec3d p1 = dir.multiply(s1).add(o1.multiply(r1));
            Vec3d w = o0.multiply(0.08 + r0 * 0.02);
            float fade = tipFade(s0, length);
            vertex(vc, m, p0.subtract(w), color, alpha * fade);
            vertex(vc, m, p0.add(w), color, alpha * fade);
            vertex(vc, m, p1.add(w), color, alpha * fade);
            vertex(vc, m, p1.subtract(w), color, alpha * fade);
        }
    }

    // short open cylinder round an axis
    private static void band(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d axis, Vec3d u, Vec3d v, float radius,
                             float halfWidth, int[] color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vec3d back = axis.multiply(-halfWidth);
        Vec3d front = axis.multiply(halfWidth);
        for (int j = 0; j < AROUND * 2; j++) {
            float a0 = j * MathHelper.TAU / (AROUND * 2);
            float a1 = (j + 1) * MathHelper.TAU / (AROUND * 2);
            Vec3d o0 = u.multiply(MathHelper.cos(a0) * radius).add(v.multiply(MathHelper.sin(a0) * radius));
            Vec3d o1 = u.multiply(MathHelper.cos(a1) * radius).add(v.multiply(MathHelper.sin(a1) * radius));
            vertex(vc, m, center.add(o0).add(back), color, alpha);
            vertex(vc, m, center.add(o1).add(back), color, alpha);
            vertex(vc, m, center.add(o1).add(front), color, alpha);
            vertex(vc, m, center.add(o0).add(front), color, alpha);
        }
    }

    // like band, but the brightness sweeps round so you can see it spin
    private static void spinningBand(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d axis, Vec3d u, Vec3d v,
                                     float radius, float halfWidth, int[] color, float alpha, float spin, float floor) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        Vec3d back = axis.multiply(-halfWidth);
        Vec3d front = axis.multiply(halfWidth);
        int steps = AROUND * 2;
        for (int j = 0; j < steps; j++) {
            float a0 = j * MathHelper.TAU / steps;
            float a1 = (j + 1) * MathHelper.TAU / steps;
            float k0 = floor + (1.0F - floor) * MathHelper.fractionalPart(j / (float) steps + spin);
            float k1 = floor + (1.0F - floor) * MathHelper.fractionalPart((j + 1) / (float) steps + spin);
            if (k1 < k0) {
                k1 = 1.0F;
            }
            Vec3d o0 = u.multiply(MathHelper.cos(a0) * radius).add(v.multiply(MathHelper.sin(a0) * radius));
            Vec3d o1 = u.multiply(MathHelper.cos(a1) * radius).add(v.multiply(MathHelper.sin(a1) * radius));
            vertex(vc, m, center.add(o0).add(back), color, alpha * k0);
            vertex(vc, m, center.add(o1).add(back), color, alpha * k1);
            vertex(vc, m, center.add(o1).add(front), color, alpha * k1);
            vertex(vc, m, center.add(o0).add(front), color, alpha * k0);
        }
    }

    // flat dashed ring lying in the u/v plane
    private static void flatRing(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d u, Vec3d v, float radius,
                                 float width, int[] color, float alpha, float spin, int dashes) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        int steps = dashes * 6;
        for (int j = 0; j < steps; j++) {
            if (j % 6 == 5) {
                continue;
            }
            float a0 = j * MathHelper.TAU / steps + spin;
            float a1 = (j + 1) * MathHelper.TAU / steps + spin;
            Vec3d d0 = u.multiply(MathHelper.cos(a0)).add(v.multiply(MathHelper.sin(a0)));
            Vec3d d1 = u.multiply(MathHelper.cos(a1)).add(v.multiply(MathHelper.sin(a1)));
            vertex(vc, m, center.add(d0.multiply(radius - width)), color, alpha);
            vertex(vc, m, center.add(d1.multiply(radius - width)), color, alpha);
            vertex(vc, m, center.add(d1.multiply(radius + width)), color, alpha);
            vertex(vc, m, center.add(d0.multiply(radius + width)), color, alpha);
        }
    }

    // filled circle, bright in the middle and fading to edgeAlpha at the rim
    private static void disc(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d u, Vec3d v, float radius,
                             int[] color, float alpha, float edgeAlpha) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        int steps = 24;
        for (int j = 0; j < steps; j++) {
            float a0 = j * MathHelper.TAU / steps;
            float a1 = (j + 1) * MathHelper.TAU / steps;
            Vec3d p0 = center.add(u.multiply(MathHelper.cos(a0) * radius)).add(v.multiply(MathHelper.sin(a0) * radius));
            Vec3d p1 = center.add(u.multiply(MathHelper.cos(a1) * radius)).add(v.multiply(MathHelper.sin(a1) * radius));
            vertex(vc, m, center, color, alpha);
            vertex(vc, m, center, color, alpha);
            vertex(vc, m, p1, color, edgeAlpha);
            vertex(vc, m, p0, color, edgeAlpha);
        }
    }

    private static void billboardGlow(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d camRight, Vec3d camUp,
                                      float radius, int[] color, float alpha) {
        disc(vc, m, center, camRight, camUp, radius, color, alpha, 0.0F);
    }

    private static void spike(VertexConsumer vc, Matrix4f m, Vec3d from, Vec3d dir, Vec3d side, float length,
                              float width, int[] color, float alpha) {
        Vec3d tip = from.add(dir.multiply(length));
        vertex(vc, m, from.add(side.multiply(width)), color, alpha);
        vertex(vc, m, from.subtract(side.multiply(width)), color, alpha);
        vertex(vc, m, tip, color, 0.0F);
        vertex(vc, m, tip, color, 0.0F);
    }

    private static Vec3d rotate(Quaternionf rotation, float x, float y, float z) {
        Vector3f out = rotation.transform(new Vector3f(x, y, z));
        return new Vec3d(out.x(), out.y(), out.z());
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3d p, int[] color, float alpha) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(color[0], color[1], color[2], (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255))
                .next();
    }
}
