package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BladeSkillShapes {

    public static final int RELEASE_TICKS = 12;
    public static final float SQUARE_HALF = 1.93F;
    public static final double CROSS_AHEAD = 4.0;
    private static final String HIT = "hit_";

    public static boolean isHit(AnimationClip.Marker marker) {
        return marker.name().startsWith(HIT);
    }

    // hit_07_R -> 7
    public static int hitNumber(AnimationClip.Marker marker) {
        return Integer.parseInt(marker.name().substring(4, 6));
    }

    public static int hitCount(AnimationClip clip) {
        int count = 0;
        for (AnimationClip.Marker marker : clip.markers()) {
            if (isHit(marker)) {
                count = Math.max(count, hitNumber(marker));
            }
        }
        return count;
    }

    // frame of the nth hit, -1 when there is none
    public static int hitFrame(AnimationClip clip, int number) {
        for (AnimationClip.Marker marker : clip.markers()) {
            if (isHit(marker) && hitNumber(marker) == number) {
                return marker.frame();
            }
        }
        return -1;
    }

    public static boolean uses(AnimationClip.Marker marker, char hand) {
        return marker.name().indexOf(hand, 7) >= 0;
    }

    public static int window(AnimationClip clip, AnimationClip.Marker hit) {
        int gap = 30;
        for (AnimationClip.Marker other : clip.markers()) {
            if (isHit(other) && other.frame() != hit.frame()) {
                gap = Math.min(gap, Math.abs(other.frame() - hit.frame()));
            }
        }
        return Mth.clamp(gap, 5, 10);
    }

    public static boolean inFlurry(Vec3 feet, Vec3 look, AABB box, double reach) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 ahead = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(reach * 0.3);
        return AbilityHelper.distanceToPoint(box, feet.add(ahead).add(0, 1.15, 0)) <= reach;
    }

    // where the two bars of the last blow cross
    public static Vec3 crossCenter(Vec3 eye, Vec3 look) {
        return eye.add(look.scale(CROSS_AHEAD));
    }

    // the two bars of the X, across the way the wielder looks
    public static Vec3[] crossBars(Vec3 look) {
        Vec3 right = SlashWave.perpendicular(look);
        Vec3 upward = right.cross(look).normalize();
        if (upward.y < 0) {
            upward = upward.scale(-1.0);
        }
        return new Vec3[]{right.scale(0.74).add(upward.scale(0.67)).normalize(), right.scale(-0.74).add(upward.scale(0.67)).normalize()};
    }

    // inside the star burst, or on either bar of the X
    public static boolean inCross(Vec3 center, Vec3[] bars, AABB box, double reach, double burst) {
        if (AbilityHelper.distanceToPoint(box, center) <= burst) {
            return true;
        }
        for (Vec3 bar : bars) {
            double along = Mth.clamp(box.getCenter().subtract(center).dot(bar), -reach, reach);
            if (AbilityHelper.distanceToPoint(box, center.add(bar.scale(along))) <= 1.8) {
                return true;
            }
        }
        return false;
    }

    public static SlashWave squareSide(AnimationClip clip, int side, Vec3 origin, float yaw, boolean mirror, float range) {
        Vector3f local = new Vector3f();
        int from = clip.marker("corner_" + side);
        int to = side < 4 ? clip.marker("corner_" + (side + 1)) : clip.marker("finish");
        Vec3 a = AbilityHelper.toWorld(clip.point(AnimationClip.RIGHT_TIP, from, local), origin, yaw, mirror);
        Vec3 b = AbilityHelper.toWorld(clip.point(AnimationClip.RIGHT_TIP, to, local), origin, yaw, mirror);
        Vec3 middle = a.add(b).scale(0.5);
        Vec3 out = new Vec3(middle.x - origin.x, 0, middle.z - origin.z).normalize();
        Vec3 edge = b.subtract(a);
        return new SlashWave(middle, edge.normalize(), out, (float) edge.length() * 0.5F, 0.95F, range, 0.11F, 1.3F);
    }

    // half the width of the square as it bursts outward
    public static float releaseHalf(float ticks, float max) {
        float tickProgress = Mth.clamp(ticks / RELEASE_TICKS, 0.0F, 1.0F);
        return Mth.lerp(1.0F - (1.0F - tickProgress) * (1.0F - tickProgress), SQUARE_HALF, max);
    }

    public static Vec3 lanceOrigin(AnimationClip clip, Vec3 origin, float yaw, boolean mirror) {
        return AbilityHelper.toWorld(clip.point(AnimationClip.RIGHT_BASE, clip.marker("impact"), new Vector3f()), origin, yaw, mirror);
    }
}
