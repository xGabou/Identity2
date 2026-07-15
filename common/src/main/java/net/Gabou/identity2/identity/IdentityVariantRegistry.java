package net.Gabou.identity2.identity;

import dev.architectury.networking.NetworkManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.packets.IdentityVariantDefinitionS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Server-authoritative variant definitions addressed by deterministic UUIDs.
 */
public final class IdentityVariantRegistry {
    public static final int DEFINITION_CHUNK_BYTES = 20_000;
    public static final int MAX_DEFINITION_BYTES = 4 * 1024 * 1024;
    public static final int MAX_DEFINITION_CHUNKS =
        (MAX_DEFINITION_BYTES + DEFINITION_CHUNK_BYTES - 1) / DEFINITION_CHUNK_BYTES;

    private static final Map<UUID, Map<VariantKey, CompoundTag>> SERVER_DEFINITIONS = new ConcurrentHashMap<>();

    private IdentityVariantRegistry() {
    }

    public static CompoundTag normalize(CompoundTag variantNbt) {
        return IdentityProgression.normalizeVariantForUnlock(variantNbt == null ? new CompoundTag() : variantNbt);
    }

    public static String stableId(ResourceLocation identityId, CompoundTag variantNbt) {
        if (identityId == null) {
            return "";
        }
        CompoundTag normalized = normalize(variantNbt);
        if (normalized.isEmpty()) {
            return "";
        }
        String material = identityId + "\u0000" + canonicalTag(normalized);
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String remember(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        String variantId = stableId(identityId, variantNbt);
        if (player == null || variantId.isEmpty()) {
            return variantId;
        }
        SERVER_DEFINITIONS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
            .put(new VariantKey(identityId, variantId), normalize(variantNbt));
        return variantId;
    }

    public static CompoundTag resolve(ServerPlayer player, ResourceLocation identityId, String variantId) {
        if (player == null || identityId == null || variantId == null || variantId.isBlank()) {
            return new CompoundTag();
        }
        try {
            UUID.fromString(variantId);
        } catch (IllegalArgumentException invalidReference) {
            return null;
        }

        VariantKey key = new VariantKey(identityId, variantId);
        Map<VariantKey, CompoundTag> definitions = SERVER_DEFINITIONS.computeIfAbsent(
            player.getUUID(),
            ignored -> new ConcurrentHashMap<>()
        );
        CompoundTag cached = definitions.get(key);
        if (cached != null) {
            return cached.copy();
        }

        CompoundTag customData = ((EntityAccessor) player).getCustomData();
        String currentType = NbtCompat.getStringOr(customData, IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (identityId.toString().equals(currentType)) {
            CompoundTag current = IdentityProgression.parseVariantNbt(
                NbtCompat.getStringOr(customData, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "")
            );
            if (variantId.equals(stableId(identityId, current))) {
                definitions.put(key, normalize(current));
                return normalize(current);
            }
        }

        for (CompoundTag unlocked : IdentityProgression.getUnlockedVariantData(player, identityId)) {
            if (variantId.equals(stableId(identityId, unlocked))) {
                definitions.put(key, normalize(unlocked));
                return normalize(unlocked);
            }
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(identityId);
        if (type != null) {
            for (IdentityVariant variant : IdentityApi.discoverCommandVariants(type, player.level())) {
                CompoundTag data = variant == null ? null : variant.variantNbt();
                if (data != null && variantId.equals(stableId(identityId, data))) {
                    definitions.put(key, normalize(data));
                    return normalize(data);
                }
            }
        }
        return null;
    }

    public static void forget(ServerPlayer player) {
        if (player != null) {
            SERVER_DEFINITIONS.remove(player.getUUID());
        }
    }

    public static void sendReset(ServerPlayer target, int entityId) {
        if (target == null) {
            return;
        }
        NetworkManager.sendToPlayer(target, new IdentityVariantDefinitionS2CPacketPayload(
            entityId, true, "", "", 0, 0, new byte[0]
        ));
    }

    /**
     * Sends a definition in fragments below the serverbound custom-payload ceiling.
     * Returns the reference sent to the client, or an empty reference for default/oversized data.
     */
    public static String sendDefinition(
        ServerPlayer target,
        int entityId,
        ResourceLocation identityId,
        CompoundTag variantNbt,
        boolean reset
    ) {
        if (target == null || identityId == null) {
            return "";
        }
        CompoundTag normalized = normalize(variantNbt);
        String variantId = stableId(identityId, normalized);
        if (variantId.isEmpty()) {
            if (reset) {
                sendReset(target, entityId);
            }
            return "";
        }

        byte[] encoded = IdentityProgression.serializeVariantNbt(normalized).getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_DEFINITION_BYTES) {
            Identity2.LOGGER.warn(
                "Variant definition {} for {} is {} bytes, above the {} byte registry limit; using the default variant.",
                variantId,
                identityId,
                encoded.length,
                MAX_DEFINITION_BYTES
            );
            if (target.getId() == entityId) {
                IdentityProgression.notifyPlayerOversizedIdentityPayload(target);
            }
            if (reset) {
                sendReset(target, entityId);
            }
            return "";
        }

        int chunkCount = Math.max(1, (encoded.length + DEFINITION_CHUNK_BYTES - 1) / DEFINITION_CHUNK_BYTES);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int start = chunkIndex * DEFINITION_CHUNK_BYTES;
            int end = Math.min(encoded.length, start + DEFINITION_CHUNK_BYTES);
            NetworkManager.sendToPlayer(target, new IdentityVariantDefinitionS2CPacketPayload(
                entityId,
                reset && chunkIndex == 0,
                identityId.toString(),
                variantId,
                chunkIndex,
                chunkCount,
                Arrays.copyOfRange(encoded, start, end)
            ));
        }
        return variantId;
    }

    private static String canonicalTag(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            keys.sort(Comparator.naturalOrder());
            StringBuilder result = new StringBuilder("{");
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                String key = keys.get(index);
                result.append(key.length()).append(':').append(key).append('=').append(canonicalTag(compound.get(key)));
            }
            return result.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < list.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                result.append(canonicalTag(list.get(index)));
            }
            return result.append(']').toString();
        }
        return tag == null ? "null" : tag.toString();
    }

    private record VariantKey(ResourceLocation identityId, String variantId) {
    }
}
