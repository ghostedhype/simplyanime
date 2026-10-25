package net.ghostedhype.simplyanime.client.registry;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.ghostedhype.simplyanime.client.particle.*;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;

public class SimplyAnimeParticleRegistryFabric {
    public static void register() {
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.BLADE_SPARK_CRIMSON.get(), sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::crimson));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.BLADE_SPARK_FROST.get(),   sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::frost));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.BLADE_SPARK_STAR.get(),    sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::star));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.HEAVEN_SPARK.get(),        sprites -> new SimpleParticleFactory(sprites, HeavenSparkParticle::new));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.RUPTURE_EMBER.get(),       sprites -> new SimpleParticleFactory(sprites, RuptureEmberParticle::new));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.SEVERED_ASH.get(),         sprites -> new SimpleParticleFactory(sprites, SeveredAshParticle::new));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.SUN_GLOW_SPARK.get(),      sprites -> new SimpleParticleFactory(sprites, SunGlowParticle::spark));
        ParticleFactoryRegistry.getInstance().register(ParticlesRegistry.SUN_GLOW_WISP.get(),       sprites -> new SimpleParticleFactory(sprites, SunGlowParticle::wisp));
    }
}
