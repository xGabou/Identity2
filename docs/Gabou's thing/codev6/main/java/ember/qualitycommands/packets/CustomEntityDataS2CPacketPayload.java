package ember.qualitycommands.packets;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import ember.qualitycommands.util.CustomPacketListener;
import ember.qualitycommands.ModPackets;
import java.lang.Double;
import net.minecraft.network.packet.CustomPayload;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
public record CustomEntityDataS2CPacketPayload(int entityid,List<CustomEntityDataS2CPacket.Entry> entries) implements CustomPayload {
    public static final CustomPayload.Id<CustomEntityDataS2CPacketPayload> ID = new CustomPayload.Id<>(ModPackets.CUSTOM_DOUBLE_DATA_ID);
    public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacketPayload> CODEC = 
	PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		CustomEntityDataS2CPacketPayload::entityid,
		CustomEntityDataS2CPacket.Entry.CODEC.collect(PacketCodecs.toList()),
		CustomEntityDataS2CPacketPayload::entries,
		CustomEntityDataS2CPacketPayload::new
	);
	//PacketCodec.tuple(BlockPos.PACKET_CODEC, BlockHighlightPayload::blockPos, BlockHighlightPayload::new);
    // should you need to send more data, add the appropriate record parameters and change your codec:
    // public static final PacketCodec<RegistryByteBuf, BlockHighlightPayload> CODEC = PacketCodec.tuple(
    //         BlockPos.PACKET_CODEC, BlockHighlightPayload::blockPos,
    //         PacketCodecs.INTEGER, BlockHighlightPayload::myInt,
    //         Uuids.PACKET_CODEC, BlockHighlightPayload::myUuid,
    //         BlockHighlightPayload::new
    // );
 
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}