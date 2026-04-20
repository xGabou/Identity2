package ember.qualitycommands.packets;

import ember.qualitycommands.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record IdentityAbilityPacketPayload(int entityid) implements CustomPayload {
    public static final CustomPayload.Id<IdentityAbilityPacketPayload> ID = new CustomPayload.Id<>(ModPackets.IDENTITY_ABILITY_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, IdentityAbilityPacketPayload> CODEC = 
	PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		IdentityAbilityPacketPayload::entityid,
		IdentityAbilityPacketPayload::new
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