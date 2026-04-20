package net.Gabou.identity2.progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
        return IdentitySettings.enableMorphChargeSystem;
    }

    public static boolean enableSoulJars() {
        return IdentitySettings.enableSoulJarSystem;
    }

    public static boolean enablePermanentJarMorphs() {
        return IdentitySettings.enablePermanentMorphs;
    }

    public static boolean enableSoulAbsorption() {
        return IdentitySettings.enableSoulAbsorption;
    }

    public static boolean requireChargeForMorphing() {
        return enableMorphCharges() && !IdentitySettings.dontRequireChargeForMorphing;
    }

    public static boolean shouldBypassMorphCharges(@Nullable ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (IdentitySettings.bypassMorphChargesForCreativePlayers && player.getAbilities().instabuild) {
            return true;
        }
        return IdentitySettings.bypassMorphChargesForOperators
            && player.createCommandSourceStack().hasPermission(Commands.LEVEL_ADMINS);
    }

    public static ResourceLocation resolveJarItemId(String tier) {
        String normalizedTier = ProgressionConfigHelper.normalizeTier(tier);
        Map<String, String> configured = parseTierStringEntries(IdentitySettings.soulJarTierItems);
        String value = configured.getOrDefault(normalizedTier, DEFAULT_JAR_ITEMS.getOrDefault(normalizedTier, "minecraft:clay_ball"));
        try {
            return new ResourceLocation(value);
        } catch (Exception ignored) {
            return new ResourceLocation("minecraft", "clay_ball");
        }
    }

    public static String resolveJarRecipeId(String tier) {
        String normalizedTier = ProgressionConfigHelper.normalizeTier(tier);
        Map<String, String> configured = parseTierStringEntries(IdentitySettings.soulJarTierRecipes);
        return configured.getOrDefault(normalizedTier, "");
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
