package net.ghostedhype.simplyanime.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;

import java.util.ArrayList;
import java.util.List;

public abstract class WeaponItemBase extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    protected WeaponItemBase(Tier tier, Properties properties) {
        super(tier, properties);
    }

    protected abstract void appendAbilityTooltip(ItemStack stack, List<Component> tooltip);

    @Override
    protected InteractionResultHolder<ItemStack> useUniqueWeapon(Level level, Player user, InteractionHand hand) {
        return useFromDefaultInput(level, user, hand);
    }

    @Override
    protected void appendUniqueWeaponTooltip(ItemStack stack, Item.TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag flag) {
        this.appendAbilityTooltip(stack, tooltip);
        appendSharedUniqueWeaponTooltip(stack, tooltipContext, tooltip, flag);
        SimplySwordsClientAPI.appendSpellScaleTooltip(tooltip, "arcane");
    }

    @Override
    protected void generateDynamicTooltip(ItemStack stack, Item.TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag flag) {
        List<Component> sockets = new ArrayList<>();
        SimplySwordsAPI.appendTooltipGemSocketLogic(stack, tooltipContext, sockets, flag);
        while (!sockets.isEmpty() && sockets.get(0).getString().isEmpty()) {
            sockets.remove(0);
        }
        if (!sockets.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.addAll(sockets);
        }
    }
}
