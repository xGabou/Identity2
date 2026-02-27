package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MorphAcquisitionS2CPacketPayload(
    int originEntityId,
    int acquiredEntityId,
    double acquiredX,
    double acquiredY,
    double acquiredZ,
    boolean morphAcquisition
) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.MORPH_ACQUISITION_PACKET_ID;

    public static MorphAcquisitionS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new MorphAcquisitionS2CPacketPayload(
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readBoolean()
        );
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(originEntityId);
        buffer.writeVarInt(acquiredEntityId);
        buffer.writeDouble(acquiredX);
        buffer.writeDouble(acquiredY);
        buffer.writeDouble(acquiredZ);
        buffer.writeBoolean(morphAcquisition);
    }
}
