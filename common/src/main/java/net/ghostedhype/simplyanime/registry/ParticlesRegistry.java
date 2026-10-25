package net.ghostedhype.simplyanime.registry;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar;
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier;
import net.ghostedhype.simplyanime.Constants;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class ParticlesRegistry {

    private static final Registrar<ParticleType<?>> PARTICLES =
            ConfigApiJava.platform().createRegistrar(Constants.MOD_ID, BuiltInRegistries.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> BLADE_SPARK_CRIMSON = register("blade_spark_crimson");
    public static final RegistrySupplier<SimpleParticleType> BLADE_SPARK_FROST   = register("blade_spark_frost");
    public static final RegistrySupplier<SimpleParticleType> BLADE_SPARK_STAR    = register("blade_spark_star");
    public static final RegistrySupplier<SimpleParticleType> HEAVEN_SPARK        = register("heaven_spark");
    public static final RegistrySupplier<SimpleParticleType> RUPTURE_EMBER       = register("rupture_ember");
    public static final RegistrySupplier<SimpleParticleType> SEVERED_ASH         = register("severed_ash");
    public static final RegistrySupplier<SimpleParticleType> SUN_GLOW_SPARK      = register("sun_glow_spark");
    public static final RegistrySupplier<SimpleParticleType> SUN_GLOW_WISP       = register("sun_glow_wisp");

    public static void init() {
        PARTICLES.init();
    }

    protected static RegistrySupplier<SimpleParticleType> register(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(true) {});
    }
}
