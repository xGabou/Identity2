package net.Gabou.identity2.util;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public final class NbtCompat {

    private NbtCompat() {}

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        if (tag == null || key == null) {
            return fallback;
        }
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public static double getDoubleOr(CompoundTag tag, String key, double fallback) {
        if (tag == null || key == null) {
            return fallback;
        }
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : fallback;
    }
    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        if (tag == null || key == null) {
            return fallback;
        }
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        if (tag == null || key == null) {
            return fallback;
        }
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : fallback;
    }

    public static CompoundTag getCompoundOr(CompoundTag tag, String key, CompoundTag fallback) {
        if (tag == null || key == null) {
            return fallback;
        }
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : fallback;
    }

    public static CompoundTag getCompoundOrNull(CompoundTag tag, String key) {
        return getCompoundOr(tag, key, null);
    }

    public static Set<String> keySet(CompoundTag tag) {
        if (tag == null) {
            return Collections.emptySet();
        }
        return tag.getAllKeys();
    }

    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        if (tag == null || key == null || codec == null) {
            return Optional.empty();
        }
        Tag stored = tag.get(key);
        if (stored == null) {
            return Optional.empty();
        }
        return codec.parse(NbtOps.INSTANCE, stored).result();
    }

    public static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value) {
        if (tag == null || key == null || codec == null) {
            return;
        }
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }
}
