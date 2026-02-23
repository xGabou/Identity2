package net.Gabou.identity2.progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class ProgressionConfig {
    private static final Map<String, String> DEFAULT_JAR_ITEMS = Map.of(
        "mud", "identity2:mud_jar",
        "glass", "identity2:glass_jar",
        "reinforced", "identity2:reinforced_jar",
        "true_soul", "identity2:endgame_jar"
    );

    private ProgressionConfig() {
    }

    public static boolean enableMorphCharges() {
        return IdentitySettings.enableMorphChargeSystem || IdentitySettings.EnableMorphCharges;
    }

    public static boolean enableSoulJars() {
        return IdentitySettings.enableSoulJarSystem || IdentitySettings.EnableSoulJars;
    }

    public static boolean enablePermanentJarMorphs() {
        return IdentitySettings.enablePermanentMorphs || IdentitySettings.EnablePermanentJarMorphs;
    }

    public static boolean enableSoulAbsorption() {
        return IdentitySettings.enableSoulAbsorption || IdentitySettings.EnableSoulAbsorption;
    }

    public static boolean shouldLoseMorphsOnDeath(@Nullable MinecraftServer server) {
        // Charge-based progression is a legitimate gameplay replacement for death loss.
        if (enableMorphCharges()) {
            return false;
        }

        boolean disableRequested = IdentitySettings.disableMorphLossOnDeath || !IdentitySettings.loseAllMorphsOnDeath;
        if (!disableRequested) {
            return true;
        }

        if (IdentitySettings.allowDisableMorphLossOnDeathWithoutCheats) {
            return false;
        }

        return !areCheatsEnabled(server);
    }

    public static Identifier resolveJarItemId(String tier) {
        String normalizedTier = ProgressionConfigHelper.normalizeTier(tier);
        Map<String, String> configured = parseTierStringEntries(IdentitySettings.soulJarTierItems);
        String value = configured.getOrDefault(normalizedTier, DEFAULT_JAR_ITEMS.getOrDefault(normalizedTier, "minecraft:clay_ball"));
        try {
            return Identifier.parse(value);
        } catch (Exception ignored) {
            return Identifier.fromNamespaceAndPath("minecraft", "clay_ball");
        }
    }

    public static String resolveJarRecipeId(String tier) {
        String normalizedTier = ProgressionConfigHelper.normalizeTier(tier);
        Map<String, String> configured = parseTierStringEntries(IdentitySettings.soulJarTierRecipes);
        return configured.getOrDefault(normalizedTier, "");
    }

    private static boolean areCheatsEnabled(@Nullable MinecraftServer server) {
        if (server == null || server.getWorldData() == null) {
            return false;
        }
        return server.getWorldData().isAllowCommands();
    }

    private static Map<String, String> parseTierStringEntries(List<String> entries) {
        Map<String, String> out = new HashMap<>();
        if (entries == null) {
            return out;
        }
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            String tier = ProgressionConfigHelper.normalizeTier(raw.substring(0, separator));
            String value = raw.substring(separator + 1).trim();
            if (value.isBlank()) {
                continue;
            }
            out.put(tier, value);
        }
        return out;
    }
}
