package net.Gabou.identity2.api;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.api.morph.IdentityMorphTickHandler;
import net.Gabou.identity2.api.variant.IdentityVariantAdapter;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.Gabou.identity2.util.NetworkCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IdentityApi {
    private static final Map<EntityType<?>, IdentityVariantAdapter> VARIANT_ADAPTERS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, CopyOnWriteArrayList<IdentityMorphTickHandler>> MORPH_TICK_HANDLERS = new ConcurrentHashMap<>();

    private IdentityApi() {
    }

    public static void registerBuiltinAbility(ResourceLocation id, BuiltinIdentityAbility ability) {
        PredefIdentityAbilities.register(id, ability);
    }

    public static void registerBuiltinAbility(EntityType<?> type, BuiltinIdentityAbility ability) {
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null.");
        }
        ResourceLocation id = EntityType.getKey(type);
        if (id == null) {
            throw new IllegalArgumentException("Entity type is not registered: " + type);
        }
        registerBuiltinAbility(id, ability);
    }

    public static void registerVariantAdapter(EntityType<?> type, IdentityVariantAdapter adapter) {
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null.");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("Variant adapter cannot be null.");
        }
        VARIANT_ADAPTERS.put(type, adapter);
    }

    @Nullable
    public static IdentityVariantAdapter getVariantAdapter(EntityType<?> type) {
        return type == null ? null : VARIANT_ADAPTERS.get(type);
    }

    public static CompoundTag extractVariantData(LivingEntity entity) {
        if (entity == null) {
            return new CompoundTag();
        }
        IdentityVariantAdapter adapter = getVariantAdapter(entity.getType());
        if (adapter == null) {
            return new CompoundTag();
        }
        try {
            CompoundTag extracted = adapter.extractVariantData(entity);
            return extracted == null ? new CompoundTag() : extracted.copy();
        } catch (Throwable throwable) {
            Identity2.LOGGER.error("Variant adapter extract failed for {}", EntityType.getKey(entity.getType()), throwable);
            return new CompoundTag();
        }
    }

    public static void applyVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null) {
            return;
        }
        IdentityVariantAdapter adapter = getVariantAdapter(entity.getType());
        if (adapter == null) {
            return;
        }
        try {
            adapter.applyVariantData(entity, variantNbt == null ? new CompoundTag() : variantNbt.copy());
        } catch (Throwable throwable) {
            Identity2.LOGGER.error("Variant adapter apply failed for {}", EntityType.getKey(entity.getType()), throwable);
        }
    }

    public static List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        IdentityVariantAdapter adapter = getVariantAdapter(type);
        if (adapter == null || level == null) {
            return List.of();
        }
        try {
            List<IdentityVariant> variants = adapter.discoverVariants(type, level);
            if (variants == null || variants.isEmpty()) {
                return List.of();
            }
            return List.copyOf(variants);
        } catch (Throwable throwable) {
            Identity2.LOGGER.error("Variant adapter discovery failed for {}", EntityType.getKey(type), throwable);
            return List.of();
        }
    }

    public static void registerMorphTickHandler(EntityType<?> type, IdentityMorphTickHandler handler) {
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Morph tick handler cannot be null.");
        }
        MORPH_TICK_HANDLERS.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public static void runMorphTickHandlers(Entity host, Entity currentMorph) {
        if (host == null || currentMorph == null) {
            return;
        }
        List<IdentityMorphTickHandler> handlers = MORPH_TICK_HANDLERS.get(currentMorph.getType());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        for (IdentityMorphTickHandler handler : handlers) {
            try {
                handler.tick(host, currentMorph);
            } catch (Throwable throwable) {
                Identity2.LOGGER.error("Morph tick handler failed for {}", EntityType.getKey(currentMorph.getType()), throwable);
            }
        }
    }

    @Nullable
    public static Entity getCurrentMorph(Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return null;
        }
        return accessor.getCurrentIdentity();
    }

    @Nullable
    public static ResourceLocation getCurrentMorphId(Entity entity) {
        if (entity == null) {
            return null;
        }
        CompoundTag nbt = getCustomDataTag(entity);
        String raw = NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (raw.isBlank()) {
            raw = NbtCompat.getStringOr(nbt, "model_override", "");
        }
        if (raw.isBlank() || IdentityProgression.PLAYER_IDENTITY_ID.toString().equals(raw)) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static CompoundTag getCurrentMorphVariant(Entity entity) {
        CompoundTag nbt = getCustomDataTag(entity);
        return IdentityProgression.parseVariantNbt(
            NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "")
        );
    }

    public static boolean isMorphed(Entity entity) {
        return getCurrentMorphId(entity) != null;
    }

    public static void updateCurrentMorphVariant(ServerPlayer player, CompoundTag variantNbt) {
        IdentityProgression.updateCurrentVariantAndSync(player, variantNbt);
    }

    public static void syncBoolean(ServerPlayer player, String key, boolean value) {
        if (!shouldWriteBoolean(player, key, value)) {
            return;
        }
        CustomEntityBoolDataS2CPacketPayload payload = new CustomEntityBoolDataS2CPacketPayload(
            player.getId(),
            List.of(new CustomEntityDataS2CPacket.EntryBool(key, value))
        );
        broadcast(player, payload);
    }

    public static void syncDouble(ServerPlayer player, String key, double value) {
        if (!shouldWriteDouble(player, key, value)) {
            return;
        }
        CustomEntityDataS2CPacketPayload payload = new CustomEntityDataS2CPacketPayload(
            player.getId(),
            List.of(new CustomEntityDataS2CPacket.Entry(key, value))
        );
        broadcast(player, payload);
    }

    public static void syncString(ServerPlayer player, String key, String value) {
        if (!shouldWriteString(player, key, value)) {
            return;
        }
        CustomEntityStringDataS2CPacketPayload payload = new CustomEntityStringDataS2CPacketPayload(
            player.getId(),
            List.of(new CustomEntityDataS2CPacket.EntryString(key, value))
        );
        broadcast(player, payload);
    }

    private static boolean shouldWriteBoolean(ServerPlayer player, String key, boolean value) {
        if (player == null || key == null || key.isBlank()) {
            return false;
        }
        CompoundTag nbt = getCustomDataTag(player);
        boolean current = NbtCompat.getBooleanOr(nbt, key, !value);
        if (current == value && nbt.contains(key)) {
            return false;
        }
        nbt.putBoolean(key, value);
        return true;
    }

    private static boolean shouldWriteDouble(ServerPlayer player, String key, double value) {
        if (player == null || key == null || key.isBlank()) {
            return false;
        }
        CompoundTag nbt = getCustomDataTag(player);
        double current = NbtCompat.getDoubleOr(nbt, key, Double.NaN);
        if (!Double.isNaN(current) && Double.compare(current, value) == 0) {
            return false;
        }
        nbt.putDouble(key, value);
        return true;
    }

    private static boolean shouldWriteString(ServerPlayer player, String key, String value) {
        if (player == null || key == null || key.isBlank()) {
            return false;
        }
        String safeValue = value == null ? "" : value;
        CompoundTag nbt = getCustomDataTag(player);
        String current = NbtCompat.getStringOr(nbt, key, "\u0000");
        if (safeValue.equals(current)) {
            return false;
        }
        nbt.putString(key, safeValue);
        return true;
    }

    private static void broadcast(ServerPlayer player, Object payload) {
        if (player == null || payload == null) {
            return;
        }
        if (payload instanceof CustomEntityBoolDataS2CPacketPayload boolPayload) {
            NetworkCompat.sendToPlayer(player, boolPayload);
            for (ServerPlayer other : trackedPlayers(player)) {
                NetworkCompat.sendToPlayer(other, boolPayload);
            }
            return;
        }
        if (payload instanceof CustomEntityDataS2CPacketPayload doublePayload) {
            NetworkCompat.sendToPlayer(player, doublePayload);
            for (ServerPlayer other : trackedPlayers(player)) {
                NetworkCompat.sendToPlayer(other, doublePayload);
            }
            return;
        }
        if (payload instanceof CustomEntityStringDataS2CPacketPayload stringPayload) {
            NetworkCompat.sendToPlayer(player, stringPayload);
            for (ServerPlayer other : trackedPlayers(player)) {
                NetworkCompat.sendToPlayer(other, stringPayload);
            }
        }
    }

    private static List<ServerPlayer> trackedPlayers(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        List<ServerPlayer> tracked = new ArrayList<>();
        for (ServerPlayer other : serverLevel.players()) {
            if (other != player) {
                tracked.add(other);
            }
        }
        return tracked;
    }

    private static CompoundTag getCustomDataTag(Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return new CompoundTag();
        }
        CompoundTag customData = accessor.getCustomData();
        return customData == null ? new CompoundTag() : customData;
    }
}
