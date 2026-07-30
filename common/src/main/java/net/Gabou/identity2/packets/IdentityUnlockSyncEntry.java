package net.Gabou.identity2.packets;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record IdentityUnlockSyncEntry(Identifier identityId, boolean replaceTokens, List<String> variantTokens) {
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityUnlockSyncEntry> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        IdentityUnlockSyncEntry::identityId,
        ByteBufCodecs.BOOL,
        IdentityUnlockSyncEntry::replaceTokens,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        IdentityUnlockSyncEntry::variantTokens,
        IdentityUnlockSyncEntry::new
    );
}
