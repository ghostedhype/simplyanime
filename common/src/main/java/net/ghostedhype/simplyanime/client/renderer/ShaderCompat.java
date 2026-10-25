package net.ghostedhype.simplyanime.client.renderer;

import net.ghostedhype.simplyanime.config.SimplyAnimeClientConfig;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class ShaderCompat {

    private static final long NAME_CACHE_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static long nameRead;
    private static boolean nameCached;
    private static String name = "";

    private ShaderCompat() {
    }

    public static boolean packInUse() {
        boolean active = ShaderRuntimeAccess.packInUse();
        if (!active) {
            invalidatePackName();
        }
        return active;
    }

    public static boolean shadowPass() {
        return ShaderRuntimeAccess.shadowPass();
    }

    public static boolean packNameHas(String part) {
        return part != null && !part.isEmpty()
                && packName().contains(part.toLowerCase(Locale.ROOT));
    }

    public static float gain(boolean solid) {
        return ShaderPackProfiles.gain(packName(), solid)
                * SimplyAnimeClientConfig.get().shaderGlow;
    }

    static ShaderPackProfiles.RenderPath renderPath() {
        return ShaderPackProfiles.renderPath(packName());
    }

    /** May also be called by a future shader-reload integration. */
    public static void invalidatePackName() {
        name = "";
        nameRead = 0L;
        nameCached = false;
    }

    private static String packName() {
        if (!packInUse()) {
            return "";
        }
        long now = System.nanoTime();
        if (!nameCached || now - nameRead >= NAME_CACHE_NANOS) {
            name = ShaderRuntimeAccess.packName().toLowerCase(Locale.ROOT);
            nameRead = now;
            nameCached = true;
        }
        return name;
    }
}
