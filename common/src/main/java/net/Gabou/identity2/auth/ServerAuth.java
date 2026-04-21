package net.Gabou.identity2.auth;

import java.util.Map;
import java.util.UUID;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.ModNetworking;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerAuth {
    private ServerAuth() {
    }

    public static boolean onLogin(Connection connection, ServerPlayer player) {
        if (player == null) {
            return true;
        }

        PendingAuthManager.clear(player);
        SuspiciousPlayers.clear(player);

        if (AuthGuards.isLikelyOfflineUuid(player) && IdentitySettings.authStrictOfflineUuidReject) {
            Identity2.LOGGER.warn(
                "Rejected {} because strict auth mode disallows offline UUID v3 identities.",
                player.getGameProfile().getName()
            );
            connection.disconnect(Component.literal("Authentication rejected."));
            return false;
        }

        PendingAuthManager.begin(player);
        return true;
    }

    public static void sendChallenge(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PendingAuthManager.PendingAuth pending = PendingAuthManager.get(player.getUUID());
        if (pending == null) {
            return;
        }

        ModNetworking.sendToPlayer(player, new S2CChallengePacket(pending.nonce()));
    }

    public static void onTick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long timeoutMs = PendingAuthManager.getTimeoutMs();
        for (Map.Entry<UUID, PendingAuthManager.PendingAuth> entry : PendingAuthManager.snapshot().entrySet()) {
            PendingAuthManager.PendingAuth pending = entry.getValue();
            if (pending == null || now - pending.issuedAtMs() < timeoutMs) {
                continue;
            }

            UUID uuid = entry.getKey();
            PendingAuthManager.clear(uuid);
            SuspiciousPlayers.mark(uuid, "auth timeout");

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }

            Identity2.LOGGER.warn(
                "Marked {} as suspicious because the auth challenge timed out.",
                player.getGameProfile().getName()
            );
            if (IdentitySettings.authKickOnFailure) {
                player.connection.disconnect(Component.literal("Authentication timed out."));
            } else {
                player.displayClientMessage(Component.literal("Authentication timed out. Mod features are limited."), false);
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PendingAuthManager.clear(player);
        SuspiciousPlayers.clear(player);
    }

    public static void handleChallengeReply(ServerPlayer player, C2SChallengeReplyPacket packet) {
        if (player == null || packet == null) {
            return;
        }

        PendingAuthManager.PendingAuth pending = PendingAuthManager.get(player.getUUID());
        if (pending == null) {
            markInvalid(player, "unexpected auth reply");
            return;
        }
        if (pending.nonce() != packet.nonce()) {
            markInvalid(player, "nonce mismatch");
            return;
        }
        if (!SharedSecret.verifyResponse(player.getUUID(), packet.nonce(), packet.response())) {
            markInvalid(player, "invalid auth response");
            return;
        }

        PendingAuthManager.clear(player);
        SuspiciousPlayers.clear(player);
        Identity2.LOGGER.info("Auth challenge completed for {}", player.getGameProfile().getName());
    }

    private static void markInvalid(ServerPlayer player, String reason) {
        PendingAuthManager.clear(player);
        SuspiciousPlayers.mark(player, reason);
        Identity2.LOGGER.warn(
            "Marked {} as suspicious because auth verification failed: {}",
            player.getGameProfile().getName(),
            reason
        );
        if (IdentitySettings.authKickOnFailure) {
            player.connection.disconnect(Component.literal("Authentication failed."));
        } else {
            player.displayClientMessage(Component.literal("Authentication failed. Mod features are limited."), false);
        }
    }
}
