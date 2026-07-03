package net.Gabou.identity2.packets;

import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IdentityUnlockSyncS2CPacketPayload(int entityid, boolean replaceAll, List<IdentityUnlockSyncEntry> entries)
    implements CustomPacketPayload {
    public static final Type<IdentityUnlockSyncS2CPacketPayload> ID = new Type<>(ModPackets.UNLOCK_SYNC_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityUnlockSyncS2CPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        IdentityUnlockSyncS2CPacketPayload::entityid,
        ByteBufCodecs.BOOL,
        IdentityUnlockSyncS2CPacketPayload::replaceAll,
        IdentityUnlockSyncEntry.CODEC.apply(ByteBufCodecs.list()),
        IdentityUnlockSyncS2CPacketPayload::entries,
        IdentityUnlockSyncS2CPacketPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
