package net.Gabou.identity2.auth;

import java.util.concurrent.ThreadLocalRandom;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class AuthGuards {
    private AuthGuards() {
    }

    public static boolean isLikelyOfflineUuid(ServerPlayer player) {
        return player != null && player.getUUID() != null && player.getUUID().version() == 3;
    }

    public static boolean canUseProtectedFeature(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(Commands.LEVEL_ADMINS)) {
            return true;
        }
        return !PendingAuthManager.isPending(player.getUUID()) && !SuspiciousPlayers.isSuspicious(player.getUUID());
    }

    public static boolean shouldSabotageFeatureUse(ServerPlayer player) {
        if (player == null || canUseProtectedFeature(player)) {
            return false;
        }
        double chance = Math.max(0.0D, Math.min(1.0D, IdentitySettings.authAbilityFailureChance));
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static int inflateCooldown(int baseCooldown) {
        if (baseCooldown <= 0) {
            return 20;
        }
        float multiplier = Math.max(1.0F, IdentitySettings.authCooldownMultiplier);
        return Math.max(baseCooldown + 1, Math.round(baseCooldown * multiplier));
    }
}
