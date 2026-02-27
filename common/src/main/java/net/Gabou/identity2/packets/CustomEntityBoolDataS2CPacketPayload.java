package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CustomEntityBoolDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryBool> entries) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.CUSTOM_BOOL_DATA_ID;

    public static CustomEntityBoolDataS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new CustomEntityBoolDataS2CPacketPayload(buffer.readVarInt(), CustomEntityDataS2CPacket.readBoolEntries(buffer));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityid);
        CustomEntityDataS2CPacket.writeBoolEntries(buffer, entries);
    }
}
