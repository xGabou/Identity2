package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProgressionJarTransferC2SPacketPayload(
    int slotIndex,
    String identityId,
    int amount,
    boolean deposit
) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.PROGRESSION_JAR_TRANSFER_PACKET_ID;

    public static ProgressionJarTransferC2SPacketPayload decode(FriendlyByteBuf buffer) {
        return new ProgressionJarTransferC2SPacketPayload(
            buffer.readVarInt(),
            buffer.readUtf(),
            buffer.readVarInt(),
            buffer.readBoolean()
        );
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(slotIndex);
        buffer.writeUtf(identityId == null ? "" : identityId);
        buffer.writeVarInt(amount);
        buffer.writeBoolean(deposit);
    }
}
