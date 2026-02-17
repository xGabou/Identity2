package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record IdentityMorphRequestC2SPacketPayload(String identityId) implements CustomPayload {
    public static final CustomPayload.Id<IdentityMorphRequestC2SPacketPayload> ID = new CustomPayload.Id<>(ModPackets.IDENTITY_MORPH_REQUEST_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, IdentityMorphRequestC2SPacketPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        IdentityMorphRequestC2SPacketPayload::identityId,
        IdentityMorphRequestC2SPacketPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
