package net.hussain.simplyanime.item;

import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.hussain.simplyanime.client.EnumaElishFirstPerson;
import net.hussain.simplyanime.config.Config;
import net.hussain.simplyanime.world.EnumaElishAbilityManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnumaElishItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public EnumaElishItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    protected TypedActionResult<ItemStack> useUniqueWeapon(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null || !context.actor().isAlive()
                || context.stack() == null || context.stack().isEmpty()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || EnumaElishAbilityManager.isBusy(context.world(), context.actor())) {
            return false;
        }
        // mobs only let it go when they have something to point it at
        return context.actor() instanceof PlayerEntity || context.target() != null;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return EnumaElishAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.weapons.enuma_elish.cooldown;
    }

    @Override
    public String getTooltipWeaponType(ItemStack stack) {
        return "claymore";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(EnumaElishFirstPerson.INSTANCE);
    }

    @Override
    protected void appendUniqueWeaponTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyanime.enuma_elish.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyanime.enuma_elish.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyanime.enuma_elish.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.enuma_elish.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.enuma_elish.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.weapons.enuma_elish.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        appendSharedUniqueWeaponTooltip(itemStack, world, tooltip, tooltipContext);
        SimplySwordsClientAPI.appendSpellScaleTooltip(tooltip, "arcane");
    }

    // same as the spear, no oracle index pages yet so only the gem sockets
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

        public float damageModifier = 5.0f;
        public float attackSpeed = -2.9f;
        public boolean craftable = true;
        public boolean lootable = true;

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1800;

        // charge
        @ValidatedInt.Restrict(min = 1)
        public int chargeDuration = 80;
        @ValidatedInt.Restrict(min = -1, max = 9)
        public int chargeSlowness = 3;
        @ValidatedInt.Restrict(min = 0)
        public int shoutLead = 40;

        // beam
        @ValidatedInt.Restrict(min = 1)
        public int beamDuration = 120;
        @ValidatedDouble.Restrict(min = 4.0, max = 512.0)
        public double beamRange = 160.0;
        @ValidatedDouble.Restrict(min = 0.5, max = 32.0)
        public double beamRadius = 8.0;
        // off by default, the beam goes straight through terrain. Blocks are never broken either way
        public boolean stopAtBlocks = false;
        @ValidatedDouble.Restrict(min = 0.0, max = 32.0)
        public double impactRadius = 5.0;
        // the opening blast, hits everything in the beam once
        @ValidatedFloat.Restrict(min = 0f)
        public float blastDamageScaling = 9.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float blastSpellScaling = 25.0f;
        // damage over time for anything standing in the beam afterwards
        @ValidatedFloat.Restrict(min = 0f)
        public float tickDamageScaling = 0.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float tickSpellScaling = 1.5f;
        @ValidatedInt.Restrict(min = 1)
        public int tickInterval = 5;
        @ValidatedDouble.Restrict(min = 0.0, max = 5.0)
        public double beamKnockback = 1.2;

        // sounds, volume above 1 only widens how far away it can be heard
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float chargeVolume = 3.0f;
        @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
        public float chargePitch = 1.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float shoutVolume = 5.0f;
        @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
        public float shoutPitch = 1.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float releaseVolume = 5.0f;
        @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
        public float releasePitch = 1.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float beamVolume = 6.0f;
        @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
        public float beamPitch = 1.0f;
        @ValidatedFloat.Restrict(min = 0f, max = 16f)
        public float impactVolume = 6.0f;
        @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
        public float impactPitch = 1.0f;
    }
}
