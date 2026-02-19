package net.Gabou.identity2.identity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public record IdentityVariant(Identifier entityTypeId, String displayName, NbtCompound variantNbt) {
    public IdentityVariant {
        if (entityTypeId == null) {
            throw new IllegalArgumentException("entityTypeId cannot be null");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = entityTypeId.toString();
        }
        variantNbt = variantNbt == null ? new NbtCompound() : variantNbt.copy();
    }
}

