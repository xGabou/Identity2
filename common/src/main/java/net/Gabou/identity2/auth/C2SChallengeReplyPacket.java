package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SChallengeReplyPacket(long nonce, String response, String launcherReason) implements CustomPacketPayload {
    public static final Type<C2SChallengeReplyPacket> ID = new Type<>(ModPackets.AUTH_CHALLENGE_REPLY_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SChallengeReplyPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        C2SChallengeReplyPacket::nonce,
        ByteBufCodecs.STRING_UTF8,
        C2SChallengeReplyPacket::response,
        ByteBufCodecs.STRING_UTF8,
        C2SChallengeReplyPacket::launcherReason,
        C2SChallengeReplyPacket::new
    );

    public C2SChallengeReplyPacket {
        response = response == null ? "" : response;
        launcherReason = launcherReason == null ? "" : launcherReason;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
