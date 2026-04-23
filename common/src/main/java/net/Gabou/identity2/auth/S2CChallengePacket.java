package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record S2CChallengePacket(long nonce) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.AUTH_CHALLENGE_PACKET_ID;

    public static S2CChallengePacket decode(FriendlyByteBuf buffer) {
        return new S2CChallengePacket(buffer.readVarLong());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(nonce);
    }
}
