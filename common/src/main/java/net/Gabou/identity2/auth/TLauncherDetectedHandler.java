package net.Gabou.identity2.auth;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import net.Gabou.identity2.Identity2;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

public final class TLauncherDetectedHandler {
    private static final String BAN_SOURCE = "Identity2";

    private TLauncherDetectedHandler() {
    }

    public static void handle(ServerLevel level, ServerPlayer player, String reason) {
        if (level == null || player == null || reason == null || reason.isBlank()) {
            return;
        }

        handle(level, player.getUUID(), player.getName().getString(), reason);
        banIp(level, player, reason);
        disconnect(player, reason);
    }

    public static void handle(ServerLevel level, UUID uuid, String playerName, String reason) {
        if (level == null || uuid == null || reason == null || reason.isBlank()) {
            return;
        }

        NameAndId profile = new NameAndId(uuid, playerName == null || playerName.isBlank() ? uuid.toString() : playerName);
        UserBanList bans = level.getServer().getPlayerList().getBans();
        if (!bans.isBanned(profile)) {
            bans.add(new UserBanListEntry(profile, new Date(), BAN_SOURCE, null, reason));
            saveBanLists(level);
            Identity2.LOGGER.error(
                "Banned launcher-violating player {} ({}) on {}: {}",
                profile.name(),
                profile.id(),
                level.dimension().location(),
                reason
            );
        }
    }

    private static void banIp(ServerLevel level, ServerPlayer player, String reason) {
        String ipAddress = player.getIpAddress();
        if (ipAddress == null || ipAddress.isBlank() || "<unknown>".equals(ipAddress)) {
            return;
        }

        IpBanList ipBans = level.getServer().getPlayerList().getIpBans();
        if (ipBans.isBanned(ipAddress)) {
            return;
        }

        ipBans.add(new IpBanListEntry(ipAddress, new Date(), BAN_SOURCE, null, reason));
        saveBanLists(level);
        Identity2.LOGGER.error("Banned launcher-violating IP {} on {}: {}", ipAddress, level.dimension().location(), reason);
    }

    private static void saveBanLists(ServerLevel level) {
        try {
            level.getServer().getPlayerList().getBans().save();
            level.getServer().getPlayerList().getIpBans().save();
        } catch (IOException e) {
            Identity2.LOGGER.warn("Failed to persist launcher violation ban lists", e);
        }
    }

    private static void disconnect(ServerPlayer player, String reason) {
        if (player.connection != null) {
            player.connection.disconnect(Component.literal("Launcher violation detected: " + reason));
        }
    }
}
