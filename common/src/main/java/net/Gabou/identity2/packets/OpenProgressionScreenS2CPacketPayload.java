package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenProgressionScreenS2CPacketPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenProgressionScreenS2CPacketPayload> ID =
        new CustomPacketPayload.Type<>(ModPackets.OPEN_PROGRESSION_SCREEN_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenProgressionScreenS2CPacketPayload> CODEC =
        StreamCodec.of((buffer, payload) -> {}, buffer -> new OpenProgressionScreenS2CPacketPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
