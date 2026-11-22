package dev.yukiinotenshi.simplephonepromax.network;

import dev.yukiinotenshi.foxoperator.OperatorWire;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OperatorPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<OperatorPayload> TYPE = new Type<>(Identifier.parse(OperatorWire.CHANNEL));
    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                if (buffer.readableBytes() > OperatorWire.CHUNK + 16) {
                    buffer.skipBytes(buffer.readableBytes());
                    return new OperatorPayload(new byte[0]);
                }
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new OperatorPayload(data);
            });

    @Override public Type<OperatorPayload> type() { return TYPE; }
}
