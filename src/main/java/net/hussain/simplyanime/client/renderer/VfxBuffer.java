package net.hussain.simplyanime.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

// the shader pack layers are entity format so they want overlay, light and a normal too
public final class VfxBuffer {

    private final VertexConsumer consumer;
    private final boolean textured;
    private final boolean entity;
    private final float gain;

    private VfxBuffer(VertexConsumer consumer, VertexFormat format, boolean solid) {
        this.consumer = consumer;
        this.entity = format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
        this.textured = this.entity || format == VertexFormats.POSITION_COLOR_TEXTURE;
        this.gain = this.entity ? ShaderCompat.gain(solid) : 1.0F;
    }

    public static VfxBuffer of(VertexConsumerProvider consumers, RenderLayer layer) {
        return new VfxBuffer(consumers.getBuffer(layer), layer.getVertexFormat(), AddonRenderLayers.isSolid(layer));
    }

    public void vertex(Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        if (this.gain != 1.0F) {
            r = Math.min(255, (int) (r * this.gain));
            g = Math.min(255, (int) (g * this.gain));
            b = Math.min(255, (int) (b * this.gain));
        }
        this.consumer.vertex(matrix, x, y, z).color(r, g, b, a);
        if (this.textured) {
            this.consumer.texture(u, v);
        }
        if (this.entity) {
            this.consumer.overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(0.0F, 1.0F, 0.0F);
        }
        this.consumer.next();
    }
}
