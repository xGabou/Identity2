package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionPlayerChargesS2CPacketPayload(String serializedCharges) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ProgressionPlayerChargesS2CPacketPayload> ID =
        new CustomPacketPayload.Type<>(ModPackets.PROGRESSION_PLAYER_CHARGES_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionPlayerChargesS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ProgressionPlayerChargesS2CPacketPayload::serializedCharges,
        ProgressionPlayerChargesS2CPacketPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
