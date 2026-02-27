package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProgressionJarStateS2CPacketPayload(
    int slotIndex,
    String jarId,
    String jarTier,
    String serializedJarCharges,
    String serializedPlayerCharges,
    String message
) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.PROGRESSION_JAR_STATE_PACKET_ID;

    public static ProgressionJarStateS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new ProgressionJarStateS2CPacketPayload(
            buffer.readVarInt(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf()
        );
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(slotIndex);
        buffer.writeUtf(jarId == null ? "" : jarId);
        buffer.writeUtf(jarTier == null ? "" : jarTier);
        buffer.writeUtf(serializedJarCharges == null ? "" : serializedJarCharges);
        buffer.writeUtf(serializedPlayerCharges == null ? "" : serializedPlayerCharges);
        buffer.writeUtf(message == null ? "" : message);
    }
}
