package net.ghostedhype.simplyanime.client;

import net.minecraft.client.Minecraft;

public final class ClientTicks {

    private ClientTicks() {
    }

    public static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }
}
