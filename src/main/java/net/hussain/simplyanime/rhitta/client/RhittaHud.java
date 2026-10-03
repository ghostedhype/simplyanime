package net.hussain.simplyanime.rhitta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hussain.simplyanime.SimplyAnime;
import net.hussain.simplyanime.rhitta.RhittaGrowth;
import net.hussain.simplyanime.rhitta.SunPower;
import net.hussain.simplyanime.rhitta.config.RhittaClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

// little sun in the corner while Rhitta is held, bigger and brighter the closer it is to noon
public class RhittaHud implements IGuiOverlay {

    public static final RhittaHud INSTANCE = new RhittaHud();

    private static final Identifier SUN = new Identifier(SimplyAnime.MOD_ID, "textures/gui/rhitta_sun.png");
    private static final int SIZE = 16;

    @Override
    public void render(ForgeGui gui, DrawContext context, float partialTick, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        RhittaClientConfig cfg = RhittaClientConfig.get();
        if (!cfg.hudEnabled || client.player == null || client.world == null || client.options.hudHidden
                || !RhittaGrowth.holding(client.player)) {
            return;
        }
        float power = SunPower.sunPower(client.world, client.player);
        float sun = SunPower.fraction(power);
        float time = client.player.age + partialTick;
        float scale = 1.0F + 0.6F * sun + 0.04F * sun * MathHelper.sin(time * 0.15F);
        int box = 30;

        boolean right = cfg.hudCorner.get() == RhittaClientConfig.HudCorner.TOP_RIGHT
                || cfg.hudCorner.get() == RhittaClientConfig.HudCorner.BOTTOM_RIGHT;
        boolean bottom = cfg.hudCorner.get() == RhittaClientConfig.HudCorner.BOTTOM_LEFT
                || cfg.hudCorner.get() == RhittaClientConfig.HudCorner.BOTTOM_RIGHT;
        float cx = right ? width - cfg.hudOffsetX - box / 2.0F : cfg.hudOffsetX + box / 2.0F;
        float cy = bottom ? height - cfg.hudOffsetY - box / 2.0F - 10 : cfg.hudOffsetY + box / 2.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.45F + 0.55F * sun);
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTexture(SUN, -SIZE / 2, -SIZE / 2, 0, 0, SIZE, SIZE, SIZE, SIZE);
        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        String text = Math.round(power * 100) + "%";
        int color = sun > 0.8F ? 0xFFF97A : sun > 0.35F ? 0xEBB316 : 0x8B7B60;
        context.drawTextWithShadow(client.textRenderer, text, (int) cx - client.textRenderer.getWidth(text) / 2,
                (int) cy + 14, color);
    }
}
