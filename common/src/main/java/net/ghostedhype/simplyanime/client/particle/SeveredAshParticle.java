package net.ghostedhype.simplyanime.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.NotNull;

public class SeveredAshParticle extends TextureSheetParticle {

    public SeveredAshParticle(ClientLevel level,
                              double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.88f;
        this.gravity = -0.02f;
        this.quadSize = 0.1f + this.random.nextFloat() * 0.12f;
        this.lifetime = 14 + this.random.nextInt(10);
        this.roll = this.random.nextFloat() * 6.28f;
        this.oRoll = this.roll;
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / this.lifetime;

        this.oRoll = this.roll;
        this.roll += 0.08f;
        this.alpha = 1.0f - life;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
