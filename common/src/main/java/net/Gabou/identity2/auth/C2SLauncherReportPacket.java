package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SLauncherReportPacket(String reason) implements CustomPacketPayload {
    public static final Type<C2SLauncherReportPacket> ID = new Type<>(ModPackets.LAUNCHER_REPORT_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SLauncherReportPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        C2SLauncherReportPacket::reason,
        C2SLauncherReportPacket::new
    );

    public C2SLauncherReportPacket {
        reason = reason == null ? "" : reason;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
