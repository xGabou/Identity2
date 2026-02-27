package net.Gabou.identity2.packets;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class CustomEntityDataS2CPacket {
    private CustomEntityDataS2CPacket() {
    }

    public static List<Entry> readDoubleEntries(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buffer.readUtf(), buffer.readDouble()));
        }
        return entries;
    }

    public static void writeDoubleEntries(FriendlyByteBuf buffer, List<Entry> entries) {
        List<Entry> safeEntries = entries == null ? List.of() : entries;
        buffer.writeVarInt(safeEntries.size());
        for (Entry entry : safeEntries) {
            buffer.writeUtf(entry.key());
            buffer.writeDouble(entry.value());
        }
    }

    public static List<EntryString> readStringEntries(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<EntryString> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            entries.add(new EntryString(buffer.readUtf(), buffer.readUtf()));
        }
        return entries;
    }

    public static void writeStringEntries(FriendlyByteBuf buffer, List<EntryString> entries) {
        List<EntryString> safeEntries = entries == null ? List.of() : entries;
        buffer.writeVarInt(safeEntries.size());
        for (EntryString entry : safeEntries) {
            buffer.writeUtf(entry.key());
            buffer.writeUtf(entry.value());
        }
    }

    public static List<EntryBool> readBoolEntries(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<EntryBool> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            entries.add(new EntryBool(buffer.readUtf(), buffer.readBoolean()));
        }
        return entries;
    }

    public static void writeBoolEntries(FriendlyByteBuf buffer, List<EntryBool> entries) {
        List<EntryBool> safeEntries = entries == null ? List.of() : entries;
        buffer.writeVarInt(safeEntries.size());
        for (EntryBool entry : safeEntries) {
            buffer.writeUtf(entry.key());
            buffer.writeBoolean(entry.value());
        }
    }

    public record Entry(String key, double value) {
    }

    public record EntryString(String key, String value) {
    }

    public record EntryBool(String key, boolean value) {
    }
}
