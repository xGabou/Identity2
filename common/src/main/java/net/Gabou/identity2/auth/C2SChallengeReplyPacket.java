package net.Gabou.identity2.auth;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record C2SChallengeReplyPacket(long nonce, String response, String launcherReason) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.AUTH_CHALLENGE_REPLY_PACKET_ID;

    public static C2SChallengeReplyPacket decode(FriendlyByteBuf buffer) {
        return new C2SChallengeReplyPacket(
            buffer.readVarLong(),
            buffer.readUtf(),
            buffer.readUtf()
        );
    }

    public C2SChallengeReplyPacket {
        response = response == null ? "" : response;
        launcherReason = launcherReason == null ? "" : launcherReason;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(nonce);
        buffer.writeUtf(response);
        buffer.writeUtf(launcherReason);
    }
}
