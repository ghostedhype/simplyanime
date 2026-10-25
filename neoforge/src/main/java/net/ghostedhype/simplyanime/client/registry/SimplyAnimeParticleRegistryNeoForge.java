package net.ghostedhype.simplyanime.client.registry;

import net.ghostedhype.simplyanime.client.particle.*;
import net.ghostedhype.simplyanime.registry.ParticlesRegistry;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public class SimplyAnimeParticleRegistryNeoForge {
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticlesRegistry.BLADE_SPARK_CRIMSON.get(),    sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::crimson));
        event.registerSpriteSet(ParticlesRegistry.BLADE_SPARK_FROST.get(),      sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::frost));
        event.registerSpriteSet(ParticlesRegistry.BLADE_SPARK_STAR.get(),       sprites -> new SimpleParticleFactory(sprites, BladeSparkParticle::star));
        event.registerSpriteSet(ParticlesRegistry.HEAVEN_SPARK.get(),           sprites -> new SimpleParticleFactory(sprites, HeavenSparkParticle::new));
        event.registerSpriteSet(ParticlesRegistry.RUPTURE_EMBER.get(),          sprites -> new SimpleParticleFactory(sprites, RuptureEmberParticle::new));
        event.registerSpriteSet(ParticlesRegistry.SEVERED_ASH.get(),            sprites -> new SimpleParticleFactory(sprites, SeveredAshParticle::new));
        event.registerSpriteSet(ParticlesRegistry.SUN_GLOW_SPARK.get(),         sprites -> new SimpleParticleFactory(sprites, SunGlowParticle::spark));
        event.registerSpriteSet(ParticlesRegistry.SUN_GLOW_WISP.get(),          sprites -> new SimpleParticleFactory(sprites, SunGlowParticle::wisp));
    }
}