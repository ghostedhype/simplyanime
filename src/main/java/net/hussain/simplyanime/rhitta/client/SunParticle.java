package net.hussain.simplyanime.rhitta.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

// flames swell and burn out, embers keep their speed
public class SunParticle extends SpriteBillboardParticle {

    private final SpriteProvider sprites;
    private final boolean flame;
    private final float startScale;

    protected SunParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz,
                          SpriteProvider sprites, boolean flame) {
        super(world, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.flame = flame;
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.gravityStrength = flame ? -0.02F : 0.0F;
        this.velocityMultiplier = flame ? 0.92F : 0.94F;
        this.collidesWithWorld = false;
        this.startScale = flame ? 0.25F + this.random.nextFloat() * 0.3F : 0.06F + this.random.nextFloat() * 0.08F;
        this.scale = this.startScale;
        this.maxAge = flame ? 12 + this.random.nextInt(12) : 14 + this.random.nextInt(10);
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.sprites);
        float life = (float) this.age / this.maxAge;
        if (this.flame) {
            this.scale = this.startScale * (0.7F + 0.8F * MathHelper.sin(life * MathHelper.PI));
            this.red = 1.0F;
            this.green = MathHelper.clamp(1.0F - life * 1.1F, 0.15F, 1.0F);
            this.blue = MathHelper.clamp(0.75F - life * 1.8F, 0.0F, 1.0F);
            this.alpha = 1.0F - life * life;
        } else {
            this.red = 1.0F;
            this.green = 0.95F - life * 0.55F;
            this.blue = 0.5F - life * 0.45F;
            this.alpha = 1.0F - life * life * life;
        }
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class FlameFactory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public FlameFactory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SunParticle(world, x, y, z, vx, vy, vz, this.sprites, true);
        }
    }

    public static class EmberFactory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public EmberFactory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SunParticle(world, x, y, z, vx, vy, vz, this.sprites, false);
        }
    }
}
