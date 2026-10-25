package net.ghostedhype.simplyanime.item;

import net.ghostedhype.simplyanime.ability.BladeSkill;
import net.ghostedhype.simplyanime.ability.BladeSkillManager;
import net.ghostedhype.simplyanime.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.sweenus.simplyswords.api.WeaponAbilityContext;

import java.util.List;

public class DarkRepulserItem extends BladeItem {

    private static final int TEAL = 0x5FF2E0;
    private static final int FROST = 0xA9DCD6;

    public DarkRepulserItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return BladeSkillManager.cooldown(BladeSkill.HORIZONTAL_SQUARE);
    }

    @Override
    protected void appendAbilityTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal(""));
        this.colored(tooltip, "tooltip1", TEAL, true);
        this.colored(tooltip, "tooltip2", FROST, false);
        rightClick(tooltip);
        this.line(tooltip, "tooltip3");
        this.line(tooltip, "tooltip4");
        this.line(tooltip, "tooltip5");
        appendAbilityCooldownTooltip(tooltip, stack, Config.weapons.dark_repulser.cooldown);
        this.dualWield(tooltip, stack, "dual_off");
        appendAbilityManaCostTooltip(tooltip, stack);
    }
}
