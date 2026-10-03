package net.hussain.simplyanime.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

// gold cooling to red, no gravity
public class RuptureEmberParticle extends SpriteBillboardParticle {

    private final SpriteProvider sprites;

    protected RuptureEmberParticle(ClientWorld world, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ, SpriteProvider sprites) {
        super(world, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = 0.9F;
        this.gravityStrength = 0.0F;
        this.collidesWithWorld = false;
        this.scale = 0.1F + this.random.nextFloat() * 0.12F;
        this.maxAge = 10 + this.random.nextInt(10);
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.sprites);
        float life = (float) this.age / this.maxAge;
        this.green = 0.8F - life * 0.7F;
        this.blue = 0.45F - life * 0.4F;
        this.alpha = 1.0F - life * life;
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
            return new RuptureEmberParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
