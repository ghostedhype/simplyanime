package net.hussain.simplyanime.rhitta;

import net.hussain.simplyanime.rhitta.client.RhittaClientHooks;
import net.hussain.simplyanime.rhitta.client.RhittaFirstPerson;
import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
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

public class DivineAxeRhittaItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public DivineAxeRhittaItem(ToolMaterial toolMaterial, Settings settings) {
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
                || CruelSunEntity.isCharging(context.actor())) {
            return false;
        }
        return context.actor() instanceof PlayerEntity || context.target() != null;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return CruelSunEntity.start(context);
    }

    // shorter the stronger the sun
    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        RhittaConfig.CruelSunSettings cfg = RhittaConfig.get().cruelSun;
        float sun = context == null || context.actor() == null ? 0.0F
                : SunPower.fraction(SunPower.sunPower(context.world(), context.actor()));
        float multiplier = MathHelper.lerp(sun, cfg.nightCooldownMultiplier, cfg.noonCooldownMultiplier);
        return Math.max(cfg.minCooldown, Math.round(cfg.cooldown * multiplier));
    }

    @Override
    public String getTooltipWeaponType(ItemStack stack) {
        return "greataxe";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(RhittaFirstPerson.INSTANCE);
    }

    @Override
    protected void appendUniqueWeaponTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip3").setStyle(Styles.TEXT));
        float power = RhittaClientHooks.localSunPower();
        if (power > 0) {
            tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.sun", Math.round(power * 100))
                    .formatted(sunColor(SunPower.fraction(power))));
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyanime.divine_axe_rhitta.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, RhittaConfig.get().cruelSun.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        appendSharedUniqueWeaponTooltip(itemStack, world, tooltip, tooltipContext);
        SimplySwordsClientAPI.appendSpellScaleTooltip(tooltip, "arcane");
    }

    private static Formatting sunColor(float sun) {
        if (sun > 0.8F) {
            return Formatting.YELLOW;
        }
        return sun > 0.35F ? Formatting.GOLD : Formatting.DARK_GRAY;
    }

    // no oracle index pages yet, so only the gem sockets
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
}
