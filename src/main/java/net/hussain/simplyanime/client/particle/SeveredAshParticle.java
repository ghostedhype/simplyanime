package net.hussain.simplyanime.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class SeveredAshParticle extends SpriteBillboardParticle {

    protected SeveredAshParticle(ClientWorld world, double x, double y, double z,
                                 double velocityX, double velocityY, double velocityZ, SpriteProvider sprites) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityMultiplier = 0.88F;
        this.gravityStrength = -0.02F;
        this.scale = 0.1F + this.random.nextFloat() * 0.12F;
        this.maxAge = 14 + this.random.nextInt(10);
        this.angle = this.random.nextFloat() * 6.28F;
        this.prevAngle = this.angle;
        this.setSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.angle += 0.08F;
        this.alpha = 1.0F - (float) this.age / this.maxAge;
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
            return new SeveredAshParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
