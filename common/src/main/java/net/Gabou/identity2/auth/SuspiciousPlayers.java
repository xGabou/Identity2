package net.Gabou.identity2.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class SuspiciousPlayers {
    public record SuspiciousState(String reason, long flaggedAtMs) {
    }

    private static final Map<UUID, SuspiciousState> SUSPICIOUS = new HashMap<>();

    private SuspiciousPlayers() {
    }

    public static void mark(ServerPlayer player, String reason) {
        if (player != null) {
            mark(player.getUUID(), reason);
        }
    }

    public static void mark(UUID uuid, String reason) {
        if (uuid != null) {
            SUSPICIOUS.put(uuid, new SuspiciousState(reason == null ? "" : reason, System.currentTimeMillis()));
        }
    }

    public static boolean isSuspicious(ServerPlayer player) {
        return player != null && isSuspicious(player.getUUID());
    }

    public static boolean isSuspicious(UUID uuid) {
        return uuid != null && SUSPICIOUS.containsKey(uuid);
    }

    public static String getReason(UUID uuid) {
        SuspiciousState state = uuid == null ? null : SUSPICIOUS.get(uuid);
        return state == null ? "" : state.reason();
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            clear(player.getUUID());
        }
    }

    public static void clear(UUID uuid) {
        if (uuid != null) {
            SUSPICIOUS.remove(uuid);
        }
    }
}
