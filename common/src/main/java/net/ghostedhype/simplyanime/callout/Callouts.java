package net.ghostedhype.simplyanime.callout;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.networking.api.NetworkApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class Callouts {

    private static final double RANGE = 48.0;

    public static void send(ServerLevel world, Vec3 serverPlayerLocation, Callout callout) {
        NetworkApi networkApi = ConfigApiJava.network();
        CalloutPayload payload = new CalloutPayload(callout);

        for (ServerPlayer player : world.getPlayers(
                p -> p.distanceToSqr(serverPlayerLocation) < RANGE * RANGE)) {
                    if (networkApi.canSend(CalloutPayload.PACKET, player)) {
                        networkApi.send(payload, player);
                    }
        }

    }
}
