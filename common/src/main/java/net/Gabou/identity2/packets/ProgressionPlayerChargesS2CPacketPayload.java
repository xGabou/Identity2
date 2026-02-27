package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProgressionPlayerChargesS2CPacketPayload(String serializedCharges) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.PROGRESSION_PLAYER_CHARGES_PACKET_ID;

    public static ProgressionPlayerChargesS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new ProgressionPlayerChargesS2CPacketPayload(buffer.readUtf());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(serializedCharges == null ? "" : serializedCharges);
    }
}
