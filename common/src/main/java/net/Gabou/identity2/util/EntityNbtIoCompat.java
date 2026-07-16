package net.Gabou.identity2.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Stable 1.20.1 entity-NBT bridge; these mapped methods are public. */
public final class EntityNbtIoCompat {
    private EntityNbtIoCompat() {
    }

    public static CompoundTag saveWithoutId(@Nullable Entity entity) {
        return entity == null ? new CompoundTag() : entity.saveWithoutId(new CompoundTag());
    }

    public static boolean load(@Nullable Entity entity, @Nullable CompoundTag nbt, @Nullable Object ignoredRegistryAccess) {
        if (entity == null || nbt == null) {
            return false;
        }
        entity.load(nbt);
        return true;
    }
}
