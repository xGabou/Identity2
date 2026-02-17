package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CustomEntityBoolDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryBool> entries) implements CustomPayload {
    public static final CustomPayload.Id<CustomEntityBoolDataS2CPacketPayload> ID = new CustomPayload.Id<>(ModPackets.CUSTOM_BOOL_DATA_ID);
    public static final PacketCodec<RegistryByteBuf, CustomEntityBoolDataS2CPacketPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        CustomEntityBoolDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.EntryBool.CODEC.collect(PacketCodecs.toList()),
        CustomEntityBoolDataS2CPacketPayload::entries,
        CustomEntityBoolDataS2CPacketPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
