package net.Gabou.identity2.identity;

import java.util.Collections;
import java.util.List;
import net.Gabou.identity2.IdentitySettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class IdentityTraitTags {
    public static final TagKey<EntityType<?>> CAN_FLY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "can_fly")
    );

    public static final TagKey<EntityType<?>> CANNOT_FLY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "cannot_fly")
    );

    public static final TagKey<EntityType<?>> VANILLA_CAN_FLY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("minecraft", "can_fly")
    );

    public static final TagKey<EntityType<?>> CAN_BREATHE_UNDERWATER = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "can_breathe_underwater")
    );

    public static final TagKey<EntityType<?>> BURNS_IN_DAYLIGHT = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "burns_in_daylight")
    );

    public static final TagKey<EntityType<?>> SLOW_FALLING = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "slow_falling")
    );

    public static final TagKey<EntityType<?>> HOSTILE_IGNORE_TARGETING = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "hostile_ignore_targeting")
    );

    public static final TagKey<EntityType<?>> INVALID_MORPH_MOUNT = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "invalid_morph_mount")
    );

    public static final TagKey<EntityType<?>> HIGH_JUMP_ABILITY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "high_jump_ability")
    );

    public static final TagKey<EntityType<?>> SECONDARY_HIGH_JUMP_ABILITY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "secondary_high_jump_ability")
    );

    public static final TagKey<EntityType<?>> RAM_ATTACK_ABILITY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "ram_attack_ability")
    );

    public static final TagKey<EntityType<?>> ROLL_ABILITY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "roll_ability")
    );

    public static final TagKey<EntityType<?>> DEFENSIVE_PUFF_ABILITY = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "defensive_puff_ability")
    );

    public static final TagKey<EntityType<?>> MELEE_IGNITES_TARGET = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("identity2", "melee_ignites_target")
    );

    private IdentityTraitTags() {
    }

    @Nullable
    public static Boolean resolveFlight(EntityType<?> type) {
        if (type == null) {
            return Boolean.FALSE;
        }

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null) {
            return Boolean.FALSE;
        }

        Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(CANNOT_FLY), tagId(CAN_FLY), tagId(VANILLA_CAN_FLY));
        if (assignmentOverride != null) {
            return assignmentOverride;
        }

        if (type.is(CANNOT_FLY)) {
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

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
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

        return type.is(CAN_BREATHE_UNDERWATER) || type.is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public static boolean burnsInDaylight(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId != null) {
            Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(BURNS_IN_DAYLIGHT));
            if (assignmentOverride != null) {
                return assignmentOverride;
            }
        }
        return type.is(BURNS_IN_DAYLIGHT);
    }

    public static boolean hasSlowFalling(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId != null) {
            Boolean assignmentOverride = resolveAssignmentOverride(typeId, tagId(SLOW_FALLING));
            if (assignmentOverride != null) {
                return assignmentOverride;
            }
        }
        return type.is(SLOW_FALLING);
    }

    public static boolean hostileIgnoresTargeting(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        return type.is(HOSTILE_IGNORE_TARGETING);
    }

    public static boolean preventsInvalidMorphMounting(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        return type.is(INVALID_MORPH_MOUNT);
    }

    public static boolean hasHighJumpAbility(EntityType<?> type) {
        return type != null && type.is(HIGH_JUMP_ABILITY);
    }

    public static boolean hasSecondaryHighJumpAbility(EntityType<?> type) {
        return type != null && type.is(SECONDARY_HIGH_JUMP_ABILITY);
    }

    public static boolean hasRamAttackAbility(EntityType<?> type) {
        return type != null && type.is(RAM_ATTACK_ABILITY);
    }

    public static boolean hasRollAbility(EntityType<?> type) {
        return type != null && type.is(ROLL_ABILITY);
    }

    public static boolean hasDefensivePuffAbility(EntityType<?> type) {
        return type != null && type.is(DEFENSIVE_PUFF_ABILITY);
    }

    public static boolean ignitesTargetsOnMelee(EntityType<?> type) {
        return type != null && type.is(MELEE_IGNITES_TARGET);
    }

    @Nullable
    private static Boolean resolveAssignmentOverride(ResourceLocation typeId, ResourceLocation... acceptedTagIds) {
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

    private static boolean matchesTag(ResourceLocation entryTag, ResourceLocation[] acceptedTagIds) {
        if (entryTag == null) {
            return false;
        }
        String entryFull = entryTag.toString();
        String entryPath = entryTag.getPath();
        for (ResourceLocation accepted : acceptedTagIds) {
            if (accepted == null) {
                continue;
            }
            if (entryFull.equals(accepted.toString()) || entryPath.equals(accepted.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesType(ResourceLocation entryType, ResourceLocation typeId) {
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

        ResourceLocation tagId = parseResourceLocationLoose(normalized.substring(0, separator).trim());
        ResourceLocation entityTypeId = parseResourceLocationLoose(normalized.substring(separator + 1).trim());
        if (tagId == null || entityTypeId == null) {
            return null;
        }
        return new TagAssignment(tagId, entityTypeId);
    }

    @Nullable
    private static ResourceLocation parseResourceLocationLoose(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.contains(":")) {
                return ResourceLocation.parse(value);
            }
            return ResourceLocation.fromNamespaceAndPath("minecraft", value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ResourceLocation tagId(TagKey<EntityType<?>> tag) {
        return tag.location();
    }

    private static boolean containsTypeId(List<String> entries, ResourceLocation typeId) {
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

    private record TagAssignment(ResourceLocation tagId, ResourceLocation entityTypeId) {
    }
}
