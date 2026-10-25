package net.ghostedhype.simplyanime.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

public class SimplyAnimeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("SimplyAnime")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cooldown")
                        .then(Commands.literal("reset")
                                .executes(context -> reset(context, List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> reset(context, EntityArgument.getPlayers(context, "targets")))))));
        dispatcher.register(Commands.literal("simplyanime")
                .requires(source -> source.hasPermission(2))
                .redirect(root));
    }

    private static int reset(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players)
            throws CommandSyntaxException {
        for (ServerPlayer player : players) {
            ItemCooldowns cooldowns = player.getCooldowns();
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && cooldowns.isOnCooldown(stack.getItem())) {
                    cooldowns.removeCooldown(stack.getItem());
                }
            }
        }
        int count = players.size();
        context.getSource().sendSuccess(() -> count == 1
                ? Component.translatable("commands.simplyanime.cooldown.reset.single", players.iterator().next().getDisplayName())
                : Component.translatable("commands.simplyanime.cooldown.reset.multiple", count), true);
        return count;
    }
}
