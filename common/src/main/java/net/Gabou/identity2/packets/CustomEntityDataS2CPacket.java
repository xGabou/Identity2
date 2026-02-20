package net.Gabou.identity2.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class CustomEntityDataS2CPacket {
    private CustomEntityDataS2CPacket() {
    }

    public record Entry(String key, double value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            Entry::key,
            ByteBufCodecs.DOUBLE,
            Entry::value,
            Entry::new
        );
    }

    public record EntryString(String key, String value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryString> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            EntryString::key,
            ByteBufCodecs.STRING_UTF8,
            EntryString::value,
            EntryString::new
        );
    }

    public record EntryBool(String key, boolean value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryBool> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            EntryBool::key,
            ByteBufCodecs.BOOL,
            EntryBool::value,
            EntryBool::new
        );
    }
}
