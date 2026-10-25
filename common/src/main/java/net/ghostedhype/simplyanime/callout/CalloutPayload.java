package net.ghostedhype.simplyanime.callout;

import net.ghostedhype.simplyanime.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record CalloutPayload(Callout callout) implements CustomPacketPayload {
    public static final ResourceLocation PACKET = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "callout");

    public static final Type<CalloutPayload> TYPE = new Type<>(PACKET);

    public static final StreamCodec<RegistryFriendlyByteBuf, CalloutPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.write(buf),
                    CalloutPayload::new
            );

    public CalloutPayload(FriendlyByteBuf buf) {
        this(buf.readEnum(Callout.class));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(callout);
    }

    @Override
    public @NotNull Type<CalloutPayload> type() {
        return TYPE;
    }
}
