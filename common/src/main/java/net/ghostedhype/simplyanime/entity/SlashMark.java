package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.ability.AbilityHelper;
import net.ghostedhype.simplyanime.animation.AnimationClip;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SlashMark {

    public static final int DARK = 0;
    public static final int TEAL = 1;
    public static final int STAR = 2;

    public static final float LIFE = 34.0F;
    // the wielder's chest, the sweep is scaled away from here
    private static final Vector3f CHEST = new Vector3f(0.0F, 1.15F, 0.0F);

    public final Vec3[] inner;
    public final Vec3[] outer;
    public final int blade;
    public final float bornAt;
    public final int seed;

    private SlashMark(Vec3[] inner, Vec3[] outer, int blade, float bornAt, int seed) {
        this.inner = inner;
        this.outer = outer;
        this.blade = blade;
        this.bornAt = bornAt;
        this.seed = seed;
    }

    public static SlashMark swing(AnimationClip clip, int point, int frame, int window, Vec3 origin, float yaw, boolean mirror,
                                  float scale, int blade, float time) {
        int count = window * 2 + 1;
        Vec3[] inner = new Vec3[count];
        Vec3[] outer = new Vec3[count];
        Vector3f tip = new Vector3f();
        for (int i = 0; i < count; i++) {
            clip.point(point, frame - window + i, tip).sub(CHEST);
            outer[i] = AbilityHelper.toWorld(new Vector3f(tip).mul(scale).add(CHEST), origin, yaw, mirror);
            inner[i] = AbilityHelper.toWorld(new Vector3f(tip).mul(scale * 0.5F).add(CHEST), origin, yaw, mirror);
        }
        return new SlashMark(inner, outer, blade, time, frame * 31 + point);
    }

    // one straight slash through a point, thin at both ends
    public static SlashMark streak(Vec3 center, Vec3 direction, Vec3 across, float length, float width, int blade, float time,
                                   int seed) {
        int count = 9;
        Vec3[] inner = new Vec3[count];
        Vec3[] outer = new Vec3[count];
        for (int i = 0; i < count; i++) {
            float lineOffset = (float) i / (count - 1) * 2.0F - 1.0F;
            Vec3 linePosition = center.add(direction.scale(lineOffset * length * 0.5F));
            Vec3 side = across.scale(width * (1.0F - lineOffset * lineOffset));
            inner[i] = linePosition.subtract(side);
            outer[i] = linePosition.add(side);
        }
        return new SlashMark(inner, outer, blade, time, seed);
    }
}
