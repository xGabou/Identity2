package net.Gabou.identity2.api.variant;

import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IdentityVariantAdapter {
    /** When true, generic NBT/baby heuristics are skipped and this adapter owns the full variant token. */
    default boolean replacesGenericExtraction() {
        return false;
    }

    default CompoundTag extractVariantData(LivingEntity entity) {
        return new CompoundTag();
    }

    /**
     * Supplies safe constructor/load defaults before an entity reads its NBT. Optional mods may
     * unconditionally replace healthy constructor values with zero when a key is absent.
     */
    default CompoundTag prepareVariantData(CompoundTag variantNbt) {
        return variantNbt == null ? new CompoundTag() : variantNbt.copy();
    }

    default void applyVariantData(Entity entity, CompoundTag variantNbt) {
    }

    default List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        return List.of();
    }
}
