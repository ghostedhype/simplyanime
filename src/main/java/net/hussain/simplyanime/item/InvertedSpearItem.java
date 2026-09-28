package net.hussain.simplyanime.item;

import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.world.InvertedSpearAbilityManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;

public class InvertedSpearItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public InvertedSpearItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    protected TypedActionResult<ItemStack> useUniqueWeapon(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !InvertedSpearAbilityManager.isBusy(context.world(), context.actor());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (InvertedSpearAbilityManager.wantsTwinStrike(context)) {
            return InvertedSpearAbilityManager.startTwinStrike(context);
        }
        return InvertedSpearAbilityManager.startSlashField(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        return InvertedSpearAbilityManager.wantsTwinStrike(context) ? settings.strikeCooldown : settings.fieldCooldown;
    }

    @Override
    public String getTooltipWeaponType(ItemStack stack) {
        return "sai";
    }

    @Override
    protected void appendUniqueWeaponTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        EffectSettings settings = Config.weapons.inverted_spear_of_heaven;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, settings.strikeCooldown);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.sneak_right_click").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.inverted_spear_of_heaven.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, settings.fieldCooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        appendSharedUniqueWeaponTooltip(itemStack, world, tooltip, tooltipContext);
        SimplySwordsClientAPI.appendSpellScaleTooltip(tooltip, "arcane");
    }

    // no oracle index pages for the addon yet, so skip the links but keep the gem sockets
    @Override
    protected void generateDynamicTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        List<Text> sockets = new ArrayList<>();
        SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, world, sockets, tooltipContext);
        while (!sockets.isEmpty() && sockets.get(0).getString().isEmpty()) {
            sockets.remove(0);
        }
        if (!sockets.isEmpty()) {
            tooltip.add(Text.literal(""));
            tooltip.addAll(sockets);
        }
    }

    public static class EffectSettings extends ConfigSection {

        public float damageModifier = 3.0f;
        public float attackSpeed = -1.8f;

        // Heavenly Slash Field (sneak + right click)
        @ValidatedInt.Restrict(min = 0)
        public int fieldCooldown = 240;
        @ValidatedInt.Restrict(min = 0)
        public int fieldWindup = 5;
        @ValidatedInt.Restrict(min = 1)
        public int fieldSpinDuration = 50;
        @ValidatedInt.Restrict(min = 1)
        public int fieldHitFrequency = 5;
        @ValidatedDouble.Restrict(min = 1.0)
        public double fieldRadius = 4.5;
        @ValidatedFloat.Restrict(min = 0f)
        public float fieldDamageScaling = 0.22f;
        @ValidatedFloat.Restrict(min = 0f)
        public float fieldSpellScaling = 1.0f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double lockOnRange = 9.0;
        @ValidatedInt.Restrict(min = 1)
        public int flingDuration = 5;
        @ValidatedFloat.Restrict(min = 0f)
        public float payoffDamageScaling = 1.1f;
        @ValidatedFloat.Restrict(min = 0f)
        public float payoffSpellScaling = 5.0f;
        @ValidatedInt.Restrict(min = 1)
        public int retractDuration = 8;
        @ValidatedFloat.Restrict(min = 0f)
        public float retractDamageScaling = 0.78f;
        @ValidatedFloat.Restrict(min = 0f)
        public float retractSpellScaling = 3.5f;

        // Twin Heaven Strike (right click)
        @ValidatedInt.Restrict(min = 0)
        public int strikeCooldown = 160;
        @ValidatedInt.Restrict(min = 1)
        public int dashWindup = 5;
        @ValidatedDouble.Restrict(min = 1.0)
        public double dashDistance = 6.0;
        @ValidatedInt.Restrict(min = 1)
        public int dashDuration = 5;
        @ValidatedDouble.Restrict(min = 0.5)
        public double strikeRadius = 2.5;
        @ValidatedFloat.Restrict(min = 0f)
        public float firstStrikeDamageScaling = 1.33f;
        @ValidatedFloat.Restrict(min = 0f)
        public float firstStrikeSpellScaling = 6.1f;
        @ValidatedDouble.Restrict(min = 0.0)
        public double firstStrikeLaunch = 0.45;
        @ValidatedInt.Restrict(min = 1)
        public int secondStrikeDelay = 7;
        @ValidatedFloat.Restrict(min = 0f)
        public float secondStrikeDamageScaling = 1.33f;
        @ValidatedFloat.Restrict(min = 0f)
        public float secondStrikeSpellScaling = 6.1f;
        @ValidatedInt.Restrict(min = 0)
        public int recoveryDuration = 10;
    }
}
