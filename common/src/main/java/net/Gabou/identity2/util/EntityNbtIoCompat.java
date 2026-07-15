package net.Gabou.identity2.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Remap-safe entity NBT access for the 1.21.1 branch.
 *
 * <p>Do not replace these direct calls with reflection by mapped method name.
 * Loader remappers transform direct bytecode references for production jars;
 * string literals such as {@code "saveWithoutId"} are not transformed.</p>
 */
public final class EntityNbtIoCompat {
    private EntityNbtIoCompat() {
    }

    public static CompoundTag saveWithoutId(@Nullable Entity entity) {
        return entity == null ? new CompoundTag() : entity.saveWithoutId(new CompoundTag());
    }

    public static boolean load(@Nullable Entity entity, @Nullable CompoundTag nbt, @Nullable Object registryAccess) {
        if (entity == null || nbt == null) {
            return false;
        }
        entity.load(nbt);
        return true;
    }
}
