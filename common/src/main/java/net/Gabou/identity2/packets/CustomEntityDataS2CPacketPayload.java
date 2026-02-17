package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CustomEntityDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.Entry> entries) implements CustomPayload {
    public static final CustomPayload.Id<CustomEntityDataS2CPacketPayload> ID = new CustomPayload.Id<>(ModPackets.CUSTOM_DOUBLE_DATA_ID);
    public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacketPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        CustomEntityDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.Entry.CODEC.collect(PacketCodecs.toList()),
        CustomEntityDataS2CPacketPayload::entries,
        CustomEntityDataS2CPacketPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
