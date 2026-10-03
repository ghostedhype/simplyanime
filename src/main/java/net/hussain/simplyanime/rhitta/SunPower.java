package net.hussain.simplyanime.rhitta;

import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

import java.util.Map;
import java.util.WeakHashMap;

// Sunshine. everything that scales with the sun reads sunPower()
// same on both sides, the client knows time, sky light and weather too
public class SunPower {

    // one cache per side, the integrated server and the client tick on different threads
    private static final Map<LivingEntity, Cached> SERVER_CACHE = new WeakHashMap<>();
    private static final Map<LivingEntity, Cached> CLIENT_CACHE = new WeakHashMap<>();

    // multiplier, min at midnight up to peak at noon
    public static float sunPower(World world, LivingEntity entity) {
        if (world == null || entity == null) {
            return 1.0F;
        }
        Map<LivingEntity, Cached> cache = world.isClient() ? CLIENT_CACHE : SERVER_CACHE;
        long now = world.getTime();
        Cached cached = cache.get(entity);
        if (cached != null && cached.time == now) {
            return cached.power;
        }
        float power = fromLevel(sunLevel(world, entity.getBlockPos()));
        cache.put(entity, new Cached(now, power));
        return power;
    }

    // 0..1, how much sun there is at a spot after time of day, sky access and weather
    public static float sunLevel(World world, BlockPos pos) {
        RhittaConfig.SunSettings cfg = RhittaConfig.get().sun;
        Float override = dimensionOverride(world, cfg);
        if (override != null) {
            return override;
        }
        DimensionType dimension = world.getDimension();
        if (!dimension.hasSkyLight() || dimension.hasFixedTime()) {
            return cfg.skylessMode.get() == SkylessMode.NOON ? 1.0F : MathHelper.clamp(cfg.skylessLevel, 0.0F, 1.0F);
        }

        float level = timeCurve(world.getTimeOfDay(), cfg.curve.get());
        if (cfg.indoorsMatter) {
            // raw sky light, the darkening at night is already in the time curve
            float sky = world.getLightLevel(LightType.SKY, pos) / 15.0F;
            level *= 1.0F - cfg.indoorPenalty * (1.0F - sky);
        }
        if (cfg.weatherMatters && world.isRaining() && getsRain(world, pos)) {
            level *= world.isThundering() ? cfg.thunderFactor : cfg.rainFactor;
        }
        return Float.isFinite(level) ? MathHelper.clamp(level, 0.0F, 1.0F) : 0.0F;
    }

    public static float fromLevel(float level) {
        RhittaConfig.SunSettings cfg = RhittaConfig.get().sun;
        float power = MathHelper.lerp(level, cfg.minPower, cfg.peakPower);
        return Float.isFinite(power) ? Math.max(0.0F, power) : 1.0F;
    }

    // where a power sits between night and noon, 0..1, for anything that just needs a blend
    public static float fraction(float power) {
        RhittaConfig.SunSettings cfg = RhittaConfig.get().sun;
        float span = cfg.peakPower - cfg.minPower;
        if (Math.abs(span) < 1.0E-4F) {
            return 1.0F;
        }
        float f = (power - cfg.minPower) / span;
        return Float.isFinite(f) ? MathHelper.clamp(f, 0.0F, 1.0F) : 0.0F;
    }

    // 1 at noon (6000), 0 at midnight (18000)
    static float timeCurve(long timeOfDay, Curve curve) {
        long day = Math.floorMod(timeOfDay, 24000L);
        double height = Math.cos((day - 6000L) / 24000.0 * Math.PI * 2.0);
        if (curve == Curve.STEPPED) {
            if (height > 0.5) {
                return 1.0F;
            }
            return height > -0.2 ? 0.5F : 0.0F;
        }
        return (float) ((height + 1.0) * 0.5);
    }

    private static boolean getsRain(World world, BlockPos pos) {
        return world.getBiome(pos).value().getPrecipitation(pos) != Biome.Precipitation.NONE;
    }

    // "minecraft:the_nether=0.8, minecraft:the_end=0.3", the value is a sun level 0..1
    private static Float dimensionOverride(World world, RhittaConfig.SunSettings cfg) {
        if (cfg.dimensionOverrides.isBlank()) {
            return null;
        }
        Identifier id = world.getRegistryKey().getValue();
        for (String entry : cfg.dimensionOverrides.split(",")) {
            int eq = entry.indexOf('=');
            if (eq <= 0 || !id.toString().equals(entry.substring(0, eq).trim())) {
                continue;
            }
            try {
                return MathHelper.clamp(Float.parseFloat(entry.substring(eq + 1).trim()), 0.0F, 1.0F);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public enum Curve {
        SMOOTH, STEPPED
    }

    public enum SkylessMode {
        NOON, FIXED
    }

    private record Cached(long time, float power) {
    }
}
