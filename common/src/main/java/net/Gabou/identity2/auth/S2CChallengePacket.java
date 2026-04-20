package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModNetworking;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record S2CChallengePacket(long nonce) implements NetworkPayload {
    public static final ResourceLocation ID = ModNetworking.AUTH_CHALLENGE_PACKET_ID;

    public static S2CChallengePacket decode(FriendlyByteBuf buffer) {
        return new S2CChallengePacket(buffer.readLong());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(nonce);
    }
}
