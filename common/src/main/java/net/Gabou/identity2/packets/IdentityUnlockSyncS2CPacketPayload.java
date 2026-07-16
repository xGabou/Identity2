package net.Gabou.identity2.packets;

import java.util.ArrayList;
import java.util.List;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record IdentityUnlockSyncS2CPacketPayload(int entityid, boolean replaceAll, List<IdentityUnlockSyncEntry> entries)
    implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.UNLOCK_SYNC_PACKET_ID;

    public static IdentityUnlockSyncS2CPacketPayload decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        boolean replaceAll = buffer.readBoolean();
        int size = Math.min(buffer.readVarInt(), 4096);
        List<IdentityUnlockSyncEntry> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            ResourceLocation identityId = buffer.readResourceLocation();
            boolean replaceTokens = buffer.readBoolean();
            int variantCount = Math.min(buffer.readVarInt(), 65536);
            List<String> variantIds = new ArrayList<>(Math.max(0, variantCount));
            for (int j = 0; j < variantCount; j++) {
                variantIds.add(buffer.readUtf(64));
            }
            entries.add(new IdentityUnlockSyncEntry(identityId, replaceTokens, variantIds));
        }
        return new IdentityUnlockSyncS2CPacketPayload(entityId, replaceAll, entries);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityid);
        buffer.writeBoolean(replaceAll);
        List<IdentityUnlockSyncEntry> safeEntries = entries == null ? List.of() : entries;
        buffer.writeVarInt(safeEntries.size());
        for (IdentityUnlockSyncEntry entry : safeEntries) {
            buffer.writeResourceLocation(entry.identityId());
            buffer.writeBoolean(entry.replaceTokens());
            List<String> variantIds = entry.variantIds() == null ? List.of() : entry.variantIds();
            buffer.writeVarInt(variantIds.size());
            for (String variantId : variantIds) {
                buffer.writeUtf(variantId == null ? "" : variantId, 64);
            }
        }
    }
}
