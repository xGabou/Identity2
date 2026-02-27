package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record OpenProgressionScreenS2CPacketPayload() implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.OPEN_PROGRESSION_SCREEN_PACKET_ID;

    public static OpenProgressionScreenS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new OpenProgressionScreenS2CPacketPayload();
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
    }
}
