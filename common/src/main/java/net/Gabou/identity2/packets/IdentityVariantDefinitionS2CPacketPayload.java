package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One bounded fragment of a server-owned identity variant definition.
 */
public record IdentityVariantDefinitionS2CPacketPayload(
    int entityId,
    boolean reset,
    String identityId,
    String variantId,
    int chunkIndex,
    int chunkCount,
    byte[] data
) implements CustomPacketPayload {
    private static final int MAX_IDENTITY_ID_CHARS = 256;
    private static final int MAX_VARIANT_ID_CHARS = 64;

    public static final Type<IdentityVariantDefinitionS2CPacketPayload> ID =
        new Type<>(ModPackets.IDENTITY_VARIANT_DEFINITION_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityVariantDefinitionS2CPacketPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public IdentityVariantDefinitionS2CPacketPayload decode(RegistryFriendlyByteBuf buffer) {
                return new IdentityVariantDefinitionS2CPacketPayload(
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readUtf(MAX_IDENTITY_ID_CHARS),
                    buffer.readUtf(MAX_VARIANT_ID_CHARS),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readByteArray()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, IdentityVariantDefinitionS2CPacketPayload payload) {
                buffer.writeVarInt(payload.entityId());
                buffer.writeBoolean(payload.reset());
                buffer.writeUtf(payload.identityId(), MAX_IDENTITY_ID_CHARS);
                buffer.writeUtf(payload.variantId(), MAX_VARIANT_ID_CHARS);
                buffer.writeVarInt(payload.chunkIndex());
                buffer.writeVarInt(payload.chunkCount());
                buffer.writeByteArray(payload.data());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
