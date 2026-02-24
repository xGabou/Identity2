package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionJarStateS2CPacketPayload(
    int slotIndex,
    String jarId,
    String jarTier,
    String serializedJarCharges,
    String serializedPlayerCharges,
    String message
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ProgressionJarStateS2CPacketPayload> ID =
        new CustomPacketPayload.Type<>(ModPackets.PROGRESSION_JAR_STATE_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionJarStateS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ProgressionJarStateS2CPacketPayload::slotIndex,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarStateS2CPacketPayload::jarId,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarStateS2CPacketPayload::jarTier,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarStateS2CPacketPayload::serializedJarCharges,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarStateS2CPacketPayload::serializedPlayerCharges,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarStateS2CPacketPayload::message,
        ProgressionJarStateS2CPacketPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
