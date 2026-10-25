package net.ghostedhype.simplyanime.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.NotNull;

public class HeavenSparkParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public HeavenSparkParticle(ClientLevel level,
                               double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.05;
        this.yd = ySpeed + (this.random.nextDouble() - 0.5) * 0.05;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.05;
        this.friction = 0.82f;
        this.gravity = 0.1f;
        this.quadSize = 0.08f + this.random.nextFloat() * 0.08f;
        this.lifetime = 6 + this.random.nextInt(6);
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet);

        float life = (float) this.age / this.lifetime;
        this.alpha = 1.0F - life * 0.7F;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
