package net.Gabou.identity2.progression;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.resources.Identifier;

final class ProgressionConfigHelper {
    private static final Map<String, Integer> DEFAULT_JAR_CAPACITIES = Map.of(
        "mud", 5,
        "glass", 10,
        "reinforced", 15,
        "true_soul", 24
    );

    private ProgressionConfigHelper() {
    }

    static int resolveChargeGain(Identifier identityId) {
        return resolveIdentityScopedInt(identityId, IdentitySettings.chargeGainByIdentity, Math.max(0, IdentitySettings.defaultChargeGainPerKill));
    }

    static int resolveChargeCost(Identifier identityId) {
        int fallback = Math.max(1, IdentitySettings.defaultMorphUseChargeCost);
        return Math.max(1, resolveIdentityScopedInt(identityId, IdentitySettings.chargeCostByIdentity, fallback));
    }

    static int resolveChargeDeathPenalty(Identifier identityId) {
        int fallback = Math.max(1, IdentitySettings.morphDeathChargePenalty);
        return Math.max(1, resolveIdentityScopedInt(identityId, IdentitySettings.chargeDeathPenaltyByIdentity, fallback));
    }

    static int resolvePermanentKillRequirement(Identifier identityId) {
        int fallback = Math.max(1, IdentitySettings.defaultPermanentKillRequirement);
        return Math.max(1, resolveIdentityScopedInt(identityId, IdentitySettings.permanentKillRequirementByIdentity, fallback));
    }

    static int resolveJarCapacity(String tier) {
        Map<String, Integer> capacities = resolveTierCapacities();
        String normalizedTier = normalizeTier(tier);
        Integer capacity = capacities.get(normalizedTier);
        if (capacity != null) {
            return Math.max(1, capacity);
        }
        return DEFAULT_JAR_CAPACITIES.getOrDefault(normalizedTier, 5);
    }

    static Map<String, Integer> resolveTierCapacities() {
        Map<String, Integer> parsed = parseStringIntEntries(IdentitySettings.soulJarTierCapacities);
        if (parsed.isEmpty()) {
            parsed.putAll(DEFAULT_JAR_CAPACITIES);
        }
        return parsed;
    }

    static String normalizeTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return "mud";
        }
        return tier.trim().toLowerCase(Locale.ROOT);
    }

    private static int resolveIdentityScopedInt(Identifier identityId, List<String> entries, int fallback) {
        if (identityId == null || entries == null || entries.isEmpty()) {
            return fallback;
        }
        String fullKey = identityId.toString();
        String pathKey = identityId.getPath();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            String key = raw.substring(0, separator).trim();
            if (!(key.equalsIgnoreCase(fullKey) || key.equalsIgnoreCase(pathKey))) {
                continue;
            }
            Integer parsed = parseInteger(raw.substring(separator + 1));
            if (parsed != null) {
                return parsed;
            }
        }
        return fallback;
    }

    private static Map<String, Integer> parseStringIntEntries(List<String> entries) {
        Map<String, Integer> out = new HashMap<>();
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
            String key = normalizeTier(raw.substring(0, separator));
            Integer value = parseInteger(raw.substring(separator + 1));
            if (value == null) {
                continue;
            }
            out.put(key, value);
        }
        return out;
    }

    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
