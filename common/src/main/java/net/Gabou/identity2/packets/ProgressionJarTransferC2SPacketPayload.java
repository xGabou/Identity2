package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionJarTransferC2SPacketPayload(
    int slotIndex,
    String identityId,
    int amount,
    boolean deposit
) implements CustomPacketPayload {
    public static final Type<ProgressionJarTransferC2SPacketPayload> ID =
        new Type<>(ModPackets.PROGRESSION_JAR_TRANSFER_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionJarTransferC2SPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ProgressionJarTransferC2SPacketPayload::slotIndex,
        ByteBufCodecs.STRING_UTF8,
        ProgressionJarTransferC2SPacketPayload::identityId,
        ByteBufCodecs.VAR_INT,
        ProgressionJarTransferC2SPacketPayload::amount,
        ByteBufCodecs.BOOL,
        ProgressionJarTransferC2SPacketPayload::deposit,
        ProgressionJarTransferC2SPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
