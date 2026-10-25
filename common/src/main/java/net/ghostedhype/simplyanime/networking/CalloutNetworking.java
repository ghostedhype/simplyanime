package net.ghostedhype.simplyanime.networking;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.ghostedhype.simplyanime.callout.CalloutPayload;
import net.ghostedhype.simplyanime.client.hud.CalloutHud;

public final class CalloutNetworking {

    private CalloutNetworking() { };

    public static void init() {
        ConfigApiJava.network().registerS2C(
                CalloutPayload.TYPE,
                CalloutPayload.STREAM_CODEC,
                (payload, context) -> context.execute(
                        () -> handleCallout(payload)
                )
        );
    }

    private static void handleCallout(CalloutPayload payload) {
        CalloutHud.INSTANCE.show(payload.callout());
    }
}
