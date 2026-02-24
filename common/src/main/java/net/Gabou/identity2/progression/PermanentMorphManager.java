package net.Gabou.identity2.progression;

import net.Gabou.identity2.IdentitySettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PermanentMorphManager {
    private PermanentMorphManager() {
    }

    public static boolean isPermanentMorph(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return false;
        }
        if (ProgressionConfig.enableSoulAbsorption() && SoulAbsorptionManager.isAbsorbed(player, identityId, variantNbt)) {
            return true;
        }
        if (!ProgressionConfig.enablePermanentJarMorphs()) {
            return false;
        }
        return ProgressionConfig.enableSoulJars() && SoulJarManager.isPermanentInJar(player, identityId, variantNbt);
    }

    public static boolean hasDeathLossProtection(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return false;
        }
        if (ProgressionConfig.enableSoulAbsorption() && SoulAbsorptionManager.isAbsorbed(player, identityId, variantNbt)) {
            return true;
        }
        if (!ProgressionConfig.enableSoulJars()) {
            return false;
        }
        if (!IdentitySettings.trueSoulJarPreventsDeathPenalty) {
            return false;
        }
        return SoulJarManager.isStoredInTrueSoulJar(player, identityId, variantNbt);
    }
}
