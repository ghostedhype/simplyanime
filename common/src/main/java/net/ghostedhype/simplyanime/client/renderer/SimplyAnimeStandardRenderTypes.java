package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class SimplyAnimeStandardRenderTypes extends RenderType {

    private static final int BUFFER_SIZE = 262144;
    private static final ShaderStateShard TEXTURED_SHADER = new ShaderStateShard(GameRenderer::getPositionTexColorShader);

    private static final RenderType GLOW = RenderType.create(
            "simplyanime_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, BUFFER_SIZE, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static final RenderType SHADE = RenderType.create(
            "simplyanime_shade", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, BUFFER_SIZE, false, true,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(
            texture -> textured("simplyanime_additive", texture, false, true));

    private static final Function<ResourceLocation, RenderType> BLENDED = Util.memoize(
            texture -> textured("simplyanime_blended", texture, false, false));

    private static final Function<ResourceLocation, RenderType> SOFT_ADDITIVE = Util.memoize(
            texture -> textured("simplyanime_soft_additive", texture, true, true));

    private static final Function<ResourceLocation, RenderType> SOFT_BLENDED = Util.memoize(
            texture -> textured("simplyanime_soft_blended", texture, true, false));

    private static final Function<ResourceLocation, RenderType> DEPTH = Util.memoize(
            texture -> RenderType.create(
                    "simplyanime_depth", DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, BUFFER_SIZE, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(DEPTH_WRITE)
                            .createCompositeState(false)));

    private SimplyAnimeStandardRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                                int size, boolean crumbling, boolean sortOnUpload,
                                Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sortOnUpload, setup, clear);
    }

    private static RenderType textured(String name, ResourceLocation texture,
                                       boolean smooth, boolean additive) {
        return RenderType.create(
                name, DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, BUFFER_SIZE, false, true,
                CompositeState.builder()
                        .setShaderState(TEXTURED_SHADER)
                        .setTextureState(new TextureStateShard(texture, smooth, false))
                        .setTransparencyState(additive
                                ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    static RenderType glow() {
        return GLOW;
    }

    static RenderType shade() {
        return SHADE;
    }

    static RenderType additive(ResourceLocation texture) {
        return ADDITIVE.apply(texture);
    }

    static RenderType blended(ResourceLocation texture) {
        return BLENDED.apply(texture);
    }

    static RenderType softAdditive(ResourceLocation texture) {
        return SOFT_ADDITIVE.apply(texture);
    }

    static RenderType softBlended(ResourceLocation texture) {
        return SOFT_BLENDED.apply(texture);
    }

    static RenderType depth(ResourceLocation texture) {
        return DEPTH.apply(texture);
    }
}
