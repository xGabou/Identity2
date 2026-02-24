package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IdentityVillagerTradeRequestC2SPacketPayload(String targetUuid) implements CustomPacketPayload {
    public static final Type<IdentityVillagerTradeRequestC2SPacketPayload> ID = new Type<>(
        ModPackets.IDENTITY_VILLAGER_TRADE_REQUEST_PACKET_ID
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityVillagerTradeRequestC2SPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        IdentityVillagerTradeRequestC2SPacketPayload::targetUuid,
        IdentityVillagerTradeRequestC2SPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
