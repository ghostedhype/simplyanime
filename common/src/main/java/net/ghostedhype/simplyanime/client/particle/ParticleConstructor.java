package net.ghostedhype.simplyanime.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;

@FunctionalInterface
public interface ParticleConstructor {
    Particle create(ClientLevel level,
                    double x, double y, double z,
                    double xSpeed, double ySpeed, double zSpeed,
                    SpriteSet spriteSet);
}
