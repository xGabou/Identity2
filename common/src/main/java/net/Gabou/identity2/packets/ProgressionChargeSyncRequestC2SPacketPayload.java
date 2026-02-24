package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionChargeSyncRequestC2SPacketPayload() implements CustomPacketPayload {
    public static final Type<ProgressionChargeSyncRequestC2SPacketPayload> ID =
        new Type<>(ModPackets.PROGRESSION_CHARGE_SYNC_REQUEST_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionChargeSyncRequestC2SPacketPayload> CODEC =
        StreamCodec.of((buffer, payload) -> {}, buffer -> new ProgressionChargeSyncRequestC2SPacketPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
