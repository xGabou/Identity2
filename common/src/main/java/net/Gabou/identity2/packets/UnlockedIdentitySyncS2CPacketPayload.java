package net.Gabou.identity2.packets;

import java.util.List;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UnlockedIdentitySyncS2CPacketPayload(
        int entityid,
        List<String> unlockedIdentityIds,
        List<VariantEntry> unlockedVariantEntries
) implements CustomPacketPayload {
    public static final Type<UnlockedIdentitySyncS2CPacketPayload> ID =
            new Type<>(ModPackets.UNLOCKED_IDENTITY_SYNC_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockedIdentitySyncS2CPacketPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UnlockedIdentitySyncS2CPacketPayload::entityid,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    UnlockedIdentitySyncS2CPacketPayload::unlockedIdentityIds,
                    VariantEntry.CODEC.apply(ByteBufCodecs.list()),
                    UnlockedIdentitySyncS2CPacketPayload::unlockedVariantEntries,
                    UnlockedIdentitySyncS2CPacketPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public record VariantEntry(String identityId, List<String> variantTokens) {
        public static final StreamCodec<RegistryFriendlyByteBuf, VariantEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                VariantEntry::identityId,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                VariantEntry::variantTokens,
                VariantEntry::new
        );
    }
}
