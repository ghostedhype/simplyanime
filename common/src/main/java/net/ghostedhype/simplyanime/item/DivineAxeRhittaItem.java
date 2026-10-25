package net.ghostedhype.simplyanime.item;

import net.ghostedhype.simplyanime.ability.SunPower;
import net.ghostedhype.simplyanime.client.SimplyAnimeClientCommon;
import net.ghostedhype.simplyanime.config.RhittaConfig;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.sweenus.simplyswords.api.WeaponAbilityContext;

import java.util.List;

public class DivineAxeRhittaItem extends AnimeWeaponItem {

    public DivineAxeRhittaItem(Tier tier, Properties properties) {
        super(tier, properties, "greataxe");
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (!ready(context) || CruelSunEntity.isCharging(context.actor())) {
            return false;
        }
        return context.actor() instanceof Player || context.target() != null;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return CruelSunEntity.start(context);
    }

    // shorter the stronger the sun
    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        RhittaConfig.CruelSunSettings config = RhittaConfig.get().cruelSun;
        float sun = context == null || context.actor() == null ? 0.0F
                : SunPower.fraction(SunPower.sunPower(context.world(), context.actor()));
        float multiplier = Mth.lerp(sun, config.nightCooldownMultiplier, config.noonCooldownMultiplier);
        return Math.max(config.minCooldown, Math.round(config.cooldown * multiplier));
    }

    @Override
    protected void appendAbilityTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal(""));
        this.title(tooltip, "tooltip1");
        this.line(tooltip, "tooltip2");
        this.line(tooltip, "tooltip3");
        float power = SimplyAnimeClientCommon.clientHooks.localSunPower();
        if (power > 0) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".sun", Math.round(power * 100))
                    .withStyle(sunColor(SunPower.fraction(power))));
        }
        rightClick(tooltip);
        this.line(tooltip, "tooltip4");
        this.line(tooltip, "tooltip5");
        this.line(tooltip, "tooltip6");
        appendAbilityCooldownTooltip(tooltip, stack, RhittaConfig.get().cruelSun.cooldown);
        appendAbilityManaCostTooltip(tooltip, stack);
    }

    private static ChatFormatting sunColor(float sun) {
        if (sun > 0.8F) {
            return ChatFormatting.YELLOW;
        }
        return sun > 0.35F ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY;
    }
}
