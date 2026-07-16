package net.Gabou.identity2.client.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/** Client variant discovery backed by the same typed API as commands. */
public final class IdentityVariantDiscovery {
    private IdentityVariantDiscovery() {
    }

    public static List<IdentityVariant> discover(EntityType<?> type, ClientLevel level) {
        if (type == null || level == null) {
            return List.of();
        }
        ResourceLocation typeId = EntityType.getKey(type);
        if (typeId == null) {
            return List.of();
        }

        Map<String, IdentityVariant> variants = new LinkedHashMap<>();
        boolean hasBaby = false;
        for (IdentityVariant discovered : IdentityApi.discoverVariants(type, level)) {
            if (discovered == null || discovered.variantNbt() == null || discovered.variantNbt().isEmpty()) {
                continue;
            }
            CompoundTag normalized = IdentityProgression.normalizeVariantForUnlock(discovered.variantNbt());
            if (normalized.isEmpty()) {
                continue;
            }
            IdentityVariant variant = new IdentityVariant(typeId, discovered.displayName(), normalized);
            variants.putIfAbsent(IdentityProgression.toVariantUnlockToken(normalized), variant);
            hasBaby |= isBaby(normalized);
        }

        if (hasBaby) {
            variants.putIfAbsent("-", defaultVariant(typeId));
        }
        return variants.isEmpty() ? List.of(defaultVariant(typeId)) : new ArrayList<>(variants.values());
    }

    private static boolean isBaby(CompoundTag nbt) {
        return NbtCompat.getBooleanOr(nbt, "IsBaby", false)
                || NbtCompat.getBooleanOr(nbt, "Baby", false)
                || nbt.contains("Age", Tag.TAG_ANY_NUMERIC) && nbt.getInt("Age") < 0;
    }

    private static IdentityVariant defaultVariant(ResourceLocation typeId) {
        return new IdentityVariant(typeId, capitalize(typeId.getPath()), new CompoundTag());
    }

    private static String capitalize(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
