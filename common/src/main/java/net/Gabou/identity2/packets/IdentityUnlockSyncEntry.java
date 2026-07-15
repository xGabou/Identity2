package net.Gabou.identity2.packets;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record IdentityUnlockSyncEntry(ResourceLocation identityId, boolean replaceTokens, List<String> variantIds) {
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityUnlockSyncEntry> CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        IdentityUnlockSyncEntry::identityId,
        ByteBufCodecs.BOOL,
        IdentityUnlockSyncEntry::replaceTokens,
        ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list()),
        IdentityUnlockSyncEntry::variantIds,
        IdentityUnlockSyncEntry::new
    );
}
