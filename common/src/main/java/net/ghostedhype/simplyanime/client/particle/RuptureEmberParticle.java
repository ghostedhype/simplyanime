package net.ghostedhype.simplyanime.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.NotNull;

public class RuptureEmberParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public RuptureEmberParticle(ClientLevel level,
                                double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.friction = 0.9f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.quadSize = 0.1f + this.random.nextFloat() * 0.12f;
        this.lifetime = 10 + this.random.nextInt(10);
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet);

        float life = (float) this.age / this.lifetime;
        this.gCol = 0.8f - life * 0.7f;
        this.bCol = 0.45f - life * 0.4f;
        this.alpha = 1.0f - life * life;
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
