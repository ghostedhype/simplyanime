package net.ghostedhype.simplyanime.client.renderer;

import net.ghostedhype.simplyanime.Constants;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class SunRenderTypes {

    public static final ResourceLocation GLOW = tex("glow");
    // both tile
    public static final ResourceLocation PLASMA_A = tex("plasma_0");
    public static final ResourceLocation PLASMA_B = tex("plasma_1");
    public static final ResourceLocation FLAMES = tex("flames");
    public static final ResourceLocation RAY = tex("ray");
    public static final ResourceLocation RING = tex("ring");
    public static final ResourceLocation SMOKE = tex("smoke");

    // additive, smoothed
    public static RenderType additive(ResourceLocation texture) {
        return SimplyAnimeRenderTypes.softAdditive(texture);
    }

    // alpha blend for smoke
    public static RenderType blended(ResourceLocation texture) {
        return SimplyAnimeRenderTypes.softBlended(texture);
    }

    private static ResourceLocation tex(String name) {
        return Constants.tryId("textures/entity/cruel_sun/" + name + ".png");
    }
}
