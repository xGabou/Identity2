package net.Gabou.identity2.api.variant;

import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IdentityVariantAdapter {
    default CompoundTag extractVariantData(LivingEntity entity) {
        return new CompoundTag();
    }

    default void applyVariantData(Entity entity, CompoundTag variantNbt) {
    }

    default List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        return List.of();
    }
}
