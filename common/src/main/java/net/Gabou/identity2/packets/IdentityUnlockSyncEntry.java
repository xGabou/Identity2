package net.Gabou.identity2.packets;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record IdentityUnlockSyncEntry(ResourceLocation identityId, boolean replaceTokens, List<CompoundTag> variantData) {
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityUnlockSyncEntry> CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        IdentityUnlockSyncEntry::identityId,
        ByteBufCodecs.BOOL,
        IdentityUnlockSyncEntry::replaceTokens,
        ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()),
        IdentityUnlockSyncEntry::variantData,
        IdentityUnlockSyncEntry::new
    );
}
