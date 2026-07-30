package net.Gabou.identity2.identity;

import dev.architectury.networking.NetworkManager;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.packets.IdentityVariantDefinitionS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/** Per-player server cache for UUID-addressed variant definitions. */
public final class IdentityVariantRegistry {
    public static final int DEFINITION_CHUNK_BYTES = IdentityVariantDefinitionS2CPacketPayload.MAX_CHUNK_BYTES;
    public static final int MAX_DEFINITION_BYTES = 4 * 1024 * 1024;
    public static final int MAX_DEFINITION_CHUNKS = (MAX_DEFINITION_BYTES + DEFINITION_CHUNK_BYTES - 1) / DEFINITION_CHUNK_BYTES;
    private static final Map<UUID, Map<VariantKey, CompoundTag>> DEFINITIONS = new ConcurrentHashMap<>();
    private IdentityVariantRegistry() {}
    public static CompoundTag normalize(CompoundTag nbt) { return IdentityProgression.normalizeVariantForUnlock(nbt == null ? new CompoundTag() : nbt); }
    public static String stableId(Identifier identityId, CompoundTag variant) {
        if (identityId == null) return "";
        CompoundTag normalized = normalize(variant);
        return normalized.isEmpty() ? "" : UUID.nameUUIDFromBytes((identityId + "\0" + canonical(normalized)).getBytes(StandardCharsets.UTF_8)).toString();
    }
    public static String remember(ServerPlayer player, Identifier id, CompoundTag variant) {
        String ref = stableId(id, variant);
        if (player != null && !ref.isEmpty()) DEFINITIONS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>()).put(new VariantKey(id, ref), normalize(variant));
        return ref;
    }
    public static CompoundTag resolve(ServerPlayer player, Identifier id, String ref) {
        if (player == null || id == null || ref == null || ref.isBlank()) return new CompoundTag();
        try { UUID.fromString(ref); } catch (IllegalArgumentException e) { return null; }
        VariantKey key = new VariantKey(id, ref);
        Map<VariantKey, CompoundTag> cache = DEFINITIONS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        CompoundTag known = cache.get(key); if (known != null) return known.copy();
        CompoundTag custom = ((NbtComponentAccessor)(Object)((EntityAccessor)player).getCustomData()).getNbt();
        if (id.toString().equals(custom.getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, ""))) {
            CompoundTag current = IdentityProgression.parseVariantNbt(custom.getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, ""));
            if (ref.equals(stableId(id, current))) { cache.put(key, normalize(current)); return normalize(current); }
        }
        for (CompoundTag candidate : IdentityProgression.getUnlockedVariantData(player, id)) if (ref.equals(stableId(id, candidate))) { cache.put(key, normalize(candidate)); return normalize(candidate); }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        if (type != null) for (IdentityVariant candidate : IdentityApi.discoverVariants(type, player.level())) {
            CompoundTag data = candidate == null ? null : candidate.variantNbt();
            if (data != null && ref.equals(stableId(id, data))) { cache.put(key, normalize(data)); return normalize(data); }
        }
        return null;
    }
    public static void forget(ServerPlayer p) { if (p != null) DEFINITIONS.remove(p.getUUID()); }
    public static String sendDefinition(ServerPlayer target, int entityId, Identifier id, CompoundTag variant, boolean reset) {
        if (target == null || id == null) return "";
        CompoundTag normalized = normalize(variant); String ref = stableId(id, normalized);
        if (ref.isEmpty()) { if (reset) sendReset(target, entityId); return ""; }
        byte[] bytes = IdentityProgression.serializeVariantNbt(normalized).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_DEFINITION_BYTES) { Identity2.LOGGER.warn("Variant definition {} exceeds {} bytes", ref, MAX_DEFINITION_BYTES); if (reset) sendReset(target, entityId); return ""; }
        int count = Math.max(1, (bytes.length + DEFINITION_CHUNK_BYTES - 1) / DEFINITION_CHUNK_BYTES);
        for (int i=0;i<count;i++) { int start=i*DEFINITION_CHUNK_BYTES, end=Math.min(bytes.length,start+DEFINITION_CHUNK_BYTES); NetworkManager.sendToPlayer(target, new IdentityVariantDefinitionS2CPacketPayload(entityId, reset && i==0, id.toString(), ref, i, count, Arrays.copyOfRange(bytes,start,end))); }
        return ref;
    }
    public static void sendReset(ServerPlayer target, int entityId) { if (target != null) NetworkManager.sendToPlayer(target, new IdentityVariantDefinitionS2CPacketPayload(entityId,true,"","",0,0,new byte[0])); }
    private static String canonical(Tag tag) {
        if (tag instanceof CompoundTag c) { List<String> keys=new ArrayList<>(c.keySet()); Collections.sort(keys); StringBuilder s=new StringBuilder("{"); for (String k:keys) s.append(k.length()).append(':').append(k).append('=').append(canonical(c.get(k))).append(';'); return s.append('}').toString(); }
        if (tag instanceof ListTag l) { StringBuilder s=new StringBuilder("["); for (Tag t:l) s.append(canonical(t)).append(';'); return s.append(']').toString(); }
        return tag == null ? "null" : tag.toString();
    }
    private record VariantKey(Identifier identityId, String variantId) {}
}
