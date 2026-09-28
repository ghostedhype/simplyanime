package net.hussain.simplyanime.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class HeavenSparkParticle extends SpriteBillboardParticle {

    private final SpriteProvider sprites;

    protected HeavenSparkParticle(ClientWorld world, double x, double y, double z,
                                  double velocityX, double velocityY, double velocityZ, SpriteProvider sprites) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.sprites = sprites;
        this.velocityX = velocityX + (this.random.nextDouble() - 0.5) * 0.05;
        this.velocityY = velocityY + (this.random.nextDouble() - 0.5) * 0.05;
        this.velocityZ = velocityZ + (this.random.nextDouble() - 0.5) * 0.05;
        this.velocityMultiplier = 0.82F;
        this.gravityStrength = 0.1F;
        this.scale = 0.08F + this.random.nextFloat() * 0.08F;
        this.maxAge = 6 + this.random.nextInt(6);
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.sprites);
        this.alpha = 1.0F - (float) this.age / this.maxAge * 0.7F;
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new HeavenSparkParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
