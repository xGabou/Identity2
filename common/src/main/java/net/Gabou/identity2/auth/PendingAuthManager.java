package net.Gabou.identity2.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.server.level.ServerPlayer;

public final class PendingAuthManager {
    public record PendingAuth(long nonce, long issuedAtMs, boolean offlineUuidVersionThree) {
    }

    private static final Map<UUID, PendingAuth> PENDING = new HashMap<>();

    private PendingAuthManager() {
    }

    public static PendingAuth begin(ServerPlayer player) {
        PendingAuth pending = new PendingAuth(
            SharedSecret.createNonce(),
            System.currentTimeMillis(),
            player != null && player.getUUID() != null && player.getUUID().version() == 3
        );
        if (player != null) {
            PENDING.put(player.getUUID(), pending);
        }
        return pending;
    }

    public static PendingAuth get(UUID uuid) {
        return uuid == null ? null : PENDING.get(uuid);
    }

    public static boolean isPending(UUID uuid) {
        return uuid != null && PENDING.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        if (uuid != null) {
            PENDING.remove(uuid);
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            clear(player.getUUID());
        }
    }

    public static Map<UUID, PendingAuth> snapshot() {
        return new HashMap<>(PENDING);
    }

    public static long getTimeoutMs() {
        return Math.max(1, IdentitySettings.authChallengeTimeoutTicks) * 50L;
    }
}
