package net.Gabou.identity2.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public final class CustomEntityDataS2CPacket {
    private CustomEntityDataS2CPacket() {
    }

    public record Entry(String key, double value) {
        public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            Entry::key,
            PacketCodecs.DOUBLE,
            Entry::value,
            Entry::new
        );
    }

    public record EntryString(String key, String value) {
        public static final PacketCodec<RegistryByteBuf, EntryString> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            EntryString::key,
            PacketCodecs.STRING,
            EntryString::value,
            EntryString::new
        );
    }

    public record EntryBool(String key, boolean value) {
        public static final PacketCodec<RegistryByteBuf, EntryBool> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            EntryBool::key,
            PacketCodecs.BOOLEAN,
            EntryBool::value,
            EntryBool::new
        );
    }
}
