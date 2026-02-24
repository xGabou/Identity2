package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MorphAcquisitionS2CPacketPayload(
    int originEntityId,
    int acquiredEntityId,
    double acquiredX,
    double acquiredY,
    double acquiredZ,
    boolean morphAcquisition
) implements CustomPacketPayload {
    public static final Type<MorphAcquisitionS2CPacketPayload> ID =
        new Type<>(ModPackets.MORPH_ACQUISITION_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphAcquisitionS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MorphAcquisitionS2CPacketPayload::originEntityId,
        ByteBufCodecs.VAR_INT,
        MorphAcquisitionS2CPacketPayload::acquiredEntityId,
        ByteBufCodecs.DOUBLE,
        MorphAcquisitionS2CPacketPayload::acquiredX,
        ByteBufCodecs.DOUBLE,
        MorphAcquisitionS2CPacketPayload::acquiredY,
        ByteBufCodecs.DOUBLE,
        MorphAcquisitionS2CPacketPayload::acquiredZ,
        ByteBufCodecs.BOOL,
        MorphAcquisitionS2CPacketPayload::morphAcquisition,
        MorphAcquisitionS2CPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
