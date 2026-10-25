package net.ghostedhype.simplyanime;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.ghostedhype.simplyanime.ability.RhittaGrowth;
import net.ghostedhype.simplyanime.command.SimplyAnimeCommand;
import net.minecraft.world.InteractionResult;

public class SimplyAnimeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SimplyAnimeCommon.initialize();
        SimplyAnimeCommon.setup();
        ResourceConditions.register(RecipeToggleCondition.TYPE);

        ServerTickEvents.END_WORLD_TICK.register(CommonEvents::levelTick);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(RhittaGrowth::tick));
        ServerWorldEvents.UNLOAD.register((server, level) -> CommonEvents.levelUnload(level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CommonEvents.serverStopped());
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> CommonEvents.playerGone(player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CommonEvents.playerGone(handler.getPlayer()));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !CommonEvents.blocksDamage(entity, source));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                CommonEvents.blocksAttack(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SimplyAnimeCommand.register(dispatcher));
    }
}
