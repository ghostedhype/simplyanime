package net.ghostedhype.simplyanime.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SunGlowParticle extends TextureSheetParticle {

    private final boolean spark;
    private final float startScale;

    private SunGlowParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ,
                            SpriteSet sprites, boolean spark) {
        super(world, x, y, z, 0, 0, 0);
        this.spark = spark;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.hasPhysics = false;
        if (spark) {
            this.gravity = 0.25F;
            this.friction = 0.95F;
            this.startScale = 0.05F + this.random.nextFloat() * 0.05F;
            this.lifetime = 14 + this.random.nextInt(14);
        } else {
            this.gravity = -0.01F;
            this.friction = 0.9F;
            this.startScale = 0.6F + this.random.nextFloat() * 0.9F;
            this.lifetime = 16 + this.random.nextInt(14);
        }
        this.quadSize = this.startScale;
        this.pickSprite(sprites);
        this.shade(0.0F);
    }

    public static SunGlowParticle spark(ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed,
                                        SpriteSet spriteSet) {
        return new SunGlowParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, true);
    }

    public static SunGlowParticle wisp(ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       SpriteSet spriteSet) {
        return new SunGlowParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.shade((float) this.age / this.lifetime);
    }

    // white -> gold -> red
    private void shade(float life) {
        this.rCol = 1.0F;
        if (this.spark) {
            this.gCol = Mth.clamp(0.95F - life * 0.75F, 0.2F, 1.0F);
            this.bCol = Mth.clamp(0.7F - life * 1.6F, 0.02F, 1.0F);
            this.alpha = 1.0F - life * life * life;
        } else {
            this.gCol = Mth.clamp(0.72F - life * 0.5F, 0.2F, 1.0F);
            this.bCol = Mth.clamp(0.25F - life * 0.4F, 0.02F, 1.0F);
            this.quadSize = this.startScale * (0.5F + 1.5F * life);
            this.alpha = 0.55F * Mth.sin(Mth.PI * Math.min(1.0F, life * 1.15F));
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        if (!this.spark) {
            super.render(consumer, camera, partialTick);
            return;
        }
        // streak back along the velocity, facing the camera
        Vec3 cameraPositionVec = camera.getPosition();
        Vec3 cameraPositionOffsetVec = new Vec3(Mth.lerp(partialTick, this.xo, this.x) - cameraPositionVec.x,
                Mth.lerp(partialTick, this.yo, this.y) - cameraPositionVec.y,
                Mth.lerp(partialTick, this.zo, this.z) - cameraPositionVec.z);
        Vec3 motion = new Vec3(this.xd, this.yd, this.zd);
        double speed = motion.length();
        if (speed < 1.0E-4) {
            super.render(consumer, camera, partialTick);
            return;
        }
        Vec3 dir = motion.scale(1.0 / speed);
        Vec3 side = dir.cross(cameraPositionOffsetVec);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(this.quadSize);
        Vec3 tail = cameraPositionOffsetVec.subtract(dir.scale(this.quadSize * 2.0 + speed * 2.2));
        Vec3 head = cameraPositionOffsetVec.add(dir.scale(this.quadSize));
        int light = 0xF000F0;
        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        vertex(consumer, tail.subtract(side), minU, maxV, light);
        vertex(consumer, tail.add(side), maxU, maxV, light);
        vertex(consumer, head.add(side), maxU, minV, light);
        vertex(consumer, head.subtract(side), minU, minV, light);
    }

    private void vertex(VertexConsumer consumer, Vec3 vec3, float u, float v, int light) {
        consumer.addVertex((float) vec3.x, (float) vec3.y, (float) vec3.z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.spark ? (tesselator, textureManager) -> {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        } : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
