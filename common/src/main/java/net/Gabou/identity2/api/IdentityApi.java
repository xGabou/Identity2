package net.Gabou.identity2.api;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.api.morph.IdentityMorphTickHandler;
import net.Gabou.identity2.api.variant.IdentityVariantAdapter;
import net.Gabou.identity2.compat.UntamedWildsCompat;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.Gabou.identity2.util.NetworkCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
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
        if (type == null) {
            return null;
        }
        IdentityVariantAdapter adapter = VARIANT_ADAPTERS.get(type);
        if (adapter != null) {
            return adapter;
        }
        UntamedWildsCompat.ensureVariantAdapterRegistered(type);
        return VARIANT_ADAPTERS.get(type);
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
        if (adapter != null && level != null) {
            try {
                List<IdentityVariant> variants = adapter.discoverVariants(type, level);
                if (variants != null && !variants.isEmpty()) {
                    return List.copyOf(variants);
                }
            } catch (Throwable throwable) {
                Identity2.LOGGER.error("Variant adapter discovery failed for {}", EntityType.getKey(type), throwable);
            }
        }
        List<IdentityVariant> builtInVariants = discoverBuiltInVariants(type, level);
        if (!builtInVariants.isEmpty()) {
            return builtInVariants;
        }
        return List.of();
    }

    private static List<IdentityVariant> discoverBuiltInVariants(EntityType<?> type, Level level) {
        if (type == null) {
            return List.of();
        }
        ResourceLocation typeId = EntityType.getKey(type);
        if (typeId == null) {
            return List.of();
        }
        List<IdentityVariant> variants = new ArrayList<>();
        if (type == EntityType.CAT) {
            variants.addAll(discoverRegistryBackedVariants(typeId, "CAT_VARIANT", "CatVariant", "Cat"));
        } else if (type == EntityType.WOLF) {
            variants.addAll(discoverRegistryBackedVariants(typeId, "WOLF_VARIANT", "WolfVariant", "Wolf"));
        } else if (type == EntityType.FROG) {
            variants.addAll(discoverRegistryBackedVariants(typeId, "FROG_VARIANT", "FrogVariant", "Frog"));
        }
        IdentityVariant baby = discoverBabyVariant(type, typeId, level);
        if (baby != null) {
            variants.add(baby);
        }
        return variants.isEmpty() ? List.of() : variants;
    }

    private static IdentityVariant discoverBabyVariant(EntityType<?> type, ResourceLocation typeId, Level level) {
        if (type == null || typeId == null || level == null) {
            return null;
        }
        Entity probe;
        try {
            probe = type.create(level);
        } catch (Throwable ignored) {
            return null;
        }
        if (!(probe instanceof AgeableMob)) {
            return null;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("IsBaby", true);
        nbt.putInt("Age", -24000);
        return new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", nbt);
    }

    private static List<IdentityVariant> discoverRegistryBackedVariants(
        ResourceLocation typeId,
        String registryField,
        String variantKey,
        String labelPrefix
    ) {
        Registry<?> registry = resolveRegistry(registryField);
        if (registry == null || registry.keySet().isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> keys = new ArrayList<>(registry.keySet());
        keys.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        List<IdentityVariant> variants = new ArrayList<>(keys.size());
        for (ResourceLocation variantId : keys) {
            if (variantId == null) {
                continue;
            }
            CompoundTag nbt = new CompoundTag();
            nbt.putString(variantKey, variantId.toString());
            variants.add(new IdentityVariant(typeId, labelPrefix + " " + capitalize(variantId.getPath().replace('_', ' ')), nbt));
        }
        return variants;
    }

    private static Registry<?> resolveRegistry(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Object direct = BuiltInRegistries.class.getField(fieldName).get(null);
            if (direct instanceof Registry<?> registry) {
                return registry;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object key = Registries.class.getField(fieldName).get(null);
            if (key instanceof net.minecraft.resources.ResourceKey<?> resourceKey) {
                ResourceLocation location = resourceKey.location();
                if (location != null) {
                    Object value = BuiltInRegistries.REGISTRY.get(location);
                    if (value instanceof Registry<?> registry) {
                        return registry;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch) || ch == '_' || ch == '-' || ch == '/') {
                builder.append(' ');
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                builder.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString().trim();
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
