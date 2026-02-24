package net.Gabou.identity2.identity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record IdentityVariant(ResourceLocation entityTypeId, String displayName, CompoundTag variantNbt) {
    public IdentityVariant {
        if (entityTypeId == null) {
            throw new IllegalArgumentException("entityTypeId cannot be null");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = entityTypeId.toString();
        }
        variantNbt = variantNbt == null ? new CompoundTag() : variantNbt.copy();
    }
}

