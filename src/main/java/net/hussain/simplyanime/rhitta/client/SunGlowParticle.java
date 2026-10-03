package net.hussain.simplyanime.rhitta.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

// sparks (stretched along their velocity, additive) and wisps (soft puffs)
public class SunGlowParticle extends SpriteBillboardParticle {

    private static final ParticleTextureSheet ADDITIVE = new ParticleTextureSheet() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        }

        @Override
        public void draw(Tessellator tessellator) {
            tessellator.draw();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "simplyanime:sun_glow";
        }
    };

    private final boolean spark;
    private final float startScale;

    protected SunGlowParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz,
                              SpriteProvider sprites, boolean spark) {
        super(world, x, y, z, 0, 0, 0);
        this.spark = spark;
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.collidesWithWorld = false;
        if (spark) {
            this.gravityStrength = 0.25F;
            this.velocityMultiplier = 0.95F;
            this.startScale = 0.05F + this.random.nextFloat() * 0.05F;
            this.maxAge = 14 + this.random.nextInt(14);
        } else {
            this.gravityStrength = -0.01F;
            this.velocityMultiplier = 0.9F;
            this.startScale = 0.6F + this.random.nextFloat() * 0.9F;
            this.maxAge = 16 + this.random.nextInt(14);
        }
        this.scale = this.startScale;
        this.setSprite(sprites);
        this.shade(0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.shade((float) this.age / this.maxAge);
    }

    // white -> gold -> red
    private void shade(float life) {
        this.red = 1.0F;
        if (this.spark) {
            this.green = MathHelper.clamp(0.95F - life * 0.75F, 0.2F, 1.0F);
            this.blue = MathHelper.clamp(0.7F - life * 1.6F, 0.02F, 1.0F);
            this.alpha = 1.0F - life * life * life;
        } else {
            this.green = MathHelper.clamp(0.72F - life * 0.5F, 0.2F, 1.0F);
            this.blue = MathHelper.clamp(0.25F - life * 0.4F, 0.02F, 1.0F);
            this.scale = this.startScale * (0.5F + 1.5F * life);
            this.alpha = 0.55F * MathHelper.sin(MathHelper.PI * Math.min(1.0F, life * 1.15F));
        }
    }

    @Override
    public void buildGeometry(VertexConsumer consumer, Camera camera, float tickDelta) {
        if (!this.spark) {
            super.buildGeometry(consumer, camera, tickDelta);
            return;
        }
        // streak back along the velocity, facing the camera
        Vec3d cam = camera.getPos();
        Vec3d at = new Vec3d(MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cam.x,
                MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cam.y,
                MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cam.z);
        Vec3d motion = new Vec3d(this.velocityX, this.velocityY, this.velocityZ);
        double speed = motion.length();
        if (speed < 1.0E-4) {
            super.buildGeometry(consumer, camera, tickDelta);
            return;
        }
        Vec3d dir = motion.multiply(1.0 / speed);
        Vec3d side = dir.crossProduct(at);
        if (side.lengthSquared() < 1.0E-8) {
            return;
        }
        side = side.normalize().multiply(this.scale);
        Vec3d tail = at.subtract(dir.multiply(this.scale * 2.0 + speed * 2.2));
        Vec3d head = at.add(dir.multiply(this.scale));
        int light = 0xF000F0;
        float u0 = this.getMinU();
        float u1 = this.getMaxU();
        float v0 = this.getMinV();
        float v1 = this.getMaxV();
        vertex(consumer, tail.subtract(side), u0, v1, light);
        vertex(consumer, tail.add(side), u1, v1, light);
        vertex(consumer, head.add(side), u1, v0, light);
        vertex(consumer, head.subtract(side), u0, v0, light);
    }

    private void vertex(VertexConsumer consumer, Vec3d p, float u, float v, int light) {
        consumer.vertex(p.x, p.y, p.z).texture(u, v).color(this.red, this.green, this.blue, this.alpha).light(light).next();
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        // wisps aren't additive, a cloud of them goes white against the sky
        return this.spark ? ADDITIVE : ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class SparkFactory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public SparkFactory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SunGlowParticle(world, x, y, z, vx, vy, vz, this.sprites, true);
        }
    }

    public static class WispFactory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public WispFactory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new SunGlowParticle(world, x, y, z, vx, vy, vz, this.sprites, false);
        }
    }
}
