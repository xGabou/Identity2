package net.Gabou.identity2.progression;

import java.util.Map;
import net.Gabou.identity2.identity.IdentityProgression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class JarProgressionManager {
    private JarProgressionManager() {
    }

    public static int getKillProgress(ServerPlayer player, String jarId, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || jarId == null || identityId == null) {
            return 0;
        }
        String normalizedId = SoulJarManager.normalizeJarId(jarId);
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);
        String key = SoulJarManager.progressKey(normalizedId, identityId, variantToken);
        Map<String, Integer> progress = SoulJarManager.getKillProgressMap(player);
        return Math.max(0, progress.getOrDefault(key, 0));
    }

    public static void onIdentityKilled(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return;
        }
        if (!ProgressionConfig.enableSoulJars() || !ProgressionConfig.enablePermanentJarMorphs()) {
            return;
        }

        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);
        int required = ProgressionConfigHelper.resolvePermanentKillRequirement(identityId);
        Map<String, Integer> progress = SoulJarManager.getKillProgressMap(player);
        SoulJarManager.applyKillProgressToStoredMorphs(player, identityId, variantToken, required, progress);
        SoulJarManager.setKillProgressMap(player, progress);
    }
}
