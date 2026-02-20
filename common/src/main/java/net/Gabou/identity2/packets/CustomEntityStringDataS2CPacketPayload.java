package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CustomEntityStringDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryString> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CustomEntityStringDataS2CPacketPayload> ID = new CustomPacketPayload.Type<>(ModPackets.CUSTOM_STRING_DATA_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomEntityStringDataS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CustomEntityStringDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.EntryString.CODEC.apply(ByteBufCodecs.list()),
        CustomEntityStringDataS2CPacketPayload::entries,
        CustomEntityStringDataS2CPacketPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
