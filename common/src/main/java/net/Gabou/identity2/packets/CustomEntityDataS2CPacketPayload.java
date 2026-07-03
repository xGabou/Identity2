package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CustomEntityDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.Entry> entries) implements CustomPacketPayload {
    public static final Type<CustomEntityDataS2CPacketPayload> ID = new Type<>(ModPackets.CUSTOM_DOUBLE_DATA_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomEntityDataS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CustomEntityDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.Entry.CODEC.apply(ByteBufCodecs.list()),
        CustomEntityDataS2CPacketPayload::entries,
        CustomEntityDataS2CPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
