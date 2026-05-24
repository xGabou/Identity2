package net.Gabou.identity2.auth;

import java.util.Map;
import java.util.UUID;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.util.NetworkCompat;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerAuth {
    private ServerAuth() {
    }

    public static boolean onLogin(Connection connection, ServerPlayer player) {
        if (player == null) {
            return true;
        }

        PendingAuthManager.clear(player);

        if (Platform.isDevelopmentEnvironment()) {
            Identity2.LOGGER.info(
                    "Skipping launcher/auth checks for {} because the game is running in a development environment.",
                    player.getGameProfile().getName()
            );
            return true;
        }

        if (player.getUUID() != null && player.getUUID().version() == 3 && IdentitySettings.authStrictOfflineUuidReject) {
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

        NetworkCompat.sendToPlayer(player, new S2CChallengePacket(pending.nonce()));
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

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }

            Identity2.LOGGER.warn("Auth challenge timed out for {}", player.getGameProfile().getName());
            if (IdentitySettings.authKickOnFailure) {
                player.connection.disconnect(Component.literal("Authentication timed out."));
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PendingAuthManager.clear(player);
    }

    public static void handleChallengeReply(ServerPlayer player, C2SChallengeReplyPacket packet) {
        if (player == null || packet == null) {
            return;
        }

        String launcherReason = packet.launcherReason();
        if (launcherReason != null && !launcherReason.isBlank() && player.level() instanceof ServerLevel serverLevel) {
            TLauncherDetectedHandler.handle(serverLevel, player, launcherReason);
            PendingAuthManager.clear(player);
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
        Identity2.LOGGER.info("Auth challenge completed for {}", player.getGameProfile().getName());
    }

    private static void markInvalid(ServerPlayer player, String reason) {
        PendingAuthManager.clear(player);
        Identity2.LOGGER.warn(
            "Marked {} as failed auth because verification failed: {}",
            player.getGameProfile().getName(),
            reason
        );
        if (IdentitySettings.authKickOnFailure) {
            player.connection.disconnect(Component.literal("Authentication failed."));
        }
    }
}
