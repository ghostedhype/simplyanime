package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VfxBuffer {

    // the normal is written as it is, nothing lights these
    private static final Matrix3f UP = new Matrix3f();

    private final VertexConsumer consumer;
    private final boolean textured;
    private final boolean entity;
    private final float gain;

    private VfxBuffer(VertexConsumer consumer, VertexFormat format, boolean solid) {
        this.consumer = consumer;
        this.entity = format == DefaultVertexFormat.NEW_ENTITY;
        this.textured = this.entity || format == DefaultVertexFormat.POSITION_TEX_COLOR;
        this.gain = this.entity ? ShaderCompat.gain(solid) : 1.0F;
    }

    public static VfxBuffer of(MultiBufferSource consumers, RenderType layer) {
        return new VfxBuffer(consumers.getBuffer(layer), layer.format(), SimplyAnimeRenderTypes.isSolid(layer));
    }

    public void vertex(Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        if (this.gain != 1.0F) {
            r = Math.min(255, (int) (r * this.gain));
            g = Math.min(255, (int) (g * this.gain));
            b = Math.min(255, (int) (b * this.gain));
        }
        if (this.entity) {
            consumer.addVertex(matrix, x, y, z)
                    .setColor(r, g, b, a)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0.0F, 1.0F, 0.0F);
        } else if (this.textured) {
            consumer.addVertex(matrix, x, y, z)
                    .setColor(r, g, b, a)
                    .setUv(u, v);
        } else {
            consumer.addVertex(matrix, x, y, z)
                    .setColor(r, g, b, a);
        }
    }


    public static Vec3 rotate(Quaternionf rotation, float x, float y, float z) {
        Vector3f out = rotation.transform(new Vector3f(x, y, z));
        return new Vec3(out.x(), out.y(), out.z());
    }
}
