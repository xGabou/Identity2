package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IdentityMorphRequestC2SPacketPayload(String identityId, String variantId) implements CustomPacketPayload {
    public static final Type<IdentityMorphRequestC2SPacketPayload> ID = new Type<>(ModPackets.IDENTITY_MORPH_REQUEST_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityMorphRequestC2SPacketPayload> CODEC = new StreamCodec<>() {
        @Override public IdentityMorphRequestC2SPacketPayload decode(RegistryFriendlyByteBuf buffer) {
            return new IdentityMorphRequestC2SPacketPayload(buffer.readUtf(256), buffer.readUtf(64));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, IdentityMorphRequestC2SPacketPayload payload) {
            buffer.writeUtf(payload.identityId(), 256);
            buffer.writeUtf(payload.variantId(), 64);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
