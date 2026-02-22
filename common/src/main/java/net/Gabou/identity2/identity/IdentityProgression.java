package net.Gabou.identity2.identity;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class IdentityProgression {
    // Sheep wool visual shape looks wider than the base collision box in this morph setup.
    // Keep this tunable to match in-game feel.
    private static final double SHEEP_WIDTH_COLLISION_SCALE = 1.2D;
    private static final Identifier HEALTH_SCALING_MODIFIER_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "identity_max_health");

    private static final String UNLOCKED_IDENTITIES_KEY = "identity2.unlocked_identities";
    private static final String IDENTITY_KILL_COUNTS_KEY = "identity2.identity_kill_counts";
    public static final String UNLOCKED_IDENTITIES_CACHE_KEY = "identity2.unlocked_identities_cache";
    public static final String SELECTED_IDENTITY_TYPE_KEY = "identity2.identity_type";
    public static final String SELECTED_IDENTITY_VARIANT_KEY = "identity2.identity_variant";
    public static final String PREVIOUS_IDENTITY_TYPE_KEY = "identity2.previous_identity_type";
    public static final String PREVIOUS_IDENTITY_VARIANT_KEY = "identity2.previous_identity_variant";
    public static final String TRANSITION_START_TICK_KEY = "identity2.transition_start_tick";
    public static final String TRANSITION_DURATION_TICKS_KEY = "identity2.transition_duration_ticks";
    public static final String BASE_PLAYER_TRANSITION_SENTINEL = "identity2:base_player";
    private static final Codec<List<String>> STRING_LIST_CODEC = Codec.STRING.listOf();
    private static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Map<Identifier, String> DISABLED_IDENTITIES = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private IdentityProgression() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        EntityEvent.LIVING_DEATH.register(IdentityProgression::onLivingDeath);
    }

    public static List<String> getUnlockedIdentities(ServerPlayer player) {
        ensureClientUnlockCache(player);
        return new ArrayList<>(getCustomData(player).read(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
    }

    public static boolean isUnlocked(ServerPlayer player, Identifier identityId) {
        return getUnlockedIdentities(player).contains(identityId.toString());
    }

    public static boolean isMorphableIdentity(Identifier identityId) {
        if (identityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            return false;
        }
        if (isIdentityTemporarilyDisabled(identityId)) {
            return false;
        }
        return isMorphableType(BuiltInRegistries.ENTITY_TYPE.getValue(identityId));
    }

    public static boolean isIdentityTemporarilyDisabled(Identifier identityId) {
        return identityId != null && DISABLED_IDENTITIES.containsKey(identityId);
    }

    public static String getDisabledIdentityReason(Identifier identityId) {
        if (identityId == null) {
            return "";
        }
        return DISABLED_IDENTITIES.getOrDefault(identityId, "");
    }

    public static void disableIdentity(Identifier identityId, String reason) {
        if (identityId == null) {
            return;
        }
        String safeReason = reason == null || reason.isBlank() ? "load failure" : reason;
        DISABLED_IDENTITIES.put(identityId, safeReason);
        Identity2.LOGGER.error("Temporarily disabled identity {}: {}", identityId, safeReason);
    }

    public static boolean isMorphableType(EntityType<?> entityType) {
        if (entityType == null || entityType == EntityType.PLAYER) {
            return false;
        }
        if (entityType == EntityType.IRON_GOLEM || entityType == EntityType.SNOW_GOLEM) {
            return true;
        }
        if (entityType.getCategory() == MobCategory.MISC) {
            return false;
        }
        // 1.21.11 mappings return Entity.class from EntityType#getBaseClass(),
        // so class-based living checks are unreliable here.
        return true;
    }

    public static void morph(ServerPlayer player, Identifier identityId) {
        morph(player, identityId, new CompoundTag());
    }

    public static void morph(ServerPlayer player, Identifier identityId, CompoundTag variantNbt) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        String value = identityId.toString();
        CompoundTag safeVariant = variantNbt == null ? new CompoundTag() : variantNbt.copy();
        String serializedVariant = serializeVariantNbt(safeVariant);
        CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        String previousType = resolveTransitionSourceType(nbt);
        String previousVariant = resolveTransitionSourceVariant(nbt, previousType);
        if (previousType.isBlank()) {
            previousType = BASE_PLAYER_TRANSITION_SENTINEL;
            previousVariant = "";
        }
        double transitionDuration = Math.max(0, IdentitySettings.morphTransitionTicks);
        double transitionStart = player.level() != null ? player.level().getGameTime() : 0.0D;
        setTransitionData(nbt, previousType, previousVariant, transitionStart, transitionDuration);
        if (transitionDuration <= 0.0D) {
            previousType = "";
            previousVariant = "";
            transitionStart = 0.0D;
        }

        nbt.putString("model_override", value);
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, value);
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, serializedVariant);
        ((EntityAccessor) player).setCurrentIdentity(identityId.toString(), safeVariant);

        double widthOverride = 0.0;
        double heightOverride = 0.0;
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            nbt.putString("model_override", "");
            nbt.putString(SELECTED_IDENTITY_TYPE_KEY, "");
            nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, "");
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearTransitionData(nbt);
            syncMorphData(player, "", "", 0.0, 0.0, "", "", 0.0, 0.0);
            return;
        }

        net.minecraft.world.entity.EntityDimensions identityDimensions = identity.getDimensions(identity.getPose());
        widthOverride = identityDimensions.width();
        heightOverride = identityDimensions.height();

        if (identity.getType() == EntityType.SHEEP) {
            widthOverride *= SHEEP_WIDTH_COLLISION_SCALE;
        }

        float widthScale = identityDimensions.width() > 0.0F ? (float)(widthOverride / identityDimensions.width()) : 1.0F;
        float heightScale = identityDimensions.height() > 0.0F ? (float)(heightOverride / identityDimensions.height()) : 1.0F;
        ((EntityAccessor) player).setEntityDimensions(identityDimensions.scale(widthScale, heightScale));
        ((EntityAccessor) player).setStandingEyeHeight(identity.getEyeHeight());

        nbt.putDouble("width_override", widthOverride);
        nbt.putDouble("height_override", heightOverride);
        applyHealthScaling(player, identity);
        syncMorphData(player, value, serializedVariant, widthOverride, heightOverride, previousType, previousVariant, transitionStart, transitionDuration);
    }

    public static void clearMorph(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        String previousType = resolveTransitionSourceType(nbt);
        String previousVariant = resolveTransitionSourceVariant(nbt, previousType);
        if (previousType.isBlank()) {
            previousType = BASE_PLAYER_TRANSITION_SENTINEL;
            previousVariant = "";
        }
        double transitionDuration = Math.max(0, IdentitySettings.morphTransitionTicks);
        double transitionStart = player.level() != null ? player.level().getGameTime() : 0.0D;
        setTransitionData(nbt, previousType, previousVariant, transitionStart, transitionDuration);
        if (transitionDuration <= 0.0D) {
            previousType = "";
            previousVariant = "";
            transitionStart = 0.0D;
        }
        nbt.putString("model_override", "");
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        ((EntityAccessor) player).setCurrentIdentity("");
        ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
        ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
        applyHealthScaling(player, null);
        syncMorphData(player, "", "", 0.0, 0.0, previousType, previousVariant, transitionStart, transitionDuration);
    }

    public static void restoreMorphFromSavedData(ServerPlayer player) {
        CompoundTag nbt = getCustomData(player);
        String type = nbt.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
        if (type.isBlank()) {
            type = nbt.getStringOr("model_override", "");
        }

        if (type.isBlank()) {
            nbt.putString("model_override", "");
            nbt.putString(SELECTED_IDENTITY_TYPE_KEY, "");
            nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, "");
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearTransitionData(nbt);
            ((EntityAccessor) player).setCurrentIdentity("");
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            applyHealthScaling(player, null);
            return;
        }

        CompoundTag variant = parseVariantNbt(nbt.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, ""));
        ((EntityAccessor) player).setCurrentIdentity(type, variant);
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearTransitionData(nbt);
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            applyHealthScaling(player, null);
            return;
        }

        net.minecraft.world.entity.EntityDimensions identityDimensions = identity.getDimensions(identity.getPose());
        double widthOverride = identityDimensions.width();
        double heightOverride = identityDimensions.height();

        if (identity.getType() == EntityType.SHEEP) {
            widthOverride *= SHEEP_WIDTH_COLLISION_SCALE;
        }

        float widthScale = identityDimensions.width() > 0.0F ? (float)(widthOverride / identityDimensions.width()) : 1.0F;
        float heightScale = identityDimensions.height() > 0.0F ? (float)(heightOverride / identityDimensions.height()) : 1.0F;
        ((EntityAccessor) player).setEntityDimensions(identityDimensions.scale(widthScale, heightScale));
        ((EntityAccessor) player).setStandingEyeHeight(identity.getEyeHeight());

        nbt.putString("model_override", type);
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, type);
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, serializeVariantNbt(variant));
        nbt.putDouble("width_override", widthOverride);
        nbt.putDouble("height_override", heightOverride);
        clearTransitionData(nbt);
        applyHealthScaling(player, identity);
    }

    public static void restoreMorphFromSavedDataAndSync(ServerPlayer player) {
        restoreMorphFromSavedData(player);
        CompoundTag nbt = getCustomData(player);
        String modelOverride = nbt.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
        if (modelOverride.isBlank()) {
            modelOverride = nbt.getStringOr("model_override", "");
        }
        String variant = nbt.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, "");
        double widthOverride = nbt.getDoubleOr("width_override", 0.0);
        double heightOverride = nbt.getDoubleOr("height_override", 0.0);
        String previousType = nbt.getStringOr(PREVIOUS_IDENTITY_TYPE_KEY, "");
        String previousVariant = nbt.getStringOr(PREVIOUS_IDENTITY_VARIANT_KEY, "");
        double transitionStart = nbt.getDoubleOr(TRANSITION_START_TICK_KEY, 0.0);
        double transitionDuration = nbt.getDoubleOr(TRANSITION_DURATION_TICKS_KEY, 0.0);
        syncMorphData(player, modelOverride, variant, widthOverride, heightOverride, previousType, previousVariant, transitionStart, transitionDuration);
    }

    public static void refreshScaledHealth(ServerPlayer player) {
        if (player == null) {
            return;
        }
        applyHealthScaling(player, ((EntityAccessor) player).getCurrentIdentity());
    }

    public static void ensureClientUnlockCache(ServerPlayer player) {
        CompoundTag customData = getCustomData(player);
        List<String> unlocked = new ArrayList<>(customData.read(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
        customData.putString(UNLOCKED_IDENTITIES_CACHE_KEY, serializeUnlocked(unlocked));
    }

    public static boolean grantIdentity(ServerPlayer player, Identifier identityId) {
        if (player == null || !isMorphableIdentity(identityId)) {
            return false;
        }
        return unlockIdentity(player, identityId);
    }

    public static int grantAllMorphableIdentities(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int granted = 0;
        for (Identifier identityId : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!isMorphableIdentity(identityId)) {
                continue;
            }
            if (unlockIdentity(player, identityId)) {
                granted++;
            }
        }
        return granted;
    }

    public static int clearUnlockedIdentities(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        CompoundTag customData = getCustomData(player);
        List<String> unlocked = new ArrayList<>(customData.read(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
        int removed = unlocked.size();
        customData.store(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC, List.of());
        customData.store(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC, Map.of());
        customData.putString(UNLOCKED_IDENTITIES_CACHE_KEY, "");

        NetworkManager.sendToPlayer(
            player,
            new CustomEntityStringDataS2CPacketPayload(
                player.getId(),
                List.of(new CustomEntityDataS2CPacket.EntryString(UNLOCKED_IDENTITIES_CACHE_KEY, ""))
            )
        );
        return removed;
    }

    private static EventResult onLivingDeath(LivingEntity killed, DamageSource source) {
        if (!IdentitySettings.enableIdentityKillUnlocks || killed.level().isClientSide()) {
            return EventResult.pass();
        }

        Entity attacker = source.getEntity();
        ServerPlayer player = null;
        if (attacker instanceof ServerPlayer directPlayer) {
            player = directPlayer;
        } else if (attacker != null) {
            Entity owner = ((EntityAccessor) attacker).getIdentityOwner();
            if (owner instanceof ServerPlayer ownerPlayer) {
                player = ownerPlayer;
            }
        }

        if (player == null) {
            return EventResult.pass();
        }

        Identifier identityId = resolveUnlockIdentityFromKilled(killed);
        if (!isMorphableIdentity(identityId)) {
            return EventResult.pass();
        }

        int kills = incrementKillCount(player, identityId);
        int requiredKills = Math.max(1, IdentitySettings.identityKillsRequired);
        if (kills < requiredKills) {
            return EventResult.pass();
        }

        if (unlockIdentity(player, identityId)) {
            player.displayClientMessage(Component.literal("Unlocked identity: " + identityId), false);
            Identity2.LOGGER.info("Unlocked identity {} for {}", identityId, player.getName().getString());
            broadcastAcquisitionAnimation(player, killed, true);
        }

        return EventResult.pass();
    }

    private static Identifier resolveUnlockIdentityFromKilled(LivingEntity killed) {
        if (killed instanceof ServerPlayer killedPlayer) {
            Entity activeIdentity = ((EntityAccessor) killedPlayer).getCurrentIdentity();
            if (activeIdentity != null) {
                Identifier morphedIdentityId = BuiltInRegistries.ENTITY_TYPE.getKey(activeIdentity.getType());
                if (morphedIdentityId != null && isMorphableIdentity(morphedIdentityId)) {
                    return morphedIdentityId;
                }
            }

            // Fallback to persisted identity selection if runtime entity is unavailable.
            CompoundTag customData = getCustomData(killedPlayer);
            String selectedType = customData.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
            if (selectedType.isBlank()) {
                selectedType = customData.getStringOr("model_override", "");
            }
            if (!selectedType.isBlank()) {
                try {
                    Identifier selectedId = Identifier.parse(selectedType);
                    if (isMorphableIdentity(selectedId)) {
                        return selectedId;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
    }

    private static int incrementKillCount(ServerPlayer player, Identifier identityId) {
        CompoundTag nbt = getCustomData(player);
        Map<String, Integer> killMap = new HashMap<>(nbt.read(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
        int kills = killMap.getOrDefault(identityId.toString(), 0) + 1;
        killMap.put(identityId.toString(), kills);
        nbt.store(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC, killMap);
        return kills;
    }

    private static boolean unlockIdentity(ServerPlayer player, Identifier identityId) {
        List<String> unlocked = getUnlockedIdentities(player);
        String key = identityId.toString();
        if (unlocked.contains(key)) {
            return false;
        }

        unlocked.add(key);
        CompoundTag customData = getCustomData(player);
        customData.store(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC, unlocked);
        customData.putString(UNLOCKED_IDENTITIES_CACHE_KEY, serializeUnlocked(unlocked));
        NetworkManager.sendToPlayer(
            player,
            new CustomEntityStringDataS2CPacketPayload(
                player.getId(),
                List.of(new CustomEntityDataS2CPacket.EntryString(UNLOCKED_IDENTITIES_CACHE_KEY, serializeUnlocked(unlocked)))
            )
        );
        return true;
    }

    private static String serializeUnlocked(List<String> unlocked) {
        if (unlocked.isEmpty()) {
            return "";
        }

        List<String> sorted = new ArrayList<>(unlocked);
        Collections.sort(sorted);
        return String.join(",", sorted);
    }

    private static void syncMorphData(
        ServerPlayer player,
        String modelOverride,
        String variant,
        double widthOverride,
        double heightOverride,
        String previousType,
        String previousVariant,
        double transitionStartTick,
        double transitionDurationTicks
    ) {
        CustomEntityStringDataS2CPacketPayload modelPayload = new CustomEntityStringDataS2CPacketPayload(
            player.getId(),
            List.of(
                new CustomEntityDataS2CPacket.EntryString("model_override", modelOverride),
                new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_TYPE_KEY, modelOverride),
                new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_VARIANT_KEY, variant),
                new CustomEntityDataS2CPacket.EntryString(PREVIOUS_IDENTITY_TYPE_KEY, previousType),
                new CustomEntityDataS2CPacket.EntryString(PREVIOUS_IDENTITY_VARIANT_KEY, previousVariant)
            )
        );
        CustomEntityDataS2CPacketPayload shapePayload = new CustomEntityDataS2CPacketPayload(
            player.getId(),
            List.of(
                new CustomEntityDataS2CPacket.Entry("width_override", widthOverride),
                new CustomEntityDataS2CPacket.Entry("height_override", heightOverride),
                new CustomEntityDataS2CPacket.Entry(TRANSITION_START_TICK_KEY, transitionStartTick),
                new CustomEntityDataS2CPacket.Entry(TRANSITION_DURATION_TICKS_KEY, transitionDurationTicks)
            )
        );

        NetworkManager.sendToPlayer(player, modelPayload);
        NetworkManager.sendToPlayer(player, shapePayload);
        if (player.level() instanceof ServerLevel serverWorld) {
            for (ServerPlayer other : serverWorld.players()) {
                if (other != player) {
                    NetworkManager.sendToPlayer(other, modelPayload);
                    NetworkManager.sendToPlayer(other, shapePayload);
                }
            }
        }
    }

    private static void broadcastAcquisitionAnimation(ServerPlayer player, LivingEntity acquired, boolean morphAcquisition) {
        if (!IdentitySettings.enableMorphAcquisitionTendrils) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverWorld)) {
            return;
        }
        MorphAcquisitionS2CPacketPayload payload = new MorphAcquisitionS2CPacketPayload(
            player.getId(),
            acquired.getId(),
            acquired.getX(),
            acquired.getY() + acquired.getBbHeight() * 0.5D,
            acquired.getZ(),
            morphAcquisition
        );
        NetworkManager.sendToPlayer(player, payload);
        for (ServerPlayer other : serverWorld.players()) {
            if (other != player) {
                NetworkManager.sendToPlayer(other, payload);
            }
        }
    }

    private static String resolveTransitionSourceType(CompoundTag nbt) {
        String source = nbt.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
        if (source.isBlank()) {
            source = nbt.getStringOr("model_override", "");
        }
        return source;
    }

    private static String resolveTransitionSourceVariant(CompoundTag nbt, String sourceType) {
        if (sourceType == null || sourceType.isBlank() || BASE_PLAYER_TRANSITION_SENTINEL.equals(sourceType)) {
            return "";
        }
        return nbt.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, "");
    }

    private static void setTransitionData(
        CompoundTag nbt,
        String previousType,
        String previousVariant,
        double transitionStartTick,
        double transitionDurationTicks
    ) {
        if (transitionDurationTicks <= 0.0D) {
            clearTransitionData(nbt);
            return;
        }
        nbt.putString(PREVIOUS_IDENTITY_TYPE_KEY, previousType == null ? "" : previousType);
        nbt.putString(PREVIOUS_IDENTITY_VARIANT_KEY, previousVariant == null ? "" : previousVariant);
        nbt.putDouble(TRANSITION_START_TICK_KEY, transitionStartTick);
        nbt.putDouble(TRANSITION_DURATION_TICKS_KEY, transitionDurationTicks);
    }

    private static void clearTransitionData(CompoundTag nbt) {
        nbt.putString(PREVIOUS_IDENTITY_TYPE_KEY, "");
        nbt.putString(PREVIOUS_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble(TRANSITION_START_TICK_KEY, 0.0D);
        nbt.putDouble(TRANSITION_DURATION_TICKS_KEY, 0.0D);
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
    }

    private static void applyHealthScaling(ServerPlayer player, @Nullable Entity identity) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        float oldMaxHealth = player.getMaxHealth();
        float oldHealth = player.getHealth();
        float healthRatio = oldMaxHealth > 0.0F ? (oldHealth / oldMaxHealth) : 1.0F;

        maxHealthAttr.removeModifier(HEALTH_SCALING_MODIFIER_ID);

        if (!IdentitySettings.scalingHealth || !(identity instanceof LivingEntity livingIdentity)) {
            float newMaxHealth = player.getMaxHealth();
            float scaled = Mth.clamp(healthRatio * newMaxHealth, 1.0F, newMaxHealth);
            player.setHealth(scaled);
            return;
        }

        double base = maxHealthAttr.getBaseValue();
        double desired = resolveIdentityMaxHealth(player, livingIdentity);
        desired = Math.max(1.0D, Math.min(desired, Math.max(1, IdentitySettings.maxHealth)));
        double delta = desired - base;
        if (Math.abs(delta) > 1.0E-4D) {
            maxHealthAttr.addOrUpdateTransientModifier(
                new AttributeModifier(HEALTH_SCALING_MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE)
            );
        }

        float newMaxHealth = player.getMaxHealth();
        float scaled = Mth.clamp(healthRatio * newMaxHealth, 1.0F, newMaxHealth);
        player.setHealth(scaled);
    }

    private static double resolveIdentityMaxHealth(ServerPlayer player, LivingEntity livingIdentity) {
        if (player == null || player.level() == null) {
            return livingIdentity.getMaxHealth();
        }
        try {
            Entity probe = livingIdentity.getType().create(player.level(), EntitySpawnReason.COMMAND);
            if (probe instanceof LivingEntity probeLiving) {
                return probeLiving.getMaxHealth();
            }
        } catch (Throwable ignored) {
        }
        return livingIdentity.getMaxHealth();
    }

    public static String serializeVariantNbt(CompoundTag variantNbt) {
        if (variantNbt == null || variantNbt.isEmpty()) {
            return "";
        }
        return variantNbt.copy().toString();
    }

    public static CompoundTag parseVariantNbt(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CompoundTag();
        }
        try {
            return CompoundTagArgument.compoundTag().parse(new StringReader(raw));
        } catch (Exception ignored) {
            return new CompoundTag();
        }
    }
}
