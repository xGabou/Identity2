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

    public static final TagKey<EntityType<?>> SLOW_FALLING = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("identity2", "slow_falling")
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

        Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(CAN_FLY), tagId(VANILLA_CAN_FLY));
        if (assignmentOverride != null) {
            return assignmentOverride;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.removedFlyingEntities), typeId)) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.extraFlyingEntities), typeId)) {
            return Boolean.TRUE;
        }

        if (type.builtInRegistryHolder().is(CAN_FLY) || type.builtInRegistryHolder().is(VANILLA_CAN_FLY)) {
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

        Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(CAN_BREATHE_UNDERWATER), tagId(EntityTypeTags.CAN_BREATHE_UNDER_WATER));
        if (assignmentOverride != null) {
            return assignmentOverride;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.removedAquaticEntities), typeId)) {
            return Boolean.FALSE;
        }

        if (containsTypeId(nullToEmpty(IdentitySettings.extraAquaticEntities), typeId)) {
            return Boolean.TRUE;
        }

        return type.builtInRegistryHolder().is(CAN_BREATHE_UNDERWATER)
                || type.builtInRegistryHolder().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public static boolean burnsInDaylight(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId != null) {
            Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(BURNS_IN_DAYLIGHT), tagId(EntityTypeTags.BURN_IN_DAYLIGHT));
            if (assignmentOverride != null) {
                return assignmentOverride;
            }
        }
        return type.builtInRegistryHolder().is(BURNS_IN_DAYLIGHT)
                || type.builtInRegistryHolder().is(EntityTypeTags.BURN_IN_DAYLIGHT);
    }

    public static boolean hasSlowFalling(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId != null) {
            Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(SLOW_FALLING));
            if (assignmentOverride != null) {
                return assignmentOverride;
            }
        }
        return type.builtInRegistryHolder().is(SLOW_FALLING);
    }

    @Nullable
    private static Boolean resolveAssignmentOverride(Identifier typeId, Identifier... acceptedTagIds) {
        if (typeId == null || acceptedTagIds == null || acceptedTagIds.length == 0) {
            return null;
        }

        List<String> removed = nullToEmpty(IdentitySettings.removedEntityTypeTagAssignments);
        for (String raw : removed) {
            TagAssignment assignment = parseTagAssignment(raw);
            if (assignment == null) {
                continue;
            }
            if (matchesTag(assignment.tagId(), acceptedTagIds) && matchesType(assignment.entityTypeId(), typeId)) {
                return Boolean.FALSE;
            }
        }

        List<String> extra = nullToEmpty(IdentitySettings.extraEntityTypeTagAssignments);
        for (String raw : extra) {
            TagAssignment assignment = parseTagAssignment(raw);
            if (assignment == null) {
                continue;
            }
            if (matchesTag(assignment.tagId(), acceptedTagIds) && matchesType(assignment.entityTypeId(), typeId)) {
                return Boolean.TRUE;
            }
        }

        return null;
    }

    private static boolean matchesTag(Identifier entryTag, Identifier[] acceptedTagIds) {
        if (entryTag == null) {
            return false;
        }
        String entryFull = entryTag.toString();
        String entryPath = entryTag.getPath();
        for (Identifier accepted : acceptedTagIds) {
            if (accepted == null) {
                continue;
            }
            if (entryFull.equals(accepted.toString()) || entryPath.equals(accepted.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesType(Identifier entryType, Identifier typeId) {
        if (entryType == null || typeId == null) {
            return false;
        }
        return entryType.toString().equals(typeId.toString()) || entryType.getPath().equals(typeId.getPath());
    }

    @Nullable
    private static TagAssignment parseTagAssignment(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        int separator = normalized.indexOf('=');
        if (separator <= 0 || separator >= normalized.length() - 1) {
            return null;
        }

        Identifier tagId = parseIdentifierLoose(normalized.substring(0, separator).trim());
        Identifier entityTypeId = parseIdentifierLoose(normalized.substring(separator + 1).trim());
        if (tagId == null || entityTypeId == null) {
            return null;
        }
        return new TagAssignment(tagId, entityTypeId);
    }

    @Nullable
    private static Identifier parseIdentifierLoose(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.contains(":")) {
                return Identifier.parse(value);
            }
            return Identifier.fromNamespaceAndPath("minecraft", value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Identifier tagId(TagKey<EntityType<?>> tag) {
        return tag.location();
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

    private record TagAssignment(Identifier tagId, Identifier entityTypeId) {
    }
}
