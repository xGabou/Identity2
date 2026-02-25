package net.Gabou.identity2.identity;

import net.Gabou.identity2.util.EntityNbtIoCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public final class IdentityVariantNbtHelper {
    private static final Set<String> IGNORED_ROOT_KEYS = Set.of(
        "id",
        "UUID",
        "Pos",
        "Motion",
        "Rotation",
        "FallDistance",
        "Fire",
        "Air",
        "OnGround",
        "Invulnerable",
        "PortalCooldown",
        "TicksFrozen",
        "HasVisualFire",
        "Health",
        "AbsorptionAmount",
        "HurtTime",
        "HurtByTimestamp",
        "DeathTime",
        "EggLayTime",
        "SleepingX",
        "SleepingY",
        "SleepingZ",
        "Leash",
        "CannotEnterHiveTicks",
        "LeftHanded",
        "NoAI",
        "PersistenceRequired",
        "HandItems",
        "ArmorItems",
        "HandDropChances",
        "ArmorDropChances",
        "Attributes",
        "ActiveEffects",
        "Brain",
        "CanPickUpLoot",
        "fall_flying",
        "Silent",
        "Glowing",
        "Tags"
    );

    private IdentityVariantNbtHelper() {
    }

    public static CompoundTag computeVariantDiff(Entity entity) {
        CompoundTag empty = new CompoundTag();
        if (entity == null || entity.level() == null || entity.getType() == null) {
            return empty;
        }
        Entity baselineEntity;
        try {
            baselineEntity = entity.getType().create(entity.level());
        } catch (Throwable ignored) {
            baselineEntity = null;
        }
        if (baselineEntity == null) {
            return empty;
        }
        CompoundTag baseline = writeEntityData(baselineEntity);
        CompoundTag current = writeEntityData(entity);
        if (baseline == null || current == null) {
            return empty;
        }
        return computeVariantDiff(baseline, current);
    }

    public static CompoundTag computeVariantDiff(CompoundTag baseline, CompoundTag current) {
        CompoundTag out = new CompoundTag();
        if (current == null || current.isEmpty()) {
            return out;
        }
        CompoundTag safeBase = baseline == null ? new CompoundTag() : baseline;
        for (String key : current.getAllKeys()) {
            if (isIgnoredRootKey(key)) {
                continue;
            }
            Tag currentTag = current.get(key);
            if (currentTag == null) {
                continue;
            }
            Tag baseTag = safeBase.get(key);
            if (currentTag instanceof CompoundTag currentCompound) {
                CompoundTag baseCompound = baseTag instanceof CompoundTag baseValue ? baseValue : new CompoundTag();
                CompoundTag nested = computeVariantDiff(baseCompound, currentCompound);
                if (!nested.isEmpty()) {
                    out.put(key, nested);
                }
                continue;
            }
            if (!currentTag.equals(baseTag)) {
                out.put(key, currentTag.copy());
            }
        }
        return out;
    }

    public static CompoundTag writeEntityData(Entity entity) {
        if (entity == null || entity.level() == null) {
            return null;
        }
        return EntityNbtIoCompat.saveWithoutId(entity);
    }

    public static boolean loadEntityData(Entity entity, CompoundTag nbt) {
        if (entity == null || entity.level() == null || nbt == null) {
            return false;
        }
        return EntityNbtIoCompat.load(entity, nbt, entity.level().registryAccess());
    }

    public static void applyVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        CompoundTag current = writeEntityData(entity);
        if (current == null) {
            return;
        }
        CompoundTag merged = current.copy();
        merged.merge(variantNbt.copy());
        loadEntityData(entity, merged);
    }

    private static boolean isIgnoredRootKey(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        return IGNORED_ROOT_KEYS.contains(key);
    }
}
