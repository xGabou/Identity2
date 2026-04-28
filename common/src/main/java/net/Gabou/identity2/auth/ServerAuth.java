package net.Gabou.identity2.auth;

import dev.architectury.platform.Platform;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerAuth {
    private ServerAuth() {
    }

    public static boolean onLogin(Connection connection, ServerPlayer player) {
        if (player == null) {
            return true;
        }

        if (Platform.isDevelopmentEnvironment()) {
            Identity2.LOGGER.info(
                "Skipping launcher checks for {} because the game is running in a development environment.",
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

        return true;
    }

    public static void handleLauncherReport(ServerPlayer player, C2SLauncherReportPacket packet) {
        if (player == null || packet == null) {
            return;
        }

        String launcherReason = packet.reason();
        if (launcherReason != null && !launcherReason.isBlank() && player.level() instanceof ServerLevel serverLevel) {
            TLauncherDetectedHandler.handle(serverLevel, player, launcherReason);
        }
    }
}
