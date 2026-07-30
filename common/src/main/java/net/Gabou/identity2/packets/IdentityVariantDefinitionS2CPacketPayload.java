package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A bounded fragment of a server-owned identity variant definition. */
public record IdentityVariantDefinitionS2CPacketPayload(
    int entityId, boolean reset, String identityId, String variantId, int chunkIndex, int chunkCount, byte[] data
) implements CustomPacketPayload {
    public static final int MAX_CHUNK_BYTES = 20_000;
    private static final int MAX_IDENTITY_ID_CHARS = 256;
    private static final int MAX_VARIANT_ID_CHARS = 64;
    public static final Type<IdentityVariantDefinitionS2CPacketPayload> ID =
        new Type<>(ModPackets.IDENTITY_VARIANT_DEFINITION_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityVariantDefinitionS2CPacketPayload> CODEC = new StreamCodec<>() {
        @Override public IdentityVariantDefinitionS2CPacketPayload decode(RegistryFriendlyByteBuf b) {
            return new IdentityVariantDefinitionS2CPacketPayload(b.readVarInt(), b.readBoolean(),
                b.readUtf(MAX_IDENTITY_ID_CHARS), b.readUtf(MAX_VARIANT_ID_CHARS), b.readVarInt(), b.readVarInt(),
                b.readByteArray(MAX_CHUNK_BYTES));
        }
        @Override public void encode(RegistryFriendlyByteBuf b, IdentityVariantDefinitionS2CPacketPayload p) {
            if (p.data() == null || p.data().length > MAX_CHUNK_BYTES) throw new IllegalArgumentException("Variant definition fragment is too large");
            b.writeVarInt(p.entityId()); b.writeBoolean(p.reset()); b.writeUtf(p.identityId(), MAX_IDENTITY_ID_CHARS);
            b.writeUtf(p.variantId(), MAX_VARIANT_ID_CHARS); b.writeVarInt(p.chunkIndex()); b.writeVarInt(p.chunkCount()); b.writeByteArray(p.data());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
