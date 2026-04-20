package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModNetworking;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record C2SChallengeReplyPacket(long nonce, String response) implements NetworkPayload {
    public static final ResourceLocation ID = ModNetworking.AUTH_CHALLENGE_REPLY_PACKET_ID;

    public static C2SChallengeReplyPacket decode(FriendlyByteBuf buffer) {
        return new C2SChallengeReplyPacket(buffer.readLong(), buffer.readUtf(128));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(nonce);
        buffer.writeUtf(response == null ? "" : response);
    }
}
