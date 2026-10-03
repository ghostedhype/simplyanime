package net.hussain.simplyanime.callout;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.hussain.simplyanime.SimplyAnime;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Callouts {

    public static final Identifier PACKET = new Identifier(SimplyAnime.MOD_ID, "callout");
    private static final double RANGE = 48.0;

    public static void send(ServerWorld world, Vec3d at, Callout callout) {
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(at) < RANGE * RANGE)) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeEnumConstant(callout);
            NetworkManager.sendToPlayer(player, PACKET, buf);
        }
    }
}
