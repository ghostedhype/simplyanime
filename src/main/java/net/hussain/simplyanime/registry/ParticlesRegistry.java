package net.hussain.simplyanime.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryKeys;

public class ParticlesRegistry {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(SimplyAnime.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    public static final RegistrySupplier<DefaultParticleType> HEAVEN_SPARK = PARTICLES.register(
            "heaven_spark",
            () -> new DefaultParticleType(true) {}
    );

    public static final RegistrySupplier<DefaultParticleType> SEVERED_ASH = PARTICLES.register(
            "severed_ash",
            () -> new DefaultParticleType(true) {}
    );

    public static final RegistrySupplier<DefaultParticleType> RUPTURE_EMBER = PARTICLES.register(
            "rupture_ember",
            () -> new DefaultParticleType(true) {}
    );
}
