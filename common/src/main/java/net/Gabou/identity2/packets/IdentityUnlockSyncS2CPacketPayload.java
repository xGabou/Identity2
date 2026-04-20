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
        int size = buffer.readVarInt();
        List<IdentityUnlockSyncEntry> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            ResourceLocation identityId = buffer.readResourceLocation();
            boolean replaceTokens = buffer.readBoolean();
            int tokenCount = buffer.readVarInt();
            List<String> tokens = new ArrayList<>(Math.max(0, tokenCount));
            for (int j = 0; j < tokenCount; j++) {
                tokens.add(buffer.readUtf());
            }
            entries.add(new IdentityUnlockSyncEntry(identityId, replaceTokens, tokens));
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
            List<String> tokens = entry.variantTokens() == null ? List.of() : entry.variantTokens();
            buffer.writeVarInt(tokens.size());
            for (String token : tokens) {
                buffer.writeUtf(token == null ? "" : token);
            }
        }
    }
}
