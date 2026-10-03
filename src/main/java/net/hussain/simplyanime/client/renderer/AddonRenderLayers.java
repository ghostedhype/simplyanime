package net.hussain.simplyanime.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

// layers for the weapon effects. with a shader pack they swap to the beacon beam program, otherwise packs
// light them like terrain and they go grey. write through VfxBuffer
// extends RenderLayer only to reach the protected render phases
public final class AddonRenderLayers extends RenderLayer {

    public static final Identifier WHITE = new Identifier("minecraft", "textures/misc/white.png");

    // additive like lightning, no depth write
    private static final RenderLayer GLOW = RenderLayer.of("simplyanime_glow",
            VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(LIGHTNING_PROGRAM)
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false));

    // plain alpha blend for the dark swirls, they need to darken what is behind them
    private static final RenderLayer SHADE = RenderLayer.of("simplyanime_shade",
            VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(COLOR_PROGRAM)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false));

    // textured versions of the two above
    private static final Function<Identifier, RenderLayer> ADDITIVE = Util.memoize(id -> RenderLayer.of("simplyanime_additive",
            VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_TEXTURE_PROGRAM)
                    .texture(new Texture(id, false, false))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false)));

    private static final Function<Identifier, RenderLayer> BLENDED = Util.memoize(id -> RenderLayer.of("simplyanime_blended",
            VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_TEXTURE_PROGRAM)
                    .texture(new Texture(id, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false)));

    // for effects written straight in the entity format with both windings, so culling stays on
    private static final Function<Identifier, RenderLayer> PACK_ENTITY = Util.memoize(id -> RenderLayer.of("simplyanime_entity_pack",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, true, true,
            MultiPhaseParameters.builder()
                    .program(ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM)
                    .texture(new Texture(id, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .lightmap(ENABLE_LIGHTMAP)
                    .writeMaskState(COLOR_MASK)
                    .build(false)));

    // smoothed versions for big glows, small textures go blocky when stretched
    private static final Function<Identifier, RenderLayer> SOFT_ADDITIVE = Util.memoize(id -> RenderLayer.of("simplyanime_soft_additive",
            VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_TEXTURE_PROGRAM)
                    .texture(new Texture(id, true, false))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false)));

    private static final Function<Identifier, RenderLayer> SOFT_BLENDED = Util.memoize(id -> RenderLayer.of("simplyanime_soft_blended",
            VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 262144, false, true,
            MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_TEXTURE_PROGRAM)
                    .texture(new Texture(id, true, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .build(false)));

    // shader pack layers, beacon beam program since every pack blooms that
    private record PackKey(Identifier texture, boolean smooth, int kind) {
    }

    private static final int ADD = 0;
    private static final int BLEND = 1;
    private static final int FORWARD_ADD = 2;
    private static final int FORWARD_BLEND = 3;
    private static final int SOLID = 4;
    private static final Set<RenderLayer> SOLIDS = new HashSet<>();

    private static final Function<PackKey, RenderLayer> PACK = Util.memoize(key -> {
        boolean forward = key.kind == FORWARD_ADD || key.kind == FORWARD_BLEND;
        boolean additive = key.kind == ADD || key.kind == FORWARD_ADD;
        MultiPhaseParameters.Builder builder = MultiPhaseParameters.builder()
                .program(forward ? ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM : BEACON_BEAM_PROGRAM)
                .texture(new Texture(key.texture, key.smooth, false))
                .transparency(key.kind == SOLID ? NO_TRANSPARENCY : additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                .writeMaskState(key.kind == SOLID ? ALL_MASK : COLOR_MASK)
                .cull(DISABLE_CULLING);
        if (forward) {
            builder.overlay(ENABLE_OVERLAY_COLOR).lightmap(ENABLE_LIGHTMAP);
        }
        return RenderLayer.of("simplyanime_pack_" + key.kind + (key.smooth ? "_soft" : ""),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, false,
                key.kind != SOLID, builder.build(false));
    });

    private static RenderLayer pack(Identifier id, boolean smooth, boolean additive) {
        int kind = additive ? ADD : BLEND;
        if (ShaderCompat.packNameHas("bliss")) {
            // bliss drops see through beacons
            kind = additive ? FORWARD_ADD : FORWARD_BLEND;
        } else if (ShaderCompat.packNameHas("photon")) {
            // photon ignores additive
            kind = BLEND;
        }
        return PACK.apply(new PackKey(id, smooth, kind));
    }

    // depth only. packs paint the sky over anything that has no depth
    private static final Function<Identifier, RenderLayer> DEPTH = Util.memoize(id -> RenderLayer.of("simplyanime_depth",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, false, false,
            MultiPhaseParameters.builder()
                    .program(ENTITY_CUTOUT_NONULL_PROGRAM)
                    .texture(new Texture(id, false, false))
                    .transparency(NO_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .writeMaskState(DEPTH_MASK)
                    .build(false)));

    private AddonRenderLayers(String name, VertexFormat format, VertexFormat.DrawMode mode, int size,
                              boolean crumbling, boolean translucent, Runnable start, Runnable end) {
        super(name, format, mode, size, crumbling, translucent, start, end);
    }

    // untextured glow
    public static RenderLayer glow() {
        return ShaderCompat.packInUse() ? pack(WHITE, false, true) : GLOW;
    }

    // untextured shading
    public static RenderLayer shade() {
        return ShaderCompat.packInUse() ? pack(WHITE, false, false) : SHADE;
    }

    public static RenderLayer additive(Identifier texture) {
        return ShaderCompat.packInUse() ? pack(texture, false, true) : ADDITIVE.apply(texture);
    }

    public static RenderLayer blended(Identifier texture) {
        return ShaderCompat.packInUse() ? pack(texture, false, false) : BLENDED.apply(texture);
    }

    public static RenderLayer softAdditive(Identifier texture) {
        return ShaderCompat.packInUse() ? pack(texture, true, true) : SOFT_ADDITIVE.apply(texture);
    }

    public static RenderLayer softBlended(Identifier texture) {
        return ShaderCompat.packInUse() ? pack(texture, true, false) : SOFT_BLENDED.apply(texture);
    }

    // opaque under a pack so it gets depth and bloom, the normal blended layer otherwise
    public static RenderLayer solid(Identifier texture) {
        if (!ShaderCompat.packInUse()) {
            return SOFT_BLENDED.apply(texture);
        }
        RenderLayer layer = PACK.apply(new PackKey(texture, true, SOLID));
        SOLIDS.add(layer);
        return layer;
    }

    public static boolean isSolid(RenderLayer layer) {
        return SOLIDS.contains(layer);
    }

    public static RenderLayer depth(Identifier texture) {
        return DEPTH.apply(texture);
    }

    // entity translucent, or the emissive one under a pack
    public static RenderLayer entityTranslucent(Identifier texture) {
        return ShaderCompat.packInUse() ? PACK_ENTITY.apply(texture) : RenderLayer.getEntityTranslucent(texture);
    }
}
