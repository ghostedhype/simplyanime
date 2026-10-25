package net.ghostedhype.simplyanime.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class SimpleParticleFactory implements ParticleProvider<SimpleParticleType> {

    private final SpriteSet spriteSet;
    private final ParticleConstructor particleConstruct;

    public SimpleParticleFactory(SpriteSet spriteSet, ParticleConstructor particleConstruct) {
        this.spriteSet = spriteSet;
        this.particleConstruct = particleConstruct;
    }

    @Override
    public @Nullable Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel,
                                             double x, double y, double z,
                                             double velocityX, double velocityY, double velocityZ) {
        return this.particleConstruct.create(clientLevel,
                                       x, y, z,
                                       velocityX, velocityY, velocityZ,
                                       this.spriteSet);
    }
}
