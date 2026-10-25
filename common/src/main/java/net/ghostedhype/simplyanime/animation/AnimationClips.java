package net.ghostedhype.simplyanime.animation;

import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationClips {

    private static final Map<String, AnimationClip> LOADED = new ConcurrentHashMap<>();

    public static AnimationClip get(String name) {
        return LOADED.computeIfAbsent(name, AnimationClips::load);
    }

    private static AnimationClip load(String name) {
        String path = "/data/simplyanime/animation/" + name + ".json";
        try (InputStream raw = AnimationClips.class.getResourceAsStream(path)) {
            if (raw == null) {
                throw new IllegalStateException("missing animation " + path);
            }
            return new AnimationClip(name, JsonParser.parseReader(new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException exception) {
            throw new IllegalStateException("could not read animation " + path, exception);
        }
    }
}
