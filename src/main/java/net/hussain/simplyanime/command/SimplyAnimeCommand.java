package net.hussain.simplyanime.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

// /SimplyAnime cooldown reset [players], handy for testing abilities back to back
public final class SimplyAnimeCommand {

    private SimplyAnimeCommand() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> root = dispatcher.register(CommandManager.literal("SimplyAnime")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("cooldown")
                        .then(CommandManager.literal("reset")
                                .executes(context -> reset(context, List.of(context.getSource().getPlayerOrThrow())))
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(context -> reset(context, EntityArgumentType.getPlayers(context, "targets")))))));
        dispatcher.register(CommandManager.literal("simplyanime")
                .requires(source -> source.hasPermissionLevel(2))
                .redirect(root));
    }

    private static int reset(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players)
            throws CommandSyntaxException {
        for (ServerPlayerEntity player : players) {
            ItemCooldownManager cooldowns = player.getItemCooldownManager();
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStack(slot);
                if (!stack.isEmpty() && cooldowns.isCoolingDown(stack.getItem())) {
                    cooldowns.remove(stack.getItem());
                }
            }
        }
        int count = players.size();
        context.getSource().sendFeedback(() -> count == 1
                ? Text.translatable("commands.simplyanime.cooldown.reset.single", players.iterator().next().getDisplayName())
                : Text.translatable("commands.simplyanime.cooldown.reset.multiple", count), true);
        return count;
    }
}
