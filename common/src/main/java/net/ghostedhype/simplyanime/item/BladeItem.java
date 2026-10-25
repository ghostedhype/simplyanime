package net.ghostedhype.simplyanime.item;

import net.ghostedhype.simplyanime.ability.BladeSkillManager;
import net.ghostedhype.simplyanime.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public abstract class BladeItem extends AnimeWeaponItem {

    private static final int[] STARLIGHT = {0xFF3B4E, 0xFFFFFF, 0x5FF2E0};
    private static final String DUAL = "item.simplyanime.dual_blades.";

    protected BladeItem(Tier tier, Properties properties) {
        super(tier, properties, "longsword");
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (!ready(context) || BladeSkillManager.isBusy(context.actor())
                || BladeSkillManager.skillFor(context.actor(), context.hand()) == null) {
            return false;
        }
        return context.actor() instanceof Player || context.target() != null;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return BladeSkillManager.start(context);
    }

    protected void colored(List<Component> tooltip, String key, int color, boolean bold) {
        tooltip.add(Component.translatable(this.getDescriptionId() + "." + key)
                .setStyle(Style.EMPTY.withColor(color).withBold(bold)));
    }

    protected void dualWield(List<Component> tooltip, ItemStack stack, String handKey) {
        tooltip.add(Component.literal(""));
        tooltip.add(gradient(Component.translatable(DUAL + "title").getString(), STARLIGHT));
        tooltip.add(Component.translatable(DUAL + handKey).setStyle(Styles.RIGHT_CLICK));
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(DUAL + "tooltip" + i).setStyle(Styles.TEXT));
        }
        appendAbilityCooldownTooltip(tooltip, stack, Config.weapons.starburst_stream.cooldown);
    }

    private static Component gradient(String text, int[] colors) {
        MutableComponent out = Component.empty();
        int last = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            float colorPosition = (float) i / last * (colors.length - 1);
            int from = Math.min(colors.length - 2, (int) colorPosition);
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(mix(colors[from], colors[from + 1], colorPosition - from)).withBold(true)));
        }
        return out;
    }

    private static int mix(int from, int to, float tickProgress) {
        int r = (int) ((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * tickProgress);
        int g = (int) ((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * tickProgress);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * tickProgress);
        return r << 16 | g << 8 | b;
    }
}
