package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CustomEntityStringDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryString> entries) implements CustomPayload {
    public static final CustomPayload.Id<CustomEntityStringDataS2CPacketPayload> ID = new CustomPayload.Id<>(ModPackets.CUSTOM_STRING_DATA_ID);
    public static final PacketCodec<RegistryByteBuf, CustomEntityStringDataS2CPacketPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        CustomEntityStringDataS2CPacketPayload::entityid,
        CustomEntityDataS2CPacket.EntryString.CODEC.collect(PacketCodecs.toList()),
        CustomEntityStringDataS2CPacketPayload::entries,
        CustomEntityStringDataS2CPacketPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
