package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProgressionJarSelectC2SPacketPayload(int slotIndex) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.PROGRESSION_JAR_SELECT_PACKET_ID;

    public static ProgressionJarSelectC2SPacketPayload decode(FriendlyByteBuf buffer) {
        return new ProgressionJarSelectC2SPacketPayload(buffer.readVarInt());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(slotIndex);
    }
}
