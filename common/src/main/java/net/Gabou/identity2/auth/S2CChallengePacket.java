package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CChallengePacket(long nonce) implements CustomPacketPayload {
    public static final Type<S2CChallengePacket> ID = new Type<>(ModPackets.AUTH_CHALLENGE_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CChallengePacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG,
        S2CChallengePacket::nonce,
        S2CChallengePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
