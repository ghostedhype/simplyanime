package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

final class ShaderPackRenderTypes extends RenderType {

    private static final int BUFFER_SIZE = 262144;

    private enum Kind {
        ADD(0),
        BLEND(1),
        FORWARD_ADD(2),
        FORWARD_BLEND(3),
        SOLID(4);

        private final int id;

        Kind(int id) {
            this.id = id;
        }

        boolean isForward() {
            return this == FORWARD_ADD || this == FORWARD_BLEND;
        }

        boolean isAdditive() {
            return this == ADD || this == FORWARD_ADD;
        }
    }

    private record PackKey(ResourceLocation texture, boolean smooth, Kind kind) {
    }

    private static final Set<RenderType> SOLIDS = new HashSet<>();
    private static final Function<PackKey, RenderType> PACK =
            Util.memoize(ShaderPackRenderTypes::createPackType);

    // Culling remains enabled: the caller supplies both windings.
    private static final Function<ResourceLocation, RenderType> ENTITY = Util.memoize(
            texture -> RenderType.create(
                    "simplyanime_entity_pack", DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, BUFFER_SIZE, true, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setOverlayState(OVERLAY)
                            .setLightmapState(LIGHTMAP)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    private ShaderPackRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                                  int size, boolean crumbling, boolean sortOnUpload,
                                  Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sortOnUpload, setup, clear);
    }

    private static RenderType createPackType(PackKey key) {
        Kind kind = key.kind();
        boolean forward = kind.isForward();
        boolean solid = kind == Kind.SOLID;

        CompositeState.CompositeStateBuilder builder = CompositeState.builder()
                .setShaderState(forward
                        ? RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER
                        : RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(key.texture(), key.smooth(), false))
                .setTransparencyState(solid ? NO_TRANSPARENCY
                        : kind.isAdditive() ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(solid ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                .setCullState(NO_CULL);

        if (forward) {
            builder.setOverlayState(OVERLAY).setLightmapState(LIGHTMAP);
        }

        return RenderType.create(
                "simplyanime_pack_" + kind.id + (key.smooth() ? "_soft" : ""),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                BUFFER_SIZE, false, !solid, builder.createCompositeState(false));
    }

    static RenderType effect(ResourceLocation texture, boolean smooth, boolean additive) {
        Kind kind = switch (ShaderCompat.renderPath()) {
            case FORWARD_EMISSIVE -> additive ? Kind.FORWARD_ADD : Kind.FORWARD_BLEND;
            case BEACON_BLENDED -> Kind.BLEND;
            case BEACON -> additive ? Kind.ADD : Kind.BLEND;
        };
        return PACK.apply(new PackKey(texture, smooth, kind));
    }

    static RenderType solid(ResourceLocation texture) {
        RenderType type = PACK.apply(new PackKey(texture, true, Kind.SOLID));
        SOLIDS.add(type);
        return type;
    }

    static boolean isSolid(RenderType type) {
        return SOLIDS.contains(type);
    }

    static RenderType entity(ResourceLocation texture) {
        return ENTITY.apply(texture);
    }
}