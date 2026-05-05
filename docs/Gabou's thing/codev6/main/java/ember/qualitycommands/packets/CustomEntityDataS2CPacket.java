package ember.qualitycommands.packets;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Pair;
import ember.qualitycommands.util.CustomPacketListener;
import ember.qualitycommands.ModPackets;
import java.lang.Double;
public class CustomEntityDataS2CPacket {
	public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacket> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT,
		CustomEntityDataS2CPacket::getEntityId,
		CustomEntityDataS2CPacket.Entry.CODEC.collect(PacketCodecs.toList()),
		CustomEntityDataS2CPacket::getEntries,
		CustomEntityDataS2CPacket::new
	);
	private final int entityId;
	private final List<CustomEntityDataS2CPacket.Entry> entries;

	public CustomEntityDataS2CPacket(int entityId, Collection<Pair<String,Double>> attributes) {
		this.entityId = entityId;
		this.entries = Lists.<CustomEntityDataS2CPacket.Entry>newArrayList();

		for (Pair<String,Double> entityDataInstance : attributes) {
			this.entries
				.add(
					new CustomEntityDataS2CPacket.Entry(entityDataInstance.getLeft(), entityDataInstance.getRight())
				);
		}
	}

	private CustomEntityDataS2CPacket(int entityId, List<CustomEntityDataS2CPacket.Entry> attributes) {
		this.entityId = entityId;
		this.entries = attributes;
	}

	/*@Override
	public PacketType<CustomEntityDataS2CPacket> getPacketType() {
		return ModPackets.SET_ENTITY_DOUBLE_DATA;
	}*/

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		((CustomPacketListener)clientPlayPacketListener).onUpdateCustomData(this);
	}

	public int getEntityId() {
		return this.entityId;
	}

	public List<CustomEntityDataS2CPacket.Entry> getEntries() {
		return this.entries;
	}

	public record Entry(String key, double value) {
		public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacket.Entry> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING,
            CustomEntityDataS2CPacket.Entry::key,
			PacketCodecs.DOUBLE,
            CustomEntityDataS2CPacket.Entry::value,
			CustomEntityDataS2CPacket.Entry::new
		);
	}
    public record EntryString(String key, String value) {
		public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacket.EntryString> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING,
            CustomEntityDataS2CPacket.EntryString::key,
			PacketCodecs.STRING,
            CustomEntityDataS2CPacket.EntryString::value,
			CustomEntityDataS2CPacket.EntryString::new
		);
	}
	public record EntryBool(String key, boolean value) {
		public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacket.EntryBool> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING,
            CustomEntityDataS2CPacket.EntryBool::key,
			PacketCodecs.BOOLEAN,
            CustomEntityDataS2CPacket.EntryBool::value,
			CustomEntityDataS2CPacket.EntryBool::new
		);
	}
	public record EntryNBT(String key, NbtCompound value) {
		public static final PacketCodec<RegistryByteBuf, CustomEntityDataS2CPacket.EntryNBT> CODEC = PacketCodec.<RegistryByteBuf,CustomEntityDataS2CPacket.EntryNBT, String,NbtCompound>tuple(
			PacketCodecs.STRING,
            CustomEntityDataS2CPacket.EntryNBT::key,
			PacketCodecs.codec(NbtCompound.CODEC),
            CustomEntityDataS2CPacket.EntryNBT::value,
			CustomEntityDataS2CPacket.EntryNBT::new
		);
	}
}