package net.Gabou.identity2.identity;

import java.util.Collections;
import java.util.List;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class IdentityTraitTags {
    public static final TagKey<EntityType<?>> CAN_FLY = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("identity2", "can_fly")
    );

    public static final TagKey<EntityType<?>> VANILLA_CAN_FLY = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("minecraft", "can_fly")
    );

    public static final TagKey<EntityType<?>> CAN_BREATHE_UNDERWATER = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("identity2", "can_breathe_underwater")
    );

    public static final TagKey<EntityType<?>> BURNS_IN_DAYLIGHT = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("identity2", "burns_in_daylight")
    );

    private IdentityTraitTags() {
    }

    @Nullable
    public static Boolean resolveFlight(EntityType<?> type) {
        if (type == null) {
            return Boolean.FALSE;
        }

        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.removedFlyingEntities), typeId)) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.extraFlyingEntities), typeId)) {
            return Boolean.TRUE;
        }

        if (type.is(CAN_FLY) || type.is(VANILLA_CAN_FLY)) {
            return Boolean.TRUE;
        }

        // No explicit tag information: let caller fall back to heuristic method checks.
        return null;
    }

    public static Boolean resolveCanBreatheUnderwater(EntityType<?> type) {
        if (type == null) {
            return Boolean.FALSE;
        }

        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.removedAquaticEntities), typeId)) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.extraAquaticEntities), typeId)) {
            return Boolean.TRUE;
        }

        return type.is(CAN_BREATHE_UNDERWATER) || type.is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public static boolean burnsInDaylight(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        return type.is(BURNS_IN_DAYLIGHT) || type.is(EntityTypeTags.BURN_IN_DAYLIGHT);
    }

    private static boolean containsTypeId(List<String> entries, Identifier typeId) {
        String full = typeId.toString();
        String path = typeId.getPath();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String normalized = entry.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.equals(full) || normalized.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> nullToEmpty(List<String> entries) {
        return entries == null ? Collections.emptyList() : entries;
    }
}
