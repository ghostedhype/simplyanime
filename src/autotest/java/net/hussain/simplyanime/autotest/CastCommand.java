package net.hussain.simplyanime.autotest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.hussain.simplyanime.registry.ItemsRegistry;
import net.hussain.simplyanime.rhitta.RhittaRegistry;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.hussain.simplyanime.world.EnumaElishAbilityManager;
import net.hussain.simplyanime.world.InvertedSpearAbilityManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;

import java.util.List;

// /satest <sun|ea|field|strike>, casts at the nearest iron golem
public class CastCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("satest")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("what", StringArgumentType.word()).executes(context -> {
                    ServerCommandSource source = context.getSource();
                    if (!(source.getEntity() instanceof LivingEntity caster)) {
                        source.sendError(Text.literal("Needs a living caster"));
                        return 0;
                    }
                    return cast(source.getWorld(), caster, StringArgumentType.getString(context, "what")) ? 1 : 0;
                })));
    }

    private static boolean cast(ServerWorld world, LivingEntity caster, String what) {
        List<IronGolemEntity> golems = world.getEntitiesByClass(IronGolemEntity.class, caster.getBoundingBox().expand(64), e -> true);
        LivingEntity target = golems.isEmpty() ? null : golems.get(0);
        for (IronGolemEntity golem : golems) {
            if (golem.squaredDistanceTo(caster) < target.squaredDistanceTo(caster)) {
                target = golem;
            }
        }
        switch (what) {
            case "sun", "cast" -> {
                ItemStack stack = new ItemStack(RhittaRegistry.DIVINE_AXE_RHITTA.get());
                return CruelSunEntity.start(world, caster, target, stack, Hand.MAIN_HAND);
            }
            case "ea" -> {
                return EnumaElishAbilityManager.start(context(world, caster, target, new ItemStack(ItemsRegistry.ENUMA_ELISH.get())));
            }
            case "field" -> {
                return InvertedSpearAbilityManager.startSlashField(context(world, caster, target,
                        new ItemStack(ItemsRegistry.INVERTED_SPEAR_OF_HEAVEN.get())));
            }
            case "strike" -> {
                return InvertedSpearAbilityManager.startTwinStrike(context(world, caster, target,
                        new ItemStack(ItemsRegistry.INVERTED_SPEAR_OF_HEAVEN.get())));
            }
            default -> {
                return false;
            }
        }
    }

    private static WeaponAbilityContext context(ServerWorld world, LivingEntity caster, LivingEntity target, ItemStack stack) {
        return WeaponAbilityContext.of(world, stack, caster, null, target, Hand.MAIN_HAND, WeaponAbilityActivationSource.values()[0]);
    }
}
