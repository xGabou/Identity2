package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record IdentityVillagerTradeRequestC2SPacketPayload(String targetUuid) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.IDENTITY_VILLAGER_TRADE_REQUEST_PACKET_ID;

    public static IdentityVillagerTradeRequestC2SPacketPayload decode(FriendlyByteBuf buffer) {
        return new IdentityVillagerTradeRequestC2SPacketPayload(buffer.readUtf());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(targetUuid == null ? "" : targetUuid);
    }
}
