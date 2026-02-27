package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CustomEntityDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.Entry> entries) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.CUSTOM_DOUBLE_DATA_ID;

    public static CustomEntityDataS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new CustomEntityDataS2CPacketPayload(buffer.readVarInt(), CustomEntityDataS2CPacket.readDoubleEntries(buffer));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityid);
        CustomEntityDataS2CPacket.writeDoubleEntries(buffer, entries);
    }
}
