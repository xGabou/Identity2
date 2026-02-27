package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CustomEntityStringDataS2CPacketPayload(int entityid, List<CustomEntityDataS2CPacket.EntryString> entries) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.CUSTOM_STRING_DATA_ID;

    public static CustomEntityStringDataS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new CustomEntityStringDataS2CPacketPayload(buffer.readVarInt(), CustomEntityDataS2CPacket.readStringEntries(buffer));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityid);
        CustomEntityDataS2CPacket.writeStringEntries(buffer, entries);
    }
}
