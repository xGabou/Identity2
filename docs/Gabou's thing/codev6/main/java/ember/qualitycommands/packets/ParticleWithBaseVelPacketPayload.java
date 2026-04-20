package ember.qualitycommands.packets;

import ember.qualitycommands.ModPackets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

public record ParticleWithBaseVelPacketPayload(ParticleEffect parameters, boolean forceSpawn, boolean important, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float speed, int count, float basevelx, float basevely, float basevelz) implements CustomPayload {
    public static final CustomPayload.Id<ParticleWithBaseVelPacketPayload> ID = new CustomPayload.Id<>(ModPackets.PARTICLE_BVEL_PACKET); 
    public static final PacketCodec<RegistryByteBuf, ParticleWithBaseVelPacketPayload> CODEC = CustomPayload.<RegistryByteBuf,ParticleWithBaseVelPacketPayload>codecOf(ParticleWithBaseVelPacketPayload::write, ParticleWithBaseVelPacketPayload::fromBuf);
	private static ParticleWithBaseVelPacketPayload fromBuf (RegistryByteBuf buf) {
        ParticleWithBaseVelPacketPayload payload=new ParticleWithBaseVelPacketPayload(ParticleTypes.PACKET_CODEC.decode(buf), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());

        return payload;
	}

	private static void write(ParticleWithBaseVelPacketPayload payload,RegistryByteBuf buf) {
        ParticleTypes.PACKET_CODEC.encode(buf, payload.parameters);
		buf.writeBoolean(payload.forceSpawn);
		buf.writeBoolean(payload.important);
		buf.writeFloat(payload.x);
		buf.writeFloat(payload.y);
		buf.writeFloat(payload.z);
		buf.writeFloat(payload.offsetX);
		buf.writeFloat(payload.offsetY);
		buf.writeFloat(payload.offsetZ);
		buf.writeFloat(payload.speed);
		buf.writeInt(payload.count);
        
		buf.writeFloat(payload.basevelx);
		buf.writeFloat(payload.basevely);
		buf.writeFloat(payload.basevelz);
		
	}
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