package net.ghostedhype.simplyanime.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AnimationClip {

    public static final int ROOT = 0;
    public static final int HEAD = 1;
    public static final int BODY = 2;
    public static final int RIGHT_ARM = 3;
    public static final int LEFT_ARM = 4;
    public static final int RIGHT_LEG = 5;
    public static final int LEFT_LEG = 6;
    public static final int RIGHT_ITEM = 7;
    public static final int LEFT_ITEM = 8;
    public static final int PARTS = 9;

    public static final int RIGHT_TIP = 0;
    public static final int RIGHT_BASE = 1;

    public static final int ELUCIDATOR = 1;
    public static final int DARK_REPULSER = 2;

    public static final float PIXEL = 0.9375F / 16.0F;
    private static final String[] PART_NAMES = {"root", "head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "right_item", "left_item"};

    public record Marker(String name, int frame) {
    }

    public final String name;
    public final int fps;
    public final int frames;
    public final boolean loop;
    // which sword sits in each hand, 0 for an empty one
    public final int rightSword;
    public final int leftSword;
    private final int[] slot = new int[PARTS];
    private final int stride;
    private final float[] transforms;
    private final float[] points;
    private final List<Marker> markers = new ArrayList<>();

    AnimationClip(String name, JsonObject json) {
        this.name = name;
        this.fps = json.get("fps").getAsInt();
        this.loop = json.get("loop").getAsBoolean();
        this.rightSword = json.get("right_sword").getAsInt();
        this.leftSword = json.get("left_sword").getAsInt();
        for (JsonElement element : json.getAsJsonArray("markers")) {
            JsonObject marker = element.getAsJsonObject();
            this.markers.add(new Marker(marker.get("name").getAsString(), marker.get("frame").getAsInt()));
        }
        JsonArray pointRows = json.getAsJsonArray("points");
        this.frames = pointRows.size();
        JsonObject parts = json.getAsJsonObject("parts");
        int next = 0;
        for (int part = ROOT; part < PARTS; part++) {
            this.slot[part] = parts.has(PART_NAMES[part]) ? next++ : -1;
        }
        this.stride = next * 7;
        this.transforms = new float[this.frames * this.stride];
        this.points = new float[this.frames * 12];
        for (int part = ROOT; part < PARTS; part++) {
            if (this.slot[part] < 0) {
                continue;
            }
            JsonObject channels = parts.getAsJsonObject(PART_NAMES[part]);
            JsonArray positions = channels.getAsJsonArray("position");
            JsonArray rotations = channels.getAsJsonArray("rotation");
            for (int frame = 0; frame < this.frames; frame++) {
                int frameOffset = frame * this.stride + this.slot[part] * 7;
                for (int i = 0; i < 3; i++) {
                    this.transforms[frameOffset + i] = positions.get(frame).getAsJsonArray().get(i).getAsFloat();
                }
                for (int i = 0; i < 4; i++) {
                    this.transforms[frameOffset + 3 + i] = rotations.get(frame).getAsJsonArray().get(i).getAsFloat();
                }
            }
        }
        for (int frame = 0; frame < this.frames; frame++) {
            JsonArray row = pointRows.get(frame).getAsJsonArray();
            for (int i = 0; i < 12; i++) {
                this.points[frame * 12 + i] = row.get(i).getAsFloat() * PIXEL;
            }
        }
    }

    public float framesPerTick() {
        return this.fps / 20.0F;
    }

    public int lengthTicks() {
        return Math.round((this.frames - 1) / this.framesPerTick());
    }

    public List<Marker> markers() {
        return this.markers;
    }

    public int marker(String name) {
        for (Marker marker : this.markers) {
            if (marker.name().equals(name)) {
                return marker.frame();
            }
        }
        return -1;
    }

    private float wrap(float frame) {
        int last = this.frames - 1;
        if (this.loop) {
            frame %= last;
            return frame < 0 ? frame + last : frame;
        }
        return Math.max(0.0F, Math.min(last, frame));
    }

    public boolean sample(int part, float frame, Vector3f position, Quaternionf rotation) {
        return this.slot[part] >= 0 && this.read(this.slot[part] * 7, frame, position, rotation);
    }

    private boolean read(int offset, float frame, Vector3f position, Quaternionf rotation) {
        frame = this.wrap(frame);
        int from = (int) frame;
        int to = Math.min(this.frames - 1, from + 1);
        float blend = frame - from;
        int start = from * this.stride + offset;
        int end = to * this.stride + offset;
        float[] data = this.transforms;
        position.set(data[start] + (data[end] - data[start]) * blend, data[start + 1] + (data[end + 1] - data[start + 1]) * blend,
                data[start + 2] + (data[end + 2] - data[start + 2]) * blend);
        float dot = data[start + 3] * data[end + 3] + data[start + 4] * data[end + 4] + data[start + 5] * data[end + 5] + data[start + 6] * data[end + 6];
        float endWeight = dot < 0 ? -blend : blend;
        rotation.set(data[start + 3] * (1 - blend) + data[end + 3] * endWeight, data[start + 4] * (1 - blend) + data[end + 4] * endWeight,
                data[start + 5] * (1 - blend) + data[end + 5] * endWeight, data[start + 6] * (1 - blend) + data[end + 6] * endWeight).normalize();
        return true;
    }

    public Vector3f point(int which, float frame, Vector3f out) {
        frame = this.wrap(frame);
        int from = (int) frame;
        int to = Math.min(this.frames - 1, from + 1);
        float blend = frame - from;
        int start = from * 12 + which * 3;
        int end = to * 12 + which * 3;
        float[] data = this.points;
        return out.set(data[start] + (data[end] - data[start]) * blend, data[start + 1] + (data[end + 1] - data[start + 1]) * blend,
                data[start + 2] + (data[end + 2] - data[start + 2]) * blend);
    }
}