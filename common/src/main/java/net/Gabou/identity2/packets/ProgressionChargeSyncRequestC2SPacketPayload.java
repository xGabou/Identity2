package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProgressionChargeSyncRequestC2SPacketPayload() implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.PROGRESSION_CHARGE_SYNC_REQUEST_PACKET_ID;

    public static ProgressionChargeSyncRequestC2SPacketPayload decode(FriendlyByteBuf buffer) {
        return new ProgressionChargeSyncRequestC2SPacketPayload();
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
    }
}
