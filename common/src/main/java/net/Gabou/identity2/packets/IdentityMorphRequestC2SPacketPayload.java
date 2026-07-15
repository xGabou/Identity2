package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IdentityMorphRequestC2SPacketPayload(String identityId, String variantId) implements CustomPacketPayload {
    public static final Type<IdentityMorphRequestC2SPacketPayload> ID = new Type<>(ModPackets.IDENTITY_MORPH_REQUEST_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityMorphRequestC2SPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(256),
        IdentityMorphRequestC2SPacketPayload::identityId,
        ByteBufCodecs.stringUtf8(64),
        IdentityMorphRequestC2SPacketPayload::variantId,
        IdentityMorphRequestC2SPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
