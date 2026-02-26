package net.Gabou.identity2.progression;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class SoulJarChargeStorage {
    private static final String ITEM_SOUL_JAR_KEY = "identity2_soul_jar";
    private static final String CHARGE_STORAGE_KEY = "charge_storage";
    private static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private SoulJarChargeStorage() {
    }

    public static JarSnapshot read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        CompoundTag jarTag = net.Gabou.identity2.util.NbtCompat.getCompoundOrNull(root, ITEM_SOUL_JAR_KEY);
        if (jarTag == null || jarTag.isEmpty()) {
            return null;
        }

        String jarId = net.Gabou.identity2.util.NbtCompat.getStringOr(jarTag, "jar_id", "").trim();
        if (jarId.isBlank()) {
            return null;
        }
        String tier = net.Gabou.identity2.util.NbtCompat.getStringOr(jarTag, "tier", "mud").trim();
        Map<String, Integer> charges = sanitize(net.Gabou.identity2.util.NbtCompat.read(jarTag, CHARGE_STORAGE_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
        return new JarSnapshot(jarId, tier.isBlank() ? "mud" : tier, charges);
    }

    public static boolean isPotentialSoulJarItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || itemId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            return false;
        }
        if (resolveTierFromConfiguredItem(itemId) != null) {
            return true;
        }
        return "identity2".equals(itemId.getNamespace()) && itemId.getPath().endsWith("_jar");
    }

    public static JarSnapshot ensureInitialized(ItemStack stack, String fallbackJarId, String fallbackTier) {
        JarSnapshot existing = read(stack);
        if (existing != null) {
            return existing;
        }
        if (stack == null || stack.isEmpty() || !isPotentialSoulJarItem(stack)) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String tier = normalizeTier(fallbackTier);
        String tierFromItem = resolveTierFromConfiguredItem(itemId);
        if (tierFromItem != null) {
            tier = normalizeTier(tierFromItem);
        } else if (tier.isBlank()) {
            tier = inferTierFromItemId(itemId);
        }
        if (tier.isBlank()) {
            tier = "mud";
        }

        String jarId = fallbackJarId == null ? "" : fallbackJarId.trim().toLowerCase(Locale.ROOT);
        if (jarId.isBlank()) {
            jarId = "jar_" + Math.abs((itemId == null ? stack.hashCode() : itemId.toString().hashCode()));
        }

        CustomData currentData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = currentData == null ? new CompoundTag() : currentData.copyTag();
        CompoundTag jarTag = net.Gabou.identity2.util.NbtCompat.getCompoundOr(root, ITEM_SOUL_JAR_KEY, new CompoundTag());
        jarTag.putString("jar_id", jarId);
        jarTag.putString("tier", tier);
        net.Gabou.identity2.util.NbtCompat.store(jarTag, CHARGE_STORAGE_KEY, STRING_INT_MAP_CODEC, Map.of());
        root.put(ITEM_SOUL_JAR_KEY, jarTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return read(stack);
    }

    public static boolean writeCharges(ItemStack stack, Map<String, Integer> charges) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData currentData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (currentData == null || currentData.isEmpty()) {
            return false;
        }
        CompoundTag root = currentData.copyTag();
        CompoundTag jarTag = net.Gabou.identity2.util.NbtCompat.getCompoundOrNull(root, ITEM_SOUL_JAR_KEY);
        if (jarTag == null || jarTag.isEmpty()) {
            return false;
        }

        net.Gabou.identity2.util.NbtCompat.store(jarTag, CHARGE_STORAGE_KEY, STRING_INT_MAP_CODEC, sanitize(charges));
        root.put(ITEM_SOUL_JAR_KEY, jarTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    private static Map<String, Integer> sanitize(Map<String, Integer> source) {
        Map<String, Integer> out = new HashMap<>();
        if (source == null) {
            return out;
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount > 0) {
                out.put(entry.getKey(), amount);
            }
        }
        return out;
    }

    private static String resolveTierFromConfiguredItem(ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        List<String> mappings = IdentitySettings.soulJarTierItems;
        if (mappings == null) {
            return null;
        }
        for (String raw : mappings) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            String tier = normalizeTier(raw.substring(0, separator));
            String idText = raw.substring(separator + 1).trim();
            if (idText.isBlank()) {
                continue;
            }
            ResourceLocation configuredId;
            try {
                configuredId = new ResourceLocation(idText);
            } catch (Exception exception) {
                continue;
            }
            if (configuredId.equals(itemId)) {
                return tier;
            }
        }
        return null;
    }

    private static String inferTierFromItemId(ResourceLocation itemId) {
        if (itemId == null) {
            return "";
        }
        String path = itemId.getPath();
        if ("mud_jar".equals(path)) {
            return "mud";
        }
        if ("glass_jar".equals(path)) {
            return "glass";
        }
        if ("reinforced_jar".equals(path)) {
            return "reinforced";
        }
        if ("endgame_jar".equals(path)) {
            return "true_soul";
        }
        return "";
    }

    private static String normalizeTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return "";
        }
        return tier.trim().toLowerCase(Locale.ROOT);
    }

    public record JarSnapshot(String jarId, String tier, Map<String, Integer> charges) {
    }
}


