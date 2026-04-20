package net.Gabou.identity2.fabric.auth;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.Gabou.identity2.auth.C2SChallengeReplyPacket;
import net.Gabou.identity2.auth.ClientAuth;
import net.Gabou.identity2.auth.S2CChallengePacket;
import net.Gabou.identity2.auth.ServerAuth;
import net.Gabou.identity2.platform.ModNetworkingPlatform;
import net.Gabou.identity2.util.NetworkCompat;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class Identity2FabricNetworkingPlatform implements ModNetworkingPlatform {
    private static boolean commonRegistered = false;
    private static boolean clientRegistered = false;

    @Override
    public void registerCommonPackets() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            C2SChallengeReplyPacket.ID,
            C2SChallengeReplyPacket::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    ServerAuth.handleChallengeReply(player, payload);
                }
            })
        );
    }

    @Override
    public void registerClientPackets() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        NetworkCompat.registerReceiver(
            NetworkManager.s2c(),
            S2CChallengePacket.ID,
            S2CChallengePacket::decode,
            (payload, context) -> context.queue(() -> ClientAuth.handleChallenge(payload))
        );
    }

    @Override
    public void sendToPlayer(ServerPlayer player, NetworkPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToPlayer(player, payload.id(), buffer);
    }

    @Override
    public void sendToServer(NetworkPayload payload) {
        if (payload == null) {
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToServer(payload.id(), buffer);
    }
}
