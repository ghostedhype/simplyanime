package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.client.renderer.AddonRenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class RhittaRenderLayers {

    public static final Identifier GLOW = tex("glow");
    // both tile
    public static final Identifier PLASMA_A = tex("plasma_0");
    public static final Identifier PLASMA_B = tex("plasma_1");
    public static final Identifier FLAMES = tex("flames");
    public static final Identifier RAY = tex("ray");
    public static final Identifier RING = tex("ring");
    public static final Identifier SMOKE = tex("smoke");

    // additive, smoothed
    public static RenderLayer additive(Identifier texture) {
        return AddonRenderLayers.softAdditive(texture);
    }

    // alpha blend for smoke
    public static RenderLayer blended(Identifier texture) {
        return AddonRenderLayers.softBlended(texture);
    }

    private static Identifier tex(String name) {
        return new Identifier(SimplyAnime.MOD_ID, "textures/entity/cruel_sun/" + name + ".png");
    }
}
