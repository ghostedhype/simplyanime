package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.config.RhittaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Map;
import java.util.WeakHashMap;

public class SunPower {

    private static final Map<LivingEntity, Cached> SERVER_CACHE = new WeakHashMap<>();
    private static final Map<LivingEntity, Cached> CLIENT_CACHE = new WeakHashMap<>();

    // multiplier, min at midnight up to peak at noon
    public static float sunPower(Level world, LivingEntity entity) {
        if (world == null || entity == null) {
            return 1.0F;
        }
        Map<LivingEntity, Cached> cache = world.isClientSide() ? CLIENT_CACHE : SERVER_CACHE;
        long now = world.getGameTime();
        Cached cached = cache.get(entity);
        if (cached != null && cached.time == now) {
            return cached.power;
        }
        float power = fromLevel(sunLevel(world, entity.blockPosition()));
        cache.put(entity, new Cached(now, power));
        return power;
    }

    public static float sunLevel(Level world, BlockPos pos) {
        RhittaConfig.SunSettings config = RhittaConfig.get().sun;
        Float override = dimensionOverride(world, config);
        if (override != null) {
            return override;
        }
        DimensionType dimension = world.dimensionType();
        if (!dimension.hasSkyLight() || dimension.hasFixedTime()) {
            return config.skylessMode.get() == SkylessMode.NOON ? 1.0F : Mth.clamp(config.skylessLevel, 0.0F, 1.0F);
        }

        float level = timeCurve(world.getDayTime(), config.curve.get());
        if (config.indoorsMatter) {
            float sky = world.getBrightness(LightLayer.SKY, pos) / 15.0F;
            level *= 1.0F - config.indoorPenalty * (1.0F - sky);
        }
        if (config.weatherMatters && world.isRaining() && getsRain(world, pos)) {
            level *= world.isThundering() ? config.thunderFactor : config.rainFactor;
        }
        return Float.isFinite(level) ? Mth.clamp(level, 0.0F, 1.0F) : 0.0F;
    }

    public static float fromLevel(float level) {
        RhittaConfig.SunSettings config = RhittaConfig.get().sun;
        float power = Mth.lerp(level, config.minPower, config.peakPower);
        return Float.isFinite(power) ? Math.max(0.0F, power) : 1.0F;
    }

    public static float fraction(float power) {
        RhittaConfig.SunSettings config = RhittaConfig.get().sun;
        float span = config.peakPower - config.minPower;
        if (Math.abs(span) < 1.0E-4F) {
            return 1.0F;
        }
        float f = (power - config.minPower) / span;
        return Float.isFinite(f) ? Mth.clamp(f, 0.0F, 1.0F) : 0.0F;
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

    private static boolean getsRain(Level world, BlockPos blockPos) {
        return world.getBiome(blockPos).value().getPrecipitationAt(blockPos) != Biome.Precipitation.NONE;
    }

    private static Float dimensionOverride(Level world, RhittaConfig.SunSettings config) {
        if (config.dimensionOverrides.isBlank()) {
            return null;
        }
        ResourceLocation id = world.dimension().location();
        for (String entry : config.dimensionOverrides.split(",")) {
            int equalsIndex = entry.indexOf('=');
            if (equalsIndex <= 0 || !id.toString().equals(entry.substring(0, equalsIndex).trim())) {
                continue;
            }
            try {
                return Mth.clamp(Float.parseFloat(entry.substring(equalsIndex + 1).trim()), 0.0F, 1.0F);
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
