package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CustomEntityBoolDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryBool> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CustomEntityBoolDataS2CPacketPayload> ID = new CustomPacketPayload.Type<>(ModPackets.CUSTOM_BOOL_DATA_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomEntityBoolDataS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CustomEntityBoolDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.EntryBool.CODEC.apply(ByteBufCodecs.list()),
        CustomEntityBoolDataS2CPacketPayload::entries,
        CustomEntityBoolDataS2CPacketPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
