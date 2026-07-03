package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionJarSelectC2SPacketPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<ProgressionJarSelectC2SPacketPayload> ID = new Type<>(ModPackets.PROGRESSION_JAR_SELECT_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionJarSelectC2SPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ProgressionJarSelectC2SPacketPayload::slotIndex,
        ProgressionJarSelectC2SPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
