package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record IdentityAbilityPacketPayload(int entityid) implements CustomPayload {
    public static final CustomPayload.Id<IdentityAbilityPacketPayload> ID = new CustomPayload.Id<>(ModPackets.IDENTITY_ABILITY_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, IdentityAbilityPacketPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        IdentityAbilityPacketPayload::entityid,
        IdentityAbilityPacketPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
