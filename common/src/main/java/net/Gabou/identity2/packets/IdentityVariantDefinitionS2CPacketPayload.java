package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** A bounded fragment of a server-owned identity variant definition. */
public record IdentityVariantDefinitionS2CPacketPayload(
        int entityId, boolean reset, String identityId, String variantId,
        int chunkIndex, int chunkCount, byte[] data
) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.IDENTITY_VARIANT_DEFINITION_PACKET_ID;

    public static IdentityVariantDefinitionS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new IdentityVariantDefinitionS2CPacketPayload(
                buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(256), buffer.readUtf(64),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(20_000)
        );
    }

    @Override public ResourceLocation id() { return ID; }

    @Override public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(reset);
        buffer.writeUtf(identityId == null ? "" : identityId, 256);
        buffer.writeUtf(variantId == null ? "" : variantId, 64);
        buffer.writeVarInt(chunkIndex);
        buffer.writeVarInt(chunkCount);
        byte[] safeData = data == null ? new byte[0] : data;
        if (safeData.length > 20_000) {
            throw new IllegalArgumentException("Variant definition fragment exceeds 20,000 bytes.");
        }
        buffer.writeByteArray(safeData);
    }
}
