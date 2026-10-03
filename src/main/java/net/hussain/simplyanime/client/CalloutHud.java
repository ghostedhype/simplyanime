package net.hussain.simplyanime.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hussain.simplyanime.callout.Callout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

// ability name above the hotbar, small on purpose
public class CalloutHud implements IGuiOverlay {

    public static final CalloutHud INSTANCE = new CalloutHud();

    private static final float DURATION = 56.0F;
    private static final float POP = 5.0F;
    private static final float FADE = 14.0F;
    private static final Style STYLE = Style.EMPTY.withBold(true).withItalic(true);

    private Callout current;
    private long startedAt;

    public void show(Callout callout) {
        this.current = callout;
        this.startedAt = Util.getMeasuringTimeMs();
    }

    @Override
    public void render(ForgeGui gui, DrawContext context, float partialTick, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.current == null || client.options.hudHidden) {
            return;
        }
        float age = (Util.getMeasuringTimeMs() - this.startedAt) / 50.0F;
        if (age >= DURATION) {
            this.current = null;
            return;
        }
        float alpha = Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (DURATION - age) / FADE);
        // overshoot then settle
        float pop = 1.0F - Math.min(1.0F, age / POP);
        float scale = 1.15F + 0.5F * pop * pop;

        TextRenderer font = client.textRenderer;
        String text = I18n.translate(this.current.key).toUpperCase();
        int textWidth = font.getWidth(Text.literal(text).setStyle(STYLE)) + (text.length() - 1);

        RenderSystem.enableBlend();
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F, height - 74.0F, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        if (this.current.look == Callout.Look.FIRE) {
            fire(context, font, text, textWidth, age, alpha);
        } else {
            rupture(context, font, text, textWidth, age, alpha);
        }
        context.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private static void fire(DrawContext context, TextRenderer font, String text, int textWidth, float age, float alpha) {
        rules(context, textWidth, alpha, 0xFF9A1F, 0xE8421A);
        // embers
        for (int i = 0; i < 14; i++) {
            float life = MathHelper.fractionalPart(age / 22.0F + hash(i, 1));
            float x = (hash(i, 2) - 0.5F) * (textWidth + 10) + MathHelper.sin(age * 0.2F + i) * 1.5F;
            float y = 3.0F - life * 15.0F;
            int ember = color(mix(0xFFE27A, 0xE8421A, life), alpha * (1.0F - life) * 0.9F);
            if (ember != 0) {
                context.fill((int) x, (int) y, (int) x + 1, (int) y + 1, ember);
            }
        }
        float x = -textWidth / 2.0F;
        for (int i = 0; i < text.length(); i++) {
            Text glyph = Text.literal(String.valueOf(text.charAt(i))).setStyle(STYLE);
            // heat shimmer
            float y = -4.0F + MathHelper.sin(age * 0.35F + i * 0.9F) * 0.9F;
            float flame = 0.5F + 0.5F * MathHelper.sin(age * 0.3F - i * 0.7F);
            int body = flame < 0.5F ? mix(0xE8421A, 0xFF9A1F, flame * 2.0F) : mix(0xFF9A1F, 0xFFF27A, flame * 2.0F - 1.0F);
            glow(context, font, glyph, x, y, color(0xFF5A14, alpha * 0.28F));
            draw(context, font, glyph, x, y + 1.0F, color(0x5A1200, alpha * 0.9F));
            draw(context, font, glyph, x, y, color(body, alpha));
            x += font.getWidth(glyph) + 1;
        }
    }

    private static void rupture(DrawContext context, TextRenderer font, String text, int textWidth, float age, float alpha) {
        rules(context, textWidth, alpha, 0xD3122A, 0x3A0008);
        // wind streaks
        for (int i = 0; i < 6; i++) {
            float life = MathHelper.fractionalPart(age / 9.0F + hash(i, 3));
            int length = 6 + (int) (hash(i, 4) * 12);
            int x = (int) (-textWidth / 2.0F - 14 + life * (textWidth + 28));
            int y = -6 + (int) (hash(i, 5) * 12);
            int streak = color(0xFF4A3D, alpha * MathHelper.sin(life * MathHelper.PI) * 0.55F);
            if (streak != 0) {
                context.fill(x, y, x + length, y + 1, streak);
            }
        }
        float shake = Math.max(0.35F, 2.2F - age * 0.25F);
        float x = -textWidth / 2.0F + MathHelper.sin(age * 2.9F) * shake;
        for (int i = 0; i < text.length(); i++) {
            Text glyph = Text.literal(String.valueOf(text.charAt(i))).setStyle(STYLE);
            float y = -4.0F + MathHelper.sin(age * 3.7F + i * 2.1F) * shake * 0.35F;
            float pulse = 0.5F + 0.5F * MathHelper.sin(age * 0.45F + i * 0.5F);
            glow(context, font, glyph, x, y, color(0xB00018, alpha * 0.3F));
            // split copy, dark one way and red the other
            draw(context, font, glyph, x + 1.5F, y, color(0x14000A, alpha * 0.85F));
            draw(context, font, glyph, x - 1.0F, y, color(0x7A0010, alpha * 0.7F));
            draw(context, font, glyph, x, y, color(mix(0xD3122A, 0xFF5A48, pulse), alpha));
            x += font.getWidth(glyph) + 1;
        }
    }

    private static void rules(DrawContext context, int textWidth, float alpha, int near, int far) {
        int half = textWidth / 2 + 5;
        int length = 22;
        for (int i = 0; i < length; i++) {
            float fade = 1.0F - i / (float) length;
            int line = color(mix(far, near, fade), alpha * fade * 0.85F);
            if (line != 0) {
                context.fill(half + i, 0, half + i + 1, 1, line);
                context.fill(-half - i - 1, 0, -half - i, 1, line);
            }
        }
    }

    private static void glow(DrawContext context, TextRenderer font, Text glyph, float x, float y, int color) {
        draw(context, font, glyph, x - 1.0F, y, color);
        draw(context, font, glyph, x + 1.0F, y, color);
        draw(context, font, glyph, x, y - 1.0F, color);
        draw(context, font, glyph, x, y + 1.0F, color);
    }

    private static void draw(DrawContext context, TextRenderer font, Text glyph, float x, float y, int color) {
        if (color == 0) {
            return;
        }
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0F);
        context.drawText(font, glyph, 0, 0, color, false);
        context.getMatrices().pop();
    }

    // near zero alpha text draws fully solid, skip it
    private static int color(int rgb, float alpha) {
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return a < 8 ? 0 : a << 24 | rgb;
    }

    private static int mix(int from, int to, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int r = (int) MathHelper.lerp(t, from >> 16 & 255, to >> 16 & 255);
        int g = (int) MathHelper.lerp(t, from >> 8 & 255, to >> 8 & 255);
        int b = (int) MathHelper.lerp(t, from & 255, to & 255);
        return r << 16 | g << 8 | b;
    }

    private static float hash(int index, int salt) {
        return MathHelper.fractionalPart(MathHelper.sin(index * 12.9898F + salt * 78.233F) * 43758.547F);
    }
}
