package net.hussain.simplyanime.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

// extends RenderLayer only to reach the protected render phases
public final class AddonRenderLayers extends RenderLayer {

    // additive, the same blend lightning uses. No depth write so stacked shells all show
    public static final RenderLayer GLOW = RenderLayer.of("simplyanime_glow",
            VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(LIGHTNING_PROGRAM)
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false));

    // plain alpha blend for the dark swirls, they need to darken what is behind them
    public static final RenderLayer SHADE = RenderLayer.of("simplyanime_shade",
            VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(COLOR_PROGRAM)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false));

    private AddonRenderLayers(String name, VertexFormat format, VertexFormat.DrawMode mode, int size,
                              boolean crumbling, boolean translucent, Runnable start, Runnable end) {
        super(name, format, mode, size, crumbling, translucent, start, end);
    }
}
