package net.ghostedhype.simplyanime.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class BladeSparkParticle extends TextureSheetParticle {

    private static final int STAR = 0;
    private static final int CRIMSON = 1;
    private static final int FROST = 2;

    private final int kind;
    private final float startScale;
    private final float turn;

    public BladeSparkParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ,
                               SpriteSet sprites, int kind) {
        super(world, x, y, z, 0, 0, 0);
        this.kind = kind;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.hasPhysics = false;
        this.friction = kind == FROST ? 0.93F : 0.9F;
        this.gravity = kind == FROST ? 0.35F : 0.0F;
        this.startScale = kind == STAR ? 0.12F + this.random.nextFloat() * 0.2F : 0.07F + this.random.nextFloat() * 0.1F;
        this.lifetime = kind == CRIMSON ? 10 + this.random.nextInt(8) : 14 + this.random.nextInt(16);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.turn = (this.random.nextFloat() - 0.5F) * (kind == FROST ? 0.6F : 0.25F);
        this.quadSize = this.startScale;
        this.pickSprite(sprites);
        this.shade(0.0F);
    }

    public static BladeSparkParticle star(ClientLevel level,
                                          double x, double y, double z,
                                          double xSpeed, double ySpeed, double zSpeed,
                                          SpriteSet spriteSet) {
        return new BladeSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, STAR);
    }

    public static BladeSparkParticle crimson(ClientLevel level,
                                             double x, double y, double z,
                                             double xSpeed, double ySpeed, double zSpeed,
                                             SpriteSet spriteSet) {
        return new BladeSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, CRIMSON);
    }

    public static BladeSparkParticle frost(ClientLevel level,
                                           double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed,
                                           SpriteSet spriteSet) {
        return new BladeSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, FROST);
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.turn;
        this.shade((float) this.age / this.lifetime);
    }

    private void shade(float life) {
        // stars twinkle, the others just burn down
        float twinkle = this.kind == STAR ? 0.75F + 0.25F * Mth.sin(this.age * 1.3F + this.roll * 4.0F) : 1.0F;
        this.quadSize = this.startScale * twinkle * (1.0F - life * life * 0.7F);
        this.alpha = 1.0F - life * life * life;
        switch (this.kind) {
            case STAR -> {
                this.rCol = 1.0F - life * 0.55F;
                this.gCol = 1.0F - life * 0.3F;
                this.bCol = 1.0F;
            }
            case CRIMSON -> {
                this.rCol = 1.0F;
                this.gCol = Mth.clamp(0.75F - life * 1.1F, 0.05F, 1.0F);
                this.bCol = Mth.clamp(0.7F - life * 1.4F, 0.08F, 1.0F);
            }
            default -> {
                this.rCol = 0.75F - life * 0.45F;
                this.gCol = 1.0F;
                this.bCol = 0.97F;
            }
        }
    }

    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return (tesselator, textureManager) -> {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        };
    }
}
