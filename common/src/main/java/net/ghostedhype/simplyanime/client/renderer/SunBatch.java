package net.ghostedhype.simplyanime.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SunBatch {

    private static final int[] WHITE = {255, 255, 255};
    // how much of a flame's colour gets added again on top
    private static final float LIT = 0.35F;
    private static final int LON = 40;
    private static final int LAT = 20;
    private static final float[][][] BALL = new float[LAT + 1][LON + 1][];

    static {
        for (int j = 0; j <= LAT; j++) {
            float lat = (j / (float) LAT - 0.5F) * Mth.PI;
            float ring = Mth.cos(lat);
            for (int i = 0; i <= LON; i++) {
                float lon = i * Mth.TWO_PI / LON;
                BALL[j][i] = new float[]{ring * Mth.cos(lon), Mth.sin(lat), ring * Mth.sin(lon), ring};
            }
        }
    }

    private record Corner(float x, float y, float z, int red, int green, int blue, int alpha, float u, float v) {
    }

    private final Map<ResourceLocation, List<Corner>> additive = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<Corner>> blended = new LinkedHashMap<>();
    private final List<Corner> depth = new ArrayList<>();
    private final Map<ResourceLocation, List<Corner>> solid = new LinkedHashMap<>();


    private final float soft = ShaderCompat.packInUse() ? 0.12F : 1.0F;
    final Vec3 right;
    final Vec3 cameraUp;
    // camera relative to the entity
    final Vec3 eye;

    public SunBatch(Vec3 right, Vec3 cameraUp, Vec3 eye) {
        this.right = right;
        this.cameraUp = cameraUp;
        this.eye = eye;
    }

    float facing(Vec3 centerVec, Vec3 normalVec, float radius) {
        Vec3 pointVec = centerVec.add(normalVec.scale(radius));
        return (float) normalVec.dot(this.eye.subtract(pointVec).normalize());
    }

    void drawBall(boolean solid, ResourceLocation texture, Vec3 centerVec, float radius, float tilt, float slide,
                  int[] core, int[] rim, float coreAlpha, float rimAlpha) {
        if (radius <= 0.001F || (coreAlpha <= 0.005F && rimAlpha <= 0.005F)) {
            return;
        }
        // opaque under packs when nothing shows through
        boolean opaque = solid && coreAlpha > 0.95F && rimAlpha > 0.95F && ShaderCompat.packInUse();
        List<Corner> list = (opaque ? this.solid : solid ? this.blended : this.additive).computeIfAbsent(texture, resourceLocation -> new ArrayList<>());
        float cosTilt = Mth.cos(tilt);
        float sinTilt = Mth.sin(tilt);
        // camera inside the ball
        boolean inside = this.eye.distanceToSqr(centerVec) < radius * radius;
        Corner[] corners = new Corner[4];
        for (int latitudeIndex = 0; latitudeIndex < LAT; latitudeIndex++) {
            for (int longitudeIndex = 0; longitudeIndex < LON; longitudeIndex++) {
                boolean seen = false;
                for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                    int ringIndex = longitudeIndex + (cornerIndex == 1 || cornerIndex == 2 ? 1 : 0);
                    int segmentIndex = latitudeIndex + (cornerIndex >= 2 ? 1 : 0);
                    float[] unit = BALL[segmentIndex][ringIndex];
                    Vec3 normalVec = new Vec3(unit[0], unit[1] * cosTilt - unit[2] * sinTilt, unit[1] * sinTilt + unit[2] * cosTilt);
                    Vec3 pointVec = centerVec.add(normalVec.scale(radius));
                    float facing = (float) normalVec.dot(this.eye.subtract(pointVec).normalize());
                    if (inside) {
                        facing = -facing;
                    }
                    seen |= facing > 0.0F;
                    float swing = Mth.sqrt(Mth.clamp(facing, 0.0F, 1.0F));
                    // uv pinches at the poles, fade it
                    float alpha = Mth.lerp(swing, rimAlpha, coreAlpha) * (solid ? 1.0F : Math.min(1.0F, unit[3] * 2.5F));
                    corners[cornerIndex] = corner(pointVec, mix(rim, core, swing), alpha, ringIndex * 2.0F / LON + slide, segmentIndex / (float) LAT);
                }
                if (seen) {
                    for (Corner corner : corners) {
                        list.add(corner);
                    }
                }
            }
        }
    }

    // packs only, see SimplyAnimeRenderTypes.depth
    void drawDepthBall(Vec3 centerVec, float radius) {
        if (!ShaderCompat.packInUse() || radius <= 0.001F || this.eye.distanceToSqr(centerVec) < radius * radius) {
            return;
        }
        Corner[] corners = new Corner[4];
        for (int latitudeIndex = 0; latitudeIndex < LAT; latitudeIndex++) {
            for (int longitudeIndex = 0; longitudeIndex < LON; longitudeIndex++) {
                boolean seen = false;
                for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                    float[] unit = BALL[latitudeIndex + (cornerIndex >= 2 ? 1 : 0)][longitudeIndex + (cornerIndex == 1 || cornerIndex == 2 ? 1 : 0)];
                    Vec3 normalVec = new Vec3(unit[0], unit[1], unit[2]);
                    Vec3 pointVec = centerVec.add(normalVec.scale(radius));
                    seen |= normalVec.dot(this.eye.subtract(pointVec)) > 0.0;
                    corners[cornerIndex] = corner(pointVec, WHITE, 1.0F, 0.5F, 0.5F);
                }
                if (seen) {
                    for (Corner corner : corners) {
                        this.depth.add(corner);
                    }
                }
            }
        }
    }

    // flame on a ball at normalVec, facing the camera
    void drawTongue(Vec3 centerVec, Vec3 normalVec, float from, float length, float halfWidth, int variant, int[] color, float alpha) {
        Vec3 foot = centerVec.add(normalVec.scale(from));
        Vec3 side = normalVec.cross(this.eye.subtract(foot));
        if (side.lengthSqr() < 1.0E-6 || length <= 0.001F) {
            return;
        }
        float minU = (variant & 3) * 0.25F;
        this.drawFire(SunRenderTypes.FLAMES, foot.add(normalVec.scale(length * 0.5F)), side.normalize(), normalVec,
                halfWidth, length * 0.5F, color, alpha, minU, 0, minU + 0.25F, 1);
    }

    // camera facing strip along a path
    void drawRibbon(ResourceLocation texture, Vec3[] path, float halfFrom, float halfTo, int[] color, float alphaFrom, float alphaTo, float scroll) {
        if (alphaFrom <= 0.005F && alphaTo <= 0.005F) {
            return;
        }
        List<Corner> list = this.blended.computeIfAbsent(texture, resourceLocation -> new ArrayList<>());
        List<Corner> lit = this.additive.computeIfAbsent(texture, resourceLocation -> new ArrayList<>());
        int last = path.length - 1;
        Vec3[] sides = new Vec3[path.length];
        for (int i = 0; i <= last; i++) {
            Vec3 along = path[Math.min(last, i + 1)].subtract(path[Math.max(0, i - 1)]);
            Vec3 side = along.cross(this.eye.subtract(path[i]));
            sides[i] = side.lengthSqr() < 1.0E-6 ? this.right : side.normalize();
        }
        for (int i = 0; i < last; i++) {
            float startProgress = i / (float) last;
            float endProgress = (i + 1) / (float) last;
            Vec3 startSideVec = sides[i].scale(Mth.lerp(startProgress, halfFrom, halfTo));
            Vec3 endSideVec = sides[i + 1].scale(Mth.lerp(endProgress, halfFrom, halfTo));
            float startAngle = Mth.lerp(startProgress, alphaFrom, alphaTo);
            float endAngle = Mth.lerp(endProgress, alphaFrom, alphaTo);
            for (int pass = 0; pass < 2; pass++) {
                List<Corner> into = pass == 0 ? list : lit;
                float k = pass == 0 ? 1.0F : LIT;
                into.add(corner(path[i].subtract(startSideVec), color, startAngle * k, 0.0F, 1.0F - startProgress + scroll));
                into.add(corner(path[i].add(startSideVec), color, startAngle * k, 1.0F, 1.0F - startProgress + scroll));
                into.add(corner(path[i + 1].add(endSideVec), color, endAngle * k, 1.0F, 1.0F - endProgress + scroll));
                into.add(corner(path[i + 1].subtract(endSideVec), color, endAngle * k, 0.0F, 1.0F - endProgress + scroll));
            }
        }
    }

    // additive
    void drawGlow(Vec3 centerVec, float radius, int[] color, float alpha) {
        alpha *= this.soft;
        this.quad(this.additive, SunRenderTypes.GLOW, centerVec, this.right, this.cameraUp, radius, radius, color, alpha, 0, 0, 1, 1);
    }

    // blended
    void drawHalo(Vec3 centerVec, float radius, int[] color, float alpha) {
        alpha *= this.soft;
        this.drawFire(SunRenderTypes.GLOW, centerVec, this.right, this.cameraUp, radius, radius, color, alpha, 0, 0, 1, 1);
    }

    void drawFlatGlow(Vec3 centerVec, float radius, int[] color, float alpha) {
        alpha *= this.soft;
        this.drawFire(SunRenderTypes.GLOW, centerVec, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, radius,
                color, alpha, 0, 0, 1, 1);
    }

    void drawFlatRing(Vec3 centerVec, float radius, int[] color, float alpha) {
        this.drawFire(SunRenderTypes.RING, centerVec, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, radius,
                color, alpha, 0, 0, 1, 1);
    }

    // This is not used anywhere
    void drawFacingRing(Vec3 c, float radius, int[] color, float alpha) {
        this.drawFire(SunRenderTypes.RING, c, this.right, this.cameraUp, radius, radius, color, alpha, 0, 0, 1, 1);
    }

    // blended, then a bit additive
    private void drawFire(ResourceLocation texture, Vec3 centerVec, Vec3 u, Vec3 v, float halfU, float halfV, int[] color, float alpha,
                          float minU, float minV, float maxU, float maxV) {
        this.quad(this.blended, texture, centerVec, u, v, halfU, halfV, color, alpha, minU, minV, maxU, maxV);
        this.quad(this.additive, texture, centerVec, u, v, halfU, halfV, color, alpha * LIT, minU, minV, maxU, maxV);
    }

    void drawSmoke(Vec3 centerVec, float radius, float spin, int[] color, float alpha) {
        Vec3 u = this.right.scale(Mth.cos(spin)).add(this.cameraUp.scale(Mth.sin(spin)));
        Vec3 v = this.right.scale(-Mth.sin(spin)).add(this.cameraUp.scale(Mth.cos(spin)));
        this.quad(this.blended, SunRenderTypes.SMOKE, centerVec, u, v, radius, radius, color, alpha, 0, 0, 1, 1);
    }

    void drawRay(Vec3 centerVec, float a, float from, float to, float halfWidth, int[] color, float alpha) {
        Vec3 dirVec = this.right.scale(Mth.cos(a)).add(this.cameraUp.scale(Mth.sin(a)));
        Vec3 sideVec = this.right.scale(-Mth.sin(a)).add(this.cameraUp.scale(Mth.cos(a)));
        Vec3 midVec = centerVec.add(dirVec.scale((from + to) * 0.5F));
        this.quad(this.additive, SunRenderTypes.RAY, midVec, sideVec, dirVec, halfWidth, (to - from) * 0.5F, color, alpha, 0, 0, 1, 1);
    }

    void drawStreak(ResourceLocation texture, Vec3 a, Vec3 b, Vec3 w, float halfA, float halfB, int[] color, float alpha, float scroll) {
        List<Corner> list = this.blended.computeIfAbsent(texture, resourceLocation -> new ArrayList<>());
        float minU = 0.0F;
        float maxU = texture == SunRenderTypes.FLAMES ? 0.25F : 1.0F;
        list.add(corner(a.add(w.scale(-halfA)), color, alpha, minU, 1 + scroll));
        list.add(corner(a.add(w.scale(halfA)), color, alpha, maxU, 1 + scroll));
        list.add(corner(b.add(w.scale(halfB)), color, alpha * 0.2F, maxU, scroll));
        list.add(corner(b.add(w.scale(-halfB)), color, alpha * 0.2F, minU, scroll));
    }

    private void quad(Map<ResourceLocation, List<Corner>> target, ResourceLocation texture, Vec3 centerVec, Vec3 u, Vec3 v,
                      float halfU, float halfV, int[] color, float alpha, float minU, float minV, float maxU, float maxV) {
        if (alpha <= 0.005F || halfU <= 0.001F || halfV <= 0.001F) {
            return;
        }
        List<Corner> list = target.computeIfAbsent(texture, resourceLocation -> new ArrayList<>());
        Vec3 offsetU = u.scale(halfU);
        Vec3 offsetV = v.scale(halfV);
        // v0 is the top edge (along +v), v1 the bottom edge
        list.add(corner(centerVec.subtract(offsetU).subtract(offsetV), color, alpha, minU, maxV));
        list.add(corner(centerVec.add(offsetU).subtract(offsetV), color, alpha, maxU, maxV));
        list.add(corner(centerVec.add(offsetU).add(offsetV), color, alpha, maxU, minV));
        list.add(corner(centerVec.subtract(offsetU).add(offsetV), color, alpha, minU, minV));
    }

    private static Corner corner(Vec3 pointVec, int[] color, float alpha, float u, float v) {
        return new Corner((float) pointVec.x, (float) pointVec.y, (float) pointVec.z, color[0], color[1], color[2],
                (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255), u, v);
    }

    void flush(Matrix4f matrix4f, MultiBufferSource buffers) {
        this.solid.forEach((texture, list) -> emitCorners(matrix4f, VfxBuffer.of(buffers, SimplyAnimeRenderTypes.solid(texture)), list));
        if (!this.depth.isEmpty()) {
            emitCorners(matrix4f, VfxBuffer.of(buffers, SimplyAnimeRenderTypes.depth(SimplyAnimeRenderTypes.WHITE)), this.depth);
        }
        // smoke first so the fire glows over it
        this.blended.forEach((texture, list) -> emitCorners(matrix4f, VfxBuffer.of(buffers, SunRenderTypes.blended(texture)), list));
        this.additive.forEach((texture, list) -> emitCorners(matrix4f, VfxBuffer.of(buffers, SunRenderTypes.additive(texture)), list));
    }

    private static void emitCorners(Matrix4f matrix4f, VfxBuffer vertexBuffer, List<Corner> corners) {
        for (Corner corner : corners) {
            vertexBuffer.vertex(
                    matrix4f,
                    corner.x(),
                    corner.y(),
                    corner.z(),
                    corner.red(),
                    corner.green(),
                    corner.blue(),
                    corner.alpha(),
                    corner.u(),
                    corner.v()
            );
        }
    }

    static int[] mix(int[] from, int[] to, float progress) {
        return new int[]{
                Mth.lerpInt(progress, from[0], to[0]),
                Mth.lerpInt(progress, from[1], to[1]),
                Mth.lerpInt(progress, from[2], to[2])
        };
    }
}
