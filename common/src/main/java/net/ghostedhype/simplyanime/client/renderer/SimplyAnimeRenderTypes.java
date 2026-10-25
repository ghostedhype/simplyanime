package net.ghostedhype.simplyanime.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class SimplyAnimeRenderTypes {
    public static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    private SimplyAnimeRenderTypes() {
    }

    public static RenderType glow() {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(WHITE, false, true)
                : SimplyAnimeStandardRenderTypes.glow();
    }

    public static RenderType shade() {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(WHITE, false, false)
                : SimplyAnimeStandardRenderTypes.shade();
    }

    public static RenderType additive(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(texture, false, true)
                : SimplyAnimeStandardRenderTypes.additive(texture);
    }

    public static RenderType blended(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(texture, false, false)
                : SimplyAnimeStandardRenderTypes.blended(texture);
    }

    public static RenderType softAdditive(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(texture, true, true)
                : SimplyAnimeStandardRenderTypes.softAdditive(texture);
    }

    public static RenderType softBlended(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.effect(texture, true, false)
                : SimplyAnimeStandardRenderTypes.softBlended(texture);
    }

    public static RenderType solid(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.solid(texture)
                : SimplyAnimeStandardRenderTypes.softBlended(texture);
    }

    public static boolean isSolid(RenderType type) {
        return ShaderPackRenderTypes.isSolid(type);
    }

    public static RenderType depth(ResourceLocation texture) {
        return SimplyAnimeStandardRenderTypes.depth(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return ShaderCompat.packInUse()
                ? ShaderPackRenderTypes.entity(texture)
                : RenderType.entityTranslucent(texture);
    }
}
