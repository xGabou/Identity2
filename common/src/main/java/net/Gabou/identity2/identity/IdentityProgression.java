package net.Gabou.identity2.identity;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;

import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.UnlockedIdentitySyncS2CPacketPayload;
import net.Gabou.identity2.progression.MorphChargeManager;
import net.Gabou.identity2.progression.ProgressionConfig;
import net.Gabou.identity2.progression.SoulAbsorptionManager;
import net.Gabou.identity2.progression.SoulJarManager;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.Gabou.identity2.util.AttributeContainerAccessor;
import net.Gabou.identity2.util.DefaultAttributeContainerAccessor;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class IdentityProgression {
    // Sheep wool visual shape looks wider than the base collision box in this morph setup.
    // Keep this tunable to match in-game feel.
    private static final double SHEEP_WIDTH_COLLISION_SCALE = 1.2D;
    private static final ResourceLocation HEALTH_SCALING_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Identity2.MOD_ID, "identity_max_health");
    private static final String MORPH_ATTRIBUTE_BASE_MODIFIER_PREFIX = "morph_attribute_base_";
    private static final String MORPH_ATTRIBUTE_MODIFIER_PREFIX = "morph_attribute_modifier_";
    public static final ResourceLocation PLAYER_IDENTITY_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "player");
    public static final String PLAYER_SKIN_UUID_VARIANT_KEY = "SkinPlayerUuid";
    public static final String PLAYER_SKIN_NAME_VARIANT_KEY = "SkinPlayerName";

    private static final String UNLOCKED_IDENTITIES_KEY = "identity2.unlocked_identities";
    private static final String UNLOCKED_IDENTITY_VARIANTS_KEY = "identity2.unlocked_identity_variants";
    private static final String UNLOCKED_IDENTITIES_CACHE_KEY = "identity2.unlocked_identities_cache";
    private static final String UNLOCKED_IDENTITY_VARIANTS_CACHE_KEY = "identity2.unlocked_identity_variants_cache";
    private static final String IDENTITY_KILL_COUNTS_KEY = "identity2.identity_kill_counts";
    private static final String HOSTILE_IDENTITY_GRACE_END_TICK_KEY = "identity2.hostile_identity_grace_end_tick";
    public static final String SELECTED_IDENTITY_TYPE_KEY = "identity2.identity_type";
    public static final String SELECTED_IDENTITY_VARIANT_KEY = "identity2.identity_variant";
    public static final String PREVIOUS_IDENTITY_TYPE_KEY = "identity2.previous_identity_type";
    public static final String PREVIOUS_IDENTITY_VARIANT_KEY = "identity2.previous_identity_variant";
    public static final String MORPH_DAMAGE_GRACE_END_TICK_KEY = "identity2.morph_damage_grace_end_tick";
    public static final String TRANSITION_START_TICK_KEY = "identity2.transition_start_tick";
    public static final String TRANSITION_DURATION_TICKS_KEY = "identity2.transition_duration_ticks";
    public static final String BASE_PLAYER_TRANSITION_SENTINEL = "identity2:base_player";
    private static final String DAILY_RANDOM_MORPH_LAST_DAY_KEY = "identity2.daily_random_morph_last_day";
    private static final Codec<List<String>> STRING_LIST_CODEC = Codec.STRING.listOf();
    private static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, List<String>>> STRING_LIST_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());
    private static final Set<String> NON_VARIANT_ROOT_KEYS = Set.of("Age", "AgeLocked", "EggLayTime");
    private static final int MAX_UNLOCKED_IDENTITY_SYNC_BYTES = FriendlyByteBuf.MAX_STRING_LENGTH;
    private static final Map<ResourceLocation, String> DISABLED_IDENTITIES = new ConcurrentHashMap<>();
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
        return new ArrayList<>(readUnlockedIdentityIds(getCustomData(player)));
    }

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation identityId) {
        return getUnlockedIdentities(player).contains(identityId.toString());
    }

    public static boolean shouldEnforceIdentityUnlocksForMorph() {
        return IdentitySettings.requireUnlockedIdentityForMorph
            || IdentitySettings.killForIdentity
            || IdentitySettings.enableIdentityKillUnlocks;
    }

    @Nullable
    public static ResourceLocation getForcedIdentity() {
        String forced = IdentitySettings.forcedIdentity;
        if (forced == null || forced.isBlank()) {
            return null;
        }

        try {
            ResourceLocation forcedIdentity = ResourceLocation.parse(forced.trim());
            return isMorphableIdentity(forcedIdentity) ? forcedIdentity : null;
        } catch (Exception exception) {
            Identity2.LOGGER.warn("Ignoring invalid forced identity config value: {}", forced, exception);
            return null;
        }
    }

    public static boolean canGrantIdentityFlight(ServerPlayer player) {
        if (player == null || !IdentitySettings.enableFlight) {
            return false;
        }

        List<String> requiredAdvancements = IdentitySettings.advancementsRequiredForFlight;
        if (requiredAdvancements == null || requiredAdvancements.isEmpty()) {
            return true;
        }

        for (String advancementId : requiredAdvancements) {
            if (!identity2$hasCompletedAdvancement(player, advancementId)) {
                return false;
            }
        }

        return true;
    }

    public static void updateHostileIdentityGrace(ServerPlayer player, @Nullable Entity identity) {
        if (player == null) {
            return;
        }

        CompoundTag nbt = getCustomData(player);
        if (!IdentitySettings.hostilesForgetNewHostileIdentityPlayer) {
            nbt.putDouble(HOSTILE_IDENTITY_GRACE_END_TICK_KEY, 0.0D);
            return;
        }

        if (!(identity instanceof LivingEntity livingIdentity)) {
            nbt.putDouble(HOSTILE_IDENTITY_GRACE_END_TICK_KEY, 0.0D);
            return;
        }

        EntityType<?> identityType = livingIdentity.getType();
        if (identityType == null || identityType.getCategory() != MobCategory.MONSTER) {
            nbt.putDouble(HOSTILE_IDENTITY_GRACE_END_TICK_KEY, 0.0D);
            return;
        }

        double now = player.level() != null ? player.level().getGameTime() : 0.0D;
        double duration = Math.max(0, IdentitySettings.hostilityTime);
        nbt.putDouble(HOSTILE_IDENTITY_GRACE_END_TICK_KEY, now + duration);
    }

    public static boolean isHostileIdentityGraceActive(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }

        CompoundTag nbt = getCustomData(player);
        double endTick = nbt.getDoubleOr(HOSTILE_IDENTITY_GRACE_END_TICK_KEY, 0.0D);
        return endTick > 0.0D && player.level().getGameTime() < endTick;
    }

    public static boolean isMorphableIdentity(ResourceLocation identityId) {
        if (identityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            return false;
        }
        if (isIdentityTemporarilyDisabled(identityId)) {
            return false;
        }
        return isMorphableType(BuiltInRegistries.ENTITY_TYPE.getValue(identityId));
    }

    public static boolean isIdentityTemporarilyDisabled(ResourceLocation identityId) {
        return identityId != null && DISABLED_IDENTITIES.containsKey(identityId);
    }

    public static String getDisabledIdentityReason(ResourceLocation identityId) {
        if (identityId == null) {
            return "";
        }
        return DISABLED_IDENTITIES.getOrDefault(identityId, "");
    }

    public static void disableIdentity(ResourceLocation identityId, String reason) {
        if (identityId == null) {
            return;
        }
        String safeReason = reason == null || reason.isBlank() ? "load failure" : reason;
        DISABLED_IDENTITIES.put(identityId, safeReason);
        Identity2.LOGGER.error("Temporarily disabled identity {}: {}", identityId, safeReason);
    }

    public static boolean isMorphDamageGraceActive(Player player) {
        if (player == null || player.level() == null || player.level().isClientSide()) {
            return false;
        }
        CustomData customData = ((EntityAccessor) player).getCustomData();
        CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        double endTick = nbt.getDoubleOr(MORPH_DAMAGE_GRACE_END_TICK_KEY, 0.0D);
        return endTick > 0.0D && player.level().getGameTime() <= endTick;
    }

    public static boolean isMorphableType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }
        if (
                entityType == EntityType.PLAYER
                        || entityType == EntityType.IRON_GOLEM
                        || entityType == EntityType.SNOW_GOLEM
                        || entityType == EntityType.VILLAGER
                        || entityType == EntityType.WANDERING_TRADER
        ) {
            return true;
        }
        if (entityType.getCategory() == MobCategory.MISC) {
            return false;
        }
        // 1.21.11 mappings return Entity.class from EntityType#getBaseClass(),
        // so class-based living checks are unreliable here.
        return true;
    }

    public static boolean morph(ServerPlayer player, ResourceLocation identityId) {
        return morph(player, identityId, new CompoundTag());
    }

    public static boolean morph(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return false;
        }
        CustomData customData = ((EntityAccessor) player).getCustomData();
        String value = identityId.toString();
        CompoundTag safeVariant = variantNbt == null ? new CompoundTag() : variantNbt.copy();
        if (!MorphChargeManager.tryConsumeMorphCharge(player, identityId, safeVariant, true)) {
            return false;
        }
        String serializedVariant = serializeVariantNbt(safeVariant);
        CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        double previousWidth = nbt.getDoubleOr("width_override", 0.0D);
        double previousHeight = nbt.getDoubleOr("height_override", 0.0D);
        if (previousWidth <= 0.0D) {
            previousWidth = player.getBbWidth();
        }
        if (previousHeight <= 0.0D) {
            previousHeight = player.getBbHeight();
        }
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
        if (PLAYER_IDENTITY_ID.equals(identityId)) {
            ((EntityAccessor) player).setCurrentIdentity("");
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearMorphDamageGrace(nbt);
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            ensureSafePostResizePosition(player);
            applyMorphAttributes(player, null);
            applyHealthScaling(player, null);
            syncMorphData(player, value, serializedVariant, 0.0, 0.0, previousType, previousVariant, transitionStart, transitionDuration);
            return true;
        }
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
            clearMorphDamageGrace(nbt);
            clearTransitionData(nbt);
            syncMorphData(player, "", "", 0.0, 0.0, "", "", 0.0, 0.0);
            return false;
        }

        net.minecraft.world.entity.EntityDimensions identityDimensions = identity.getDimensions(identity.getPose());
        widthOverride = identityDimensions.width();
        heightOverride = identityDimensions.height();

        if (identity.getType() == EntityType.SHEEP) {
            widthOverride *= SHEEP_WIDTH_COLLISION_SCALE;
        }
        applyMorphDamageGrace(player, nbt, previousWidth, previousHeight, widthOverride, heightOverride);

        float widthScale = identityDimensions.width() > 0.0F ? (float) (widthOverride / identityDimensions.width()) : 1.0F;
        float heightScale = identityDimensions.height() > 0.0F ? (float) (heightOverride / identityDimensions.height()) : 1.0F;
        ((EntityAccessor) player).setEntityDimensions(identityDimensions.scale(widthScale, heightScale));
        ((EntityAccessor) player).setStandingEyeHeight(identity.getEyeHeight());
        ensureSafePostResizePosition(player);

        nbt.putDouble("width_override", widthOverride);
        nbt.putDouble("height_override", heightOverride);
        applyMorphAttributes(player, identity);
        applyHealthScaling(player, identity);
        syncMorphData(player, value, serializedVariant, widthOverride, heightOverride, previousType, previousVariant, transitionStart, transitionDuration);
        return true;
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
        clearMorphDamageGrace(nbt);
        ((EntityAccessor) player).setCurrentIdentity("");
        ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
        ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
        ensureSafePostResizePosition(player);
        applyMorphAttributes(player, null);
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
            clearMorphDamageGrace(nbt);
            clearTransitionData(nbt);
            ((EntityAccessor) player).setCurrentIdentity("");
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            ensureSafePostResizePosition(player);
            applyMorphAttributes(player, null);
            applyHealthScaling(player, null);
            return;
        }

        if (PLAYER_IDENTITY_ID.toString().equals(type)) {
            nbt.putString("model_override", PLAYER_IDENTITY_ID.toString());
            nbt.putString(SELECTED_IDENTITY_TYPE_KEY, PLAYER_IDENTITY_ID.toString());
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearMorphDamageGrace(nbt);
            clearTransitionData(nbt);
            ((EntityAccessor) player).setCurrentIdentity("");
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            ensureSafePostResizePosition(player);
            applyMorphAttributes(player, null);
            applyHealthScaling(player, null);
            return;
        }

        CompoundTag variant = parseVariantNbt(nbt.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, ""));
        ((EntityAccessor) player).setCurrentIdentity(type, variant);
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            clearMorphDamageGrace(nbt);
            clearTransitionData(nbt);
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            ensureSafePostResizePosition(player);
            applyMorphAttributes(player, null);
            applyHealthScaling(player, null);
            return;
        }

        net.minecraft.world.entity.EntityDimensions identityDimensions = identity.getDimensions(identity.getPose());
        double widthOverride = identityDimensions.width();
        double heightOverride = identityDimensions.height();

        if (identity.getType() == EntityType.SHEEP) {
            widthOverride *= SHEEP_WIDTH_COLLISION_SCALE;
        }

        float widthScale = identityDimensions.width() > 0.0F ? (float) (widthOverride / identityDimensions.width()) : 1.0F;
        float heightScale = identityDimensions.height() > 0.0F ? (float) (heightOverride / identityDimensions.height()) : 1.0F;
        ((EntityAccessor) player).setEntityDimensions(identityDimensions.scale(widthScale, heightScale));
        ((EntityAccessor) player).setStandingEyeHeight(identity.getEyeHeight());
        ensureSafePostResizePosition(player);

        nbt.putString("model_override", type);
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, type);
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, serializeVariantNbt(variant));
        nbt.putDouble("width_override", widthOverride);
        nbt.putDouble("height_override", heightOverride);
        clearTransitionData(nbt);
        applyMorphAttributes(player, identity);
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

    public static void updateCurrentVariantAndSync(ServerPlayer player, CompoundTag variantNbt) {
        if (player == null) {
            return;
        }
        CompoundTag nbt = getCustomData(player);
        String serializedVariant = serializeVariantNbt(variantNbt);
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, serializedVariant);

        String modelOverride = nbt.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
        if (modelOverride.isBlank()) {
            modelOverride = nbt.getStringOr("model_override", "");
        }

        CustomEntityStringDataS2CPacketPayload payload = new CustomEntityStringDataS2CPacketPayload(
                player.getId(),
                List.of(
                        new CustomEntityDataS2CPacket.EntryString("model_override", modelOverride),
                        new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_TYPE_KEY, modelOverride),
                        new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_VARIANT_KEY, serializedVariant)
                )
        );

        NetworkManager.sendToPlayer(player, payload);
        if (player.level() instanceof ServerLevel serverWorld) {
            for (ServerPlayer other : serverWorld.players()) {
                if (other != player) {
                    NetworkManager.sendToPlayer(other, payload);
                }
            }
        }
    }

    public static void refreshScaledHealth(ServerPlayer player) {
        if (player == null) {
            return;
        }
        applyMorphAttributes(player, ((EntityAccessor) player).getCurrentIdentity());
        applyHealthScaling(player, ((EntityAccessor) player).getCurrentIdentity());
    }

    public static void tickDailyRandomMorph(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().isClientSide()) {
            return;
        }

        long day = player.level().getGameTime() / 24000L;
        CompoundTag nbt = getCustomData(player);
        long lastProcessedDay = (long) nbt.getDoubleOr(DAILY_RANDOM_MORPH_LAST_DAY_KEY, -1.0D);

        if (!IdentitySettings.randomMorphEveryDay) {
            if (lastProcessedDay != day) {
                nbt.putDouble(DAILY_RANDOM_MORPH_LAST_DAY_KEY, day);
            }
            return;
        }

        if (lastProcessedDay < 0L) {
            nbt.putDouble(DAILY_RANDOM_MORPH_LAST_DAY_KEY, day);
            return;
        }

        if (day <= lastProcessedDay) {
            return;
        }
        nbt.putDouble(DAILY_RANDOM_MORPH_LAST_DAY_KEY, day);

        List<ResourceLocation> unlocked = new ArrayList<>();
        for (String raw : getUnlockedIdentities(player)) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                ResourceLocation id = ResourceLocation.parse(raw);
                if (isMorphableIdentity(id)) {
                    unlocked.add(id);
                }
            } catch (Exception ignored) {
            }
        }
        if (unlocked.isEmpty()) {
            return;
        }

        String currentType = nbt.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
        if (currentType.isBlank()) {
            currentType = nbt.getStringOr("model_override", "");
        }
        if (!currentType.isBlank() && unlocked.size() > 1) {
            String finalCurrentType = currentType;
            unlocked.removeIf(id -> id.toString().equals(finalCurrentType));
        }

        if (unlocked.isEmpty()) {
            return;
        }

        ResourceLocation nextIdentity = unlocked.get(player.getRandom().nextInt(unlocked.size()));
        CompoundTag nextVariant = resolveRandomUnlockedVariant(nbt, nextIdentity, player.getRandom().nextInt());
        morph(player, nextIdentity, nextVariant);
    }

    public static void ensureClientUnlockCache(ServerPlayer player) {
        ensureUnlockedIdentityStorage(player);
    }

    public static boolean grantIdentity(ServerPlayer player, ResourceLocation identityId) {
        if (player == null || !isMorphableIdentity(identityId)) {
            return false;
        }
        return unlockIdentity(player, identityId);
    }

    public static boolean isVariantUnlocked(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !isUnlocked(player, identityId)) {
            return false;
        }
        if (IdentitySettings.unlockAllVariantsOnFirstUnlock) {
            return true;
        }
        CompoundTag customData = getCustomData(player);
        Map<String, List<String>> variantUnlocks = new HashMap<>(
                customData.read(UNLOCKED_IDENTITY_VARIANTS_KEY, STRING_LIST_MAP_CODEC).orElse(Map.of())
        );
        List<String> tokens = variantUnlocks.get(identityId.toString());
        if (tokens == null || tokens.isEmpty()) {
            // Legacy or command unlock: no per-variant restriction for this identity.
            return true;
        }
        String requestedToken = toVariantUnlockToken(normalizeVariantForUnlock(variantNbt));
        if (tokens.contains(requestedToken)) {
            return true;
        }

        for (String storedToken : tokens) {
            if (matchesStoredVariantToken(variantNbt, storedToken)) {
                return true;
            }
        }
        return false;
    }

    public static int grantAllMorphableIdentities(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int granted = 0;
        for (ResourceLocation identityId : BuiltInRegistries.ENTITY_TYPE.keySet()) {
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
        List<String> unlocked = new ArrayList<>(readUnlockedIdentityIds(customData));
        List<String> retained = new ArrayList<>();
        Map<String, List<String>> retainedVariants = new HashMap<>();

        if (ProgressionConfig.enableSoulJars()) {
            for (SoulJarManager.SoulJarData jar : SoulJarManager.getSoulJars(player)) {
                for (SoulJarManager.StoredMorphData morph : jar.morphs()) {
                    if (morph.identityId() == null || morph.identityId().isBlank()) {
                        continue;
                    }
                    if (!retained.contains(morph.identityId())) {
                        retained.add(morph.identityId());
                    }
                    retainedVariants.computeIfAbsent(morph.identityId(), ignored -> new ArrayList<>());
                    List<String> variants = retainedVariants.get(morph.identityId());
                    if (!variants.contains(morph.variantToken())) {
                        variants.add(morph.variantToken());
                    }
                }
            }
        }

        if (ProgressionConfig.enableSoulAbsorption()) {
            for (String absorbedIdentityId : SoulAbsorptionManager.getAbsorbedIdentityIds(player)) {
                if (!retained.contains(absorbedIdentityId)) {
                    retained.add(absorbedIdentityId);
                }
            }
        }

        int removed = Math.max(0, unlocked.size() - retained.size());
        storeUnlockedIdentityData(customData, retained, retainedVariants);
        customData.store(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC, Map.of());
        if (removed > 0) {
            player.displayClientMessage(
                    Component.literal("All unlocked identities were removed."),
                    IdentitySettings.overlayIdentityRevokes
            );
        }
        return removed;
    }

    private static EventResult onLivingDeath(LivingEntity killed, DamageSource source) {
        if (killed.level().isClientSide()) {
            return EventResult.pass();
        }
        SoulJarManager.trySpawnRandomWorldJar(killed);

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

        UnlockTarget unlockTarget = resolveUnlockIdentityFromKilled(killed);
        if (unlockTarget == null || !isMorphableIdentity(unlockTarget.identityId())) {
            return EventResult.pass();
        }

        MorphChargeManager.onIdentityKilled(player, unlockTarget.identityId(), unlockTarget.variantNbt());
        SoulJarManager.onIdentityKilled(player, unlockTarget.identityId(), unlockTarget.variantNbt());

        if (!IdentitySettings.enableIdentityKillUnlocks) {
            return EventResult.pass();
        }

        int kills = incrementKillCount(player, unlockTarget.identityId(), unlockTarget.variantNbt());
        int requiredKills = Math.max(1, IdentitySettings.identityKillsRequired);
        if (IdentitySettings.killForIdentity) {
            requiredKills = Math.max(requiredKills, IdentitySettings.requiredKillsForIdentity);
        }
        if (kills < requiredKills) {
            return EventResult.pass();
        }

        boolean unlocked = unlockIdentityVariant(player, unlockTarget.identityId(), unlockTarget.variantNbt());
        if (unlocked) {
            player.displayClientMessage(
                    Component.literal("Unlocked identity: " + unlockTarget.identityId()),
                    IdentitySettings.overlayIdentityUnlocks
            );
            Identity2.LOGGER.info("Unlocked identity {} for {}", unlockTarget.identityId(), player.getName().getString());
            broadcastAcquisitionAnimation(player, killed, true);
        }

        if (IdentitySettings.forceChangeAlways || (IdentitySettings.forceChangeNew && unlocked)) {
            morph(player, unlockTarget.identityId(), unlockTarget.variantNbt());
        }

        return EventResult.pass();
    }

    @Nullable
    private static UnlockTarget resolveUnlockIdentityFromKilled(LivingEntity killed) {
        if (killed instanceof ServerPlayer killedPlayer) {
            Entity activeIdentity = ((EntityAccessor) killedPlayer).getCurrentIdentity();
            CompoundTag killedCustomData = getCustomData(killedPlayer);
            CompoundTag selectedVariant = parseVariantNbt(killedCustomData.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, ""));
            if (activeIdentity != null) {
                ResourceLocation morphedIdentityId = BuiltInRegistries.ENTITY_TYPE.getKey(activeIdentity.getType());
                if (morphedIdentityId != null && isMorphableIdentity(morphedIdentityId)) {
                    return new UnlockTarget(morphedIdentityId, selectedVariant);
                }
            }

            // Fallback to persisted identity selection if runtime entity is unavailable.
            String selectedType = killedCustomData.getStringOr(SELECTED_IDENTITY_TYPE_KEY, "");
            if (selectedType.isBlank()) {
                selectedType = killedCustomData.getStringOr("model_override", "");
            }
            if (!selectedType.isBlank()) {
                try {
                    ResourceLocation selectedId = ResourceLocation.parse(selectedType);
                    if (isMorphableIdentity(selectedId)) {
                        return new UnlockTarget(selectedId, selectedVariant);
                    }
                } catch (Exception ignored) {
                }
            }

            CompoundTag playerSkinVariant = new CompoundTag();
            playerSkinVariant.putString(PLAYER_SKIN_UUID_VARIANT_KEY, killedPlayer.getUUID().toString());
            playerSkinVariant.putString(PLAYER_SKIN_NAME_VARIANT_KEY, killedPlayer.getGameProfile().getName());
            return new UnlockTarget(PLAYER_IDENTITY_ID, playerSkinVariant);
        }

        ResourceLocation identityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (identityId == null) {
            return null;
        }
        return new UnlockTarget(identityId, normalizeVariantForUnlock(extractVariantData(killed)));
    }

    private static int incrementKillCount(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        CompoundTag nbt = getCustomData(player);
        Map<String, Integer> killMap = new HashMap<>(nbt.read(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
        String key = identityId + "|" + toVariantUnlockToken(normalizeVariantForUnlock(variantNbt));
        int kills = killMap.getOrDefault(key, 0) + 1;
        killMap.put(key, kills);
        nbt.store(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC, killMap);
        return kills;
    }

    private static boolean unlockIdentity(ServerPlayer player, ResourceLocation identityId) {
        List<String> unlocked = getUnlockedIdentities(player);
        String key = identityId.toString();
        boolean changed = false;
        if (!unlocked.contains(key)) {
            unlocked.add(key);
            changed = true;
        }

        CompoundTag customData = getCustomData(player);
        Map<String, List<String>> variantUnlocks = new HashMap<>(
                customData.read(UNLOCKED_IDENTITY_VARIANTS_KEY, STRING_LIST_MAP_CODEC).orElse(Map.of())
        );
        // Command/admin unlock means full identity unlock (all variants), so remove per-variant restriction.
        if (variantUnlocks.remove(key) != null) {
            changed = true;
        }

        if (!changed) {
            return false;
        }

        storeUnlockedIdentityData(getCustomData(player), unlocked, variantUnlocks);
        syncUnlockedIdentities(player);
        return true;
    }

    private static boolean unlockIdentityVariant(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (IdentitySettings.unlockAllVariantsOnFirstUnlock) {
            return unlockIdentity(player, identityId);
        }
        List<String> unlocked = getUnlockedIdentities(player);
        String key = identityId.toString();
        CompoundTag customData = getCustomData(player);
        Map<String, List<String>> variantUnlocks = new HashMap<>(
                customData.read(UNLOCKED_IDENTITY_VARIANTS_KEY, STRING_LIST_MAP_CODEC).orElse(Map.of())
        );

        boolean changed = false;
        boolean previouslyUnlocked = unlocked.contains(key);
        if (!previouslyUnlocked) {
            unlocked.add(key);
            changed = true;
        }

        // If this identity is already wildcard-unlocked (legacy/admin), keep it unrestricted.
        if (previouslyUnlocked && !variantUnlocks.containsKey(key)) {
            if (changed) {
                storeUnlockedIdentityData(getCustomData(player), unlocked, variantUnlocks);
                syncUnlockedIdentities(player);
            }
            return changed;
        }

        List<String> tokens = new ArrayList<>(variantUnlocks.getOrDefault(key, List.of()));
        String token = toVariantUnlockToken(normalizeVariantForUnlock(variantNbt));
        if (!tokens.contains(token)) {
            tokens.add(token);
            variantUnlocks.put(key, tokens);
            changed = true;
        }

        if (!changed) {
            return false;
        }

        storeUnlockedIdentityData(getCustomData(player), unlocked, variantUnlocks);
        syncUnlockedIdentities(player);
        return true;
    }

    public static void syncUnlockedIdentities(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ensureUnlockedIdentityStorage(player);
        CompoundTag customData = getCustomData(player);
        List<String> unlocked = readUnlockedIdentityIds(customData);
        Map<String, List<String>> variantUnlocks = readUnlockedIdentityVariantUnlocks(customData);
        UnlockedIdentitySyncS2CPacketPayload payload = buildUnlockedIdentitySyncPayload(player, unlocked, variantUnlocks);
        if (!validateUnlockedIdentitiesPayload(player, payload)) {
            return;
        }

        NetworkManager.sendToPlayer(player, payload);
        if (player.level() instanceof ServerLevel serverWorld) {
            for (ServerPlayer other : serverWorld.players()) {
                if (other != player) {
                    NetworkManager.sendToPlayer(other, payload);
                }
            }
        }
    }

    private static UnlockedIdentitySyncS2CPacketPayload buildUnlockedIdentitySyncPayload(
            ServerPlayer player,
            List<String> unlocked,
            Map<String, List<String>> variantUnlocks
    ) {
        List<UnlockedIdentitySyncS2CPacketPayload.VariantEntry> variantEntries = new ArrayList<>(variantUnlocks.size());
        for (Map.Entry<String, List<String>> entry : variantUnlocks.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            variantEntries.add(new UnlockedIdentitySyncS2CPacketPayload.VariantEntry(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return new UnlockedIdentitySyncS2CPacketPayload(player.getId(), new ArrayList<>(unlocked), variantEntries);
    }

    public static int getSerializedUnlockedIdentitiesSize(UnlockedIdentitySyncS2CPacketPayload payload) {
        if (payload == null) {
            return 0;
        }

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            UnlockedIdentitySyncS2CPacketPayload.CODEC.encode(buffer, payload);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    public static boolean validateUnlockedIdentitiesPayload(ServerPlayer player, UnlockedIdentitySyncS2CPacketPayload payload) {
        if (player == null || payload == null) {
            return false;
        }

        int maxLength = FriendlyByteBuf.MAX_STRING_LENGTH;
        for (String identityId : payload.unlockedIdentityIds()) {
            if (identityId != null && identityId.length() > maxLength) {
                logOversizedIdentityPayload(player, payload, -1, "identity id exceeded max string length");
                notifyPlayerOversizedIdentityPayload(player);
                return false;
            }
        }
        for (UnlockedIdentitySyncS2CPacketPayload.VariantEntry entry : payload.unlockedVariantEntries()) {
            if (entry.identityId() != null && entry.identityId().length() > maxLength) {
                logOversizedIdentityPayload(player, payload, -1, "variant identity id exceeded max string length");
                notifyPlayerOversizedIdentityPayload(player);
                return false;
            }
            for (String token : entry.variantTokens()) {
                if (token != null && token.length() > maxLength) {
                    logOversizedIdentityPayload(player, payload, -1, "variant token exceeded max string length");
                    notifyPlayerOversizedIdentityPayload(player);
                    return false;
                }
            }
        }

        int serializedSize;
        try {
            serializedSize = getSerializedUnlockedIdentitiesSize(payload);
        } catch (RuntimeException exception) {
            Identity2.LOGGER.error(
                    "Failed to serialize unlocked identity payload for player={} uuid={} entityId={} payload={}",
                    player.getGameProfile().name(),
                    player.getUUID(),
                    player.getId(),
                    payload,
                    exception
            );
            notifyPlayerOversizedIdentityPayload(player);
            return false;
        }
        if (serializedSize > MAX_UNLOCKED_IDENTITY_SYNC_BYTES) {
            logOversizedIdentityPayload(player, payload, serializedSize, "serialized unlock payload exceeded safety limit");
            notifyPlayerOversizedIdentityPayload(player);
            return false;
        }
        return true;
    }

    public static void logOversizedIdentityPayload(ServerPlayer player, UnlockedIdentitySyncS2CPacketPayload payload, int serializedSize, String reason) {
        String playerName = player.getGameProfile().name();
        String playerUuid = player.getUUID().toString();
        int identityCount = payload.unlockedIdentityIds() == null ? 0 : payload.unlockedIdentityIds().size();
        int variantCount = payload.unlockedVariantEntries() == null ? 0 : payload.unlockedVariantEntries().size();
        String payloadText = String.valueOf(payload);
        Identity2.LOGGER.error(
                "Blocked oversized unlocked identity payload for player={} uuid={} entityId={} identityCount={} variantEntryCount={} serializedSize={} reason={} payload={}",
                playerName,
                playerUuid,
                player.getId(),
                identityCount,
                variantCount,
                serializedSize,
                reason,
                payloadText
        );
    }

    public static void notifyPlayerOversizedIdentityPayload(ServerPlayer player) {
        if (player == null) {
            return;
        }

        player.displayClientMessage(
                Component.literal(
                        "Too big identity packet detected. Please report this in my Discord server: https://discord.gg/2jRhTJgYz4 in issues with your logs attached."
                ),
                false
        );
    }

    public static void storeUnlockedIdentityData(CompoundTag customData, List<String> unlocked, Map<String, List<String>> variantUnlocks) {
        if (customData == null) {
            return;
        }

        List<String> normalizedUnlocked = normalizeUnlockedIdentityIds(unlocked);
        Map<String, List<String>> normalizedVariants = normalizeUnlockedIdentityVariantUnlocks(variantUnlocks);
        customData.store(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC, normalizedUnlocked);
        customData.store(UNLOCKED_IDENTITY_VARIANTS_KEY, STRING_LIST_MAP_CODEC, normalizedVariants);
        customData.remove(UNLOCKED_IDENTITIES_CACHE_KEY);
        customData.remove(UNLOCKED_IDENTITY_VARIANTS_CACHE_KEY);
    }

    public static Set<String> readUnlockedIdentityIdSet(CompoundTag customData) {
        return new LinkedHashSet<>(readUnlockedIdentityIds(customData));
    }

    public static Map<String, Set<String>> readUnlockedIdentityVariantTokenSet(CompoundTag customData) {
        Map<String, List<String>> raw = readUnlockedIdentityVariantUnlocks(customData);
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
            result.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        return result;
    }

    private static void ensureUnlockedIdentityStorage(ServerPlayer player) {
        if (player == null) {
            return;
        }

        CompoundTag customData = getCustomData(player);
        List<String> unlocked = normalizeUnlockedIdentityIds(readUnlockedIdentityIds(customData));
        Map<String, List<String>> variantUnlocks = normalizeUnlockedIdentityVariantUnlocks(readUnlockedIdentityVariantUnlocks(customData));
        storeUnlockedIdentityData(customData, unlocked, variantUnlocks);
    }

    private static List<String> readUnlockedIdentityIds(CompoundTag customData) {
        if (customData == null) {
            return List.of();
        }

        List<String> unlocked = customData.read(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of());
        if (!unlocked.isEmpty()) {
            return unlocked;
        }

        String legacy = customData.getStringOr(UNLOCKED_IDENTITIES_CACHE_KEY, "");
        if (legacy.isBlank()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String value : legacy.split(",")) {
            if (value != null && !value.isBlank()) {
                parsed.add(value.trim());
            }
        }
        return parsed;
    }

    private static Map<String, List<String>> readUnlockedIdentityVariantUnlocks(CompoundTag customData) {
        if (customData == null) {
            return Map.of();
        }

        Map<String, List<String>> unlockedVariants = new HashMap<>(
                customData.read(UNLOCKED_IDENTITY_VARIANTS_KEY, STRING_LIST_MAP_CODEC).orElse(Map.of())
        );
        if (!unlockedVariants.isEmpty()) {
            return unlockedVariants;
        }

        String legacy = customData.getStringOr(UNLOCKED_IDENTITY_VARIANTS_CACHE_KEY, "");
        if (legacy.isBlank()) {
            return Map.of();
        }

        Map<String, List<String>> parsed = new HashMap<>();
        for (String entry : legacy.split(",")) {
            String trimmed = entry == null ? "" : entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex >= trimmed.length() - 1) {
                continue;
            }

            String identityId = trimmed.substring(0, equalsIndex).trim();
            String tokenData = trimmed.substring(equalsIndex + 1).trim();
            if (identityId.isEmpty() || tokenData.isEmpty()) {
                continue;
            }

            List<String> tokens = new ArrayList<>();
            for (String token : tokenData.split("\\|")) {
                if (token != null && !token.isBlank()) {
                    tokens.add(token.trim());
                }
            }
            if (!tokens.isEmpty()) {
                parsed.put(identityId, tokens);
            }
        }
        return parsed;
    }

    private static List<String> normalizeUnlockedIdentityIds(List<String> unlocked) {
        if (unlocked == null || unlocked.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : unlocked) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> sorted = new ArrayList<>(normalized);
        Collections.sort(sorted);
        return sorted;
    }

    private static Map<String, List<String>> normalizeUnlockedIdentityVariantUnlocks(Map<String, List<String>> unlockedVariants) {
        if (unlockedVariants == null || unlockedVariants.isEmpty()) {
            return Map.of();
        }

        List<String> keys = new ArrayList<>(unlockedVariants.keySet());
        Collections.sort(keys);
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String trimmedKey = key.trim();
            if (trimmedKey.isEmpty()) {
                continue;
            }

            List<String> tokens = unlockedVariants.get(key);
            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            LinkedHashSet<String> normalizedTokens = new LinkedHashSet<>();
            for (String rawToken : tokens) {
                if (rawToken == null) {
                    continue;
                }
                String trimmedToken = rawToken.trim();
                if (!trimmedToken.isEmpty()) {
                    normalizedTokens.add(trimmedToken);
                }
            }
            if (normalizedTokens.isEmpty()) {
                continue;
            }

            List<String> sortedTokens = new ArrayList<>(normalizedTokens);
            Collections.sort(sortedTokens);
            normalized.put(trimmedKey, sortedTokens);
        }
        return normalized;
    }

    private static CompoundTag resolveRandomUnlockedVariant(CompoundTag customData, ResourceLocation identityId, int seed) {
        if (customData == null || identityId == null) {
            return new CompoundTag();
        }
        Map<String, List<String>> variantUnlocks = readUnlockedIdentityVariantUnlocks(customData);
        List<String> tokens = variantUnlocks.get(identityId.toString());
        if (tokens == null || tokens.isEmpty()) {
            return new CompoundTag();
        }
        int index = Math.floorMod(seed, tokens.size());
        return fromVariantUnlockToken(tokens.get(index));
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

    private static void applyMorphDamageGrace(
            ServerPlayer player,
            CompoundTag nbt,
            double previousWidth,
            double previousHeight,
            double nextWidth,
            double nextHeight
    ) {
        if (player == null || nbt == null) {
            return;
        }
        boolean grew = (nextWidth - previousWidth) > 1.0E-4D || (nextHeight - previousHeight) > 1.0E-4D;
        if (!grew) {
            clearMorphDamageGrace(nbt);
            return;
        }
        double endTick = (player.level() == null ? 0.0D : player.level().getGameTime()) + 40.0D;
        nbt.putDouble(MORPH_DAMAGE_GRACE_END_TICK_KEY, endTick);
    }

    private static void clearMorphDamageGrace(CompoundTag nbt) {
        if (nbt == null) {
            return;
        }
        nbt.putDouble(MORPH_DAMAGE_GRACE_END_TICK_KEY, 0.0D);
    }

    private static void ensureSafePostResizePosition(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().isClientSide() || player.noPhysics || player.isSpectator()) {
            return;
        }
        if (player.level().noCollision(player, player.getBoundingBox())) {
            return;
        }

        Vec3 origin = player.position();
        double verticalStep = 0.5D;
        double step = Math.max(0.5D, player.getBbWidth() * 0.5D);
        double maxRadius = Math.max(2.0D, player.getBbWidth() * 2.0D);
        int radialSamples = 16;
        for (int y = 0; y <= 12; y++) {
            double yOffset = y * verticalStep;
            if (identity2$tryRelocatePlayer(player, origin.add(0.0D, yOffset, 0.0D))) {
                return;
            }
            for (double radius = step; radius <= maxRadius; radius += step) {
                for (int i = 0; i < radialSamples; i++) {
                    double angle = (Math.PI * 2.0D * i) / radialSamples;
                    Vec3 candidate = origin.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
                    if (identity2$tryRelocatePlayer(player, candidate)) {
                        return;
                    }
                }
            }
        }

        player.setPos(origin.x, origin.y, origin.z);
    }

    private static boolean identity2$tryRelocatePlayer(ServerPlayer player, Vec3 candidate) {
        player.setPos(candidate.x, candidate.y, candidate.z);
        return player.level().noCollision(player, player.getBoundingBox());
    }


    private static CompoundTag getCustomData(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
    }

    private static void applyMorphAttributes(ServerPlayer player, @Nullable Entity identity) {
        if (player == null) {
            return;
        }

        AttributeMap playerAttributes = player.getAttributes();
        if (playerAttributes == null) {
            return;
        }

        clearMorphAttributes(playerAttributes);

        if (!(identity instanceof LivingEntity livingIdentity)) {
            return;
        }

        AttributeMap sourceAttributes = livingIdentity.getAttributes();
        for (AttributeInstance sourceInstance : getAttributeInstances(sourceAttributes)) {
            if (sourceInstance == null) {
                continue;
            }

            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute = sourceInstance.getAttribute();
            if (attribute == null || identity2$shouldSkipPlayerMorphAttribute(attribute)) {
                continue;
            }

            if (attribute.equals(Attributes.MOVEMENT_SPEED)) {
                continue;
            }

            AttributeInstance targetInstance = identity2$ensureMorphAttributeInstance(playerAttributes, sourceAttributes, attribute);
            if (targetInstance == null) {
                continue;
            }

            String attributeKey = resolveAttributeKey(attribute);
            ResourceLocation baseModifierId = morphAttributeBaseModifierId(attributeKey);
            targetInstance.removeModifier(baseModifierId);

            double delta = sourceInstance.getBaseValue() - targetInstance.getBaseValue();
            if (Math.abs(delta) > 1.0E-4D) {
                targetInstance.addOrUpdateTransientModifier(
                    new AttributeModifier(baseModifierId, delta, AttributeModifier.Operation.ADD_VALUE)
                );
            }

            for (AttributeModifier sourceModifier : sourceInstance.getModifiers()) {
                if (sourceModifier == null) {
                    continue;
                }

                AttributeModifier.Operation operation = identity2$getModifierOperation(sourceModifier);
                if (operation == null) {
                    continue;
                }

                ResourceLocation copiedModifierId = morphAttributeModifierId(attributeKey, sourceModifier);
                targetInstance.removeModifier(copiedModifierId);
                targetInstance.addOrUpdateTransientModifier(
                    new AttributeModifier(copiedModifierId, identity2$getModifierAmount(sourceModifier), operation)
                );
            }
        }
    }

    @Nullable
    private static AttributeInstance identity2$ensureMorphAttributeInstance(
        @Nullable AttributeMap targetAttributes,
        @Nullable AttributeMap sourceAttributes,
        @Nullable Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute
    ) {
        if (targetAttributes == null || attribute == null) {
            return null;
        }

        AttributeInstance targetInstance = targetAttributes.getInstance(attribute);
        if (targetInstance != null) {
            return targetInstance;
        }

        AttributeInstance template = identity2$findAttributeTemplate(sourceAttributes, attribute);
        if (template == null) {
            return null;
        }

        Map<Object, AttributeInstance> targetTemplates = identity2$getDefaultAttributeTemplates(targetAttributes);
        if (targetTemplates == null) {
            return null;
        }

        Object attributeKey = identity2$resolveTemplateKey(sourceAttributes, attribute, template);
        if (attributeKey == null) {
            attributeKey = attribute;
        }

        Map<Object, AttributeInstance> rebuiltTemplates = new LinkedHashMap<>(targetTemplates);
        rebuiltTemplates.putIfAbsent(attributeKey, template);
        AttributeSupplier rebuiltSupplier = identity2$createAttributeSupplier(rebuiltTemplates);
        if (rebuiltSupplier == null) {
            return null;
        }
        ((AttributeContainerAccessor) targetAttributes).setDefaultAttributes(rebuiltSupplier);
        return targetAttributes.getInstance(attribute);
    }

    private static void clearMorphAttributes(AttributeMap attributes) {
        for (AttributeInstance instance : getAttributeInstances(attributes)) {
            if (instance == null) {
                continue;
            }

            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute = instance.getAttribute();
            if (attribute == null || identity2$shouldSkipPlayerMorphAttribute(attribute)) {
                continue;
            }

            instance.removeModifier(morphAttributeBaseModifierId(resolveAttributeKey(attribute)));
            List<ResourceLocation> morphModifierIds = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                ResourceLocation modifierId = identity2$getModifierId(modifier);
                if (identity2$isMorphAttributeModifier(modifierId)) {
                    morphModifierIds.add(modifierId);
                }
            }
            for (ResourceLocation modifierId : morphModifierIds) {
                instance.removeModifier(modifierId);
            }
        }
    }

    private static List<AttributeInstance> getAttributeInstances(@Nullable AttributeMap attributes) {
        if (attributes == null) {
            return List.of();
        }
        Map<String, AttributeInstance> resolved = new LinkedHashMap<>();

        try {
            for (AttributeInstance instance : attributes.getSyncableAttributes()) {
                if (instance == null || instance.getAttribute() == null) {
                    continue;
                }
                resolved.putIfAbsent(resolveAttributeKey(instance.getAttribute()), instance);
            }
        } catch (Throwable ignored) {
        }

        try {
            for (AttributeInstance instance : ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) attributes).getDefaultAttributes())
                .getInstances()
                .values()) {
                if (instance == null || instance.getAttribute() == null) {
                    continue;
                }
                resolved.putIfAbsent(resolveAttributeKey(instance.getAttribute()), instance);
            }
        } catch (Throwable ignored) {
        }

        return new ArrayList<>(resolved.values());
    }

    @Nullable
    private static AttributeInstance identity2$findAttributeTemplate(
        @Nullable AttributeMap attributes,
        @Nullable Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute
    ) {
        if (attributes == null || attribute == null) {
            return null;
        }

        Map<Object, AttributeInstance> defaultTemplates = identity2$getDefaultAttributeTemplates(attributes);
        if (defaultTemplates != null) {
            for (AttributeInstance instance : defaultTemplates.values()) {
                if (instance != null && attribute.equals(instance.getAttribute())) {
                    return instance;
                }
            }
        }

        return attributes.getInstance(attribute);
    }

    @Nullable
    private static Object identity2$resolveTemplateKey(
        @Nullable AttributeMap attributes,
        @Nullable Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
        @Nullable AttributeInstance expectedInstance
    ) {
        if (attributes == null || attribute == null) {
            return null;
        }

        Map<Object, AttributeInstance> defaultTemplates = identity2$getDefaultAttributeTemplates(attributes);
        if (defaultTemplates == null) {
            return null;
        }

        for (Map.Entry<Object, AttributeInstance> entry : defaultTemplates.entrySet()) {
            AttributeInstance instance = entry.getValue();
            if (instance == null) {
                continue;
            }
            if (instance == expectedInstance || attribute.equals(instance.getAttribute())) {
                return entry.getKey();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Map<Object, AttributeInstance> identity2$getDefaultAttributeTemplates(@Nullable AttributeMap attributes) {
        if (attributes == null) {
            return null;
        }
        try {
            return (Map<Object, AttributeInstance>) (Map<?, ?>) ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) attributes)
                .getDefaultAttributes())
                .getInstances();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static AttributeSupplier identity2$createAttributeSupplier(Map<Object, AttributeInstance> templates) {
        if (templates == null) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<AttributeSupplier> constructor = AttributeSupplier.class.getDeclaredConstructor(Map.class);
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(templates);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String resolveAttributeKey(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        if (attribute == null) {
            return "unknown";
        }
        try {
            ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
            if (attributeId != null) {
                return attributeId.toString();
            }
        } catch (Throwable ignored) {
        }
        return attribute.toString();
    }

    private static ResourceLocation morphAttributeBaseModifierId(String attributeKey) {
        return identity2$morphModifierId(MORPH_ATTRIBUTE_BASE_MODIFIER_PREFIX, attributeKey);
    }

    private static ResourceLocation morphAttributeModifierId(String attributeKey, AttributeModifier modifier) {
        String token = String.valueOf(identity2$getModifierId(modifier));
        if ("null".equals(token)) {
            token = identity2$getModifierAmount(modifier) + "|" + String.valueOf(identity2$getModifierOperation(modifier));
        }
        return identity2$morphModifierId(MORPH_ATTRIBUTE_MODIFIER_PREFIX, attributeKey + "|" + token);
    }

    private static ResourceLocation identity2$morphModifierId(String prefix, String token) {
        String hash = java.util.UUID.nameUUIDFromBytes((prefix + token).getBytes(StandardCharsets.UTF_8)).toString().replace('-', '_');
        return ResourceLocation.fromNamespaceAndPath(Identity2.MOD_ID, prefix + hash);
    }

    private static boolean identity2$isMorphAttributeModifier(@Nullable ResourceLocation modifierId) {
        if (modifierId == null || !Identity2.MOD_ID.equals(modifierId.getNamespace())) {
            return false;
        }
        String path = modifierId.getPath();
        return path.startsWith(MORPH_ATTRIBUTE_BASE_MODIFIER_PREFIX) || path.startsWith(MORPH_ATTRIBUTE_MODIFIER_PREFIX);
    }

    private static boolean identity2$shouldSkipPlayerMorphAttribute(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        if (attribute == null) {
            return true;
        }
        return attribute.equals(Attributes.MAX_HEALTH);
    }

    @Nullable
    private static ResourceLocation identity2$getModifierId(@Nullable AttributeModifier modifier) {
        Object value = invokeNoArg(modifier, "id");
        if (!(value instanceof ResourceLocation)) {
            value = invokeNoArg(modifier, "getId");
        }
        return value instanceof ResourceLocation id ? id : null;
    }

    private static double identity2$getModifierAmount(@Nullable AttributeModifier modifier) {
        Object value = invokeNoArg(modifier, "amount");
        if (!(value instanceof Number)) {
            value = invokeNoArg(modifier, "getAmount");
        }
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    @Nullable
    private static AttributeModifier.Operation identity2$getModifierOperation(@Nullable AttributeModifier modifier) {
        Object value = invokeNoArg(modifier, "operation");
        if (!(value instanceof AttributeModifier.Operation)) {
            value = invokeNoArg(modifier, "getOperation");
        }
        return value instanceof AttributeModifier.Operation operation ? operation : null;
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

        if (!IdentitySettings.scalingHealth) {
            float newMaxHealth = player.getMaxHealth();
            float scaled = Mth.clamp(healthRatio * newMaxHealth, 1.0F, newMaxHealth);
            player.setHealth(scaled);
            return;
        }
        if (identity instanceof LivingEntity livingIdentity) {
            double base = maxHealthAttr.getBaseValue();
            double desired = resolveIdentityMaxHealth(player, livingIdentity);
            desired = Math.max(1.0D, Math.min(desired, Math.max(1, IdentitySettings.maxHealth)));
            double delta = desired - base;
            if (Math.abs(delta) > 1.0E-4D) {
                maxHealthAttr.addOrUpdateTransientModifier(
                        new AttributeModifier(HEALTH_SCALING_MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE)
                );
            }
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

    public static String toVariantUnlockToken(CompoundTag variantNbt) {
        if (variantNbt == null || variantNbt.isEmpty()) {
            return "-";
        }
        String raw = serializeVariantNbt(variantNbt);
        if (raw.isBlank()) {
            return "-";
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CompoundTag fromVariantUnlockToken(String token) {
        if (token == null || token.isBlank() || "-".equals(token)) {
            return new CompoundTag();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            return parseVariantNbt(raw);
        } catch (Throwable ignored) {
            return new CompoundTag();
        }
    }

    private static CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag variant = new CompoundTag();
        if (entity == null || entity.level() == null) {
            return variant;
        }
        CompoundTag dynamicVariant = IdentityVariantNbtHelper.computeVariantDiff(entity);
        if (dynamicVariant != null && !dynamicVariant.isEmpty()) {
            variant.merge(dynamicVariant);
        }
        try {
            TagValueOutput writeView = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
            entity.saveWithoutId(writeView);
            CompoundTag full = writeView.buildResult();
            copyVariantKey(full, variant, "Color");
            copyVariantKey(full, variant, "Variant");
            copyVariantKey(full, variant, "variant");
            copyVariantKey(full, variant, "Type");
            copyVariantKey(full, variant, "type");
            copyVariantKey(full, variant, "Skin");
            copyVariantKey(full, variant, "skin");
            copyVariantKey(full, variant, "Form");
            copyVariantKey(full, variant, "form");
            copyVariantKey(full, variant, "Age");
            copyVariantKey(full, variant, "IsBaby");
            copyVariantKey(full, variant, "Baby");
            copyVariantKey(full, variant, "AgeLocked");
            copyVariantKey(full, variant, "VillagerData");
            copyVariantKey(full, variant, "Profession");
            copyVariantKey(full, variant, "profession");
            copyVariantKey(full, variant, "Type");
            copyVariantKey(full, variant, "type");
            extractAnimalVariantData(entity, variant);
            extractVillagerVariantData(entity, variant);
            Boolean isBaby = detectBabyState(entity, variant);
            if (isBaby != null) {
                variant.putBoolean("IsBaby", isBaby);
            }
        } catch (Throwable ignored) {
        }
        CompoundTag adapterVariant = IdentityApi.extractVariantData(entity);
        if (adapterVariant != null && !adapterVariant.isEmpty()) {
            variant.merge(adapterVariant);
        }
        return normalizeVariantForUnlock(variant);
    }

    @Nullable
    private static Boolean detectBabyState(LivingEntity entity, CompoundTag variant) {
        if (entity == null) {
            return null;
        }
        if (variant != null) {
            if (variant.getBoolean("IsBaby").isPresent()) {
                return variant.getBooleanOr("IsBaby", false);
            }
            if (variant.getBoolean("Baby").isPresent()) {
                return variant.getBooleanOr("Baby", false);
            }
            if (variant.getInt("Age").isPresent()) {
                return variant.getInt("Age").get() < 0;
            }
        }

        Object isBaby = invokeNoArg(entity, "isBaby");
        if (isBaby instanceof Boolean value) {
            return value;
        }
        Object isChild = invokeNoArg(entity, "isChild");
        if (isChild instanceof Boolean value) {
            return value;
        }
        Object age = invokeNoArg(entity, "getAge");
        if (age instanceof Number number) {
            return number.intValue() < 0;
        }
        return null;
    }

    public static CompoundTag normalizeVariantForUnlock(CompoundTag source) {
        return sanitizeVariantNbt(source, true);
    }

    public static boolean matchesStoredVariantToken(CompoundTag requestedVariantNbt, String storedToken) {
        if (storedToken == null || storedToken.isBlank()) {
            return false;
        }
        CompoundTag requested = normalizeVariantForUnlock(requestedVariantNbt);
        CompoundTag stored = normalizeVariantForUnlock(fromVariantUnlockToken(storedToken));
        return isVariantEquivalent(requested, stored);
    }

    public static boolean isVariantEquivalent(CompoundTag first, CompoundTag second) {
        CompoundTag left = normalizeVariantForUnlock(first);
        CompoundTag right = normalizeVariantForUnlock(second);
        if (tagEquivalent(left, right)) {
            return true;
        }
        // Accept subset matches to tolerate noisy or partial historical tokens.
        return compoundContains(left, right) || compoundContains(right, left);
    }

    private static CompoundTag sanitizeVariantNbt(CompoundTag source, boolean root) {
        if (source == null || source.isEmpty()) {
            return new CompoundTag();
        }
        CompoundTag out = new CompoundTag();
        for (String key : source.keySet()) {
            if (root && NON_VARIANT_ROOT_KEYS.contains(key)) {
                continue;
            }
            Tag tag = source.get(key);
            if (tag == null) {
                continue;
            }
            if (tag instanceof CompoundTag nested) {
                CompoundTag sanitizedNested = sanitizeVariantNbt(nested, false);
                if (!sanitizedNested.isEmpty()) {
                    out.put(key, sanitizedNested);
                }
                continue;
            }
            out.put(key, tag.copy());
        }
        return out;
    }

    private static boolean compoundContains(CompoundTag container, CompoundTag subset) {
        if (subset == null || subset.isEmpty()) {
            return true;
        }
        if (container == null || container.isEmpty()) {
            return false;
        }
        for (String key : subset.keySet()) {
            Tag required = subset.get(key);
            Tag actual = container.get(key);
            if (required == null) {
                continue;
            }
            if (actual == null || !tagEquivalent(actual, required)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tagEquivalent(Tag left, Tag right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof NumericTag leftNum && right instanceof NumericTag rightNum) {
            return Double.compare(leftNum.doubleValue(), rightNum.doubleValue()) == 0;
        }
        if (left instanceof CompoundTag leftCompound && right instanceof CompoundTag rightCompound) {
            return compoundContains(leftCompound, rightCompound) && compoundContains(rightCompound, leftCompound);
        }
        return left.equals(right);
    }

    private static void copyVariantKey(CompoundTag source, CompoundTag target, String key) {
        if (source == null || target == null || key == null || key.isBlank()) {
            return;
        }
        if (source.getByte(key).isPresent()) {
            target.putByte(key, source.getByte(key).get());
            return;
        }
        if (source.getShort(key).isPresent()) {
            target.putShort(key, source.getShort(key).get());
            return;
        }
        if (source.getInt(key).isPresent()) {
            target.putInt(key, source.getInt(key).get());
            return;
        }
        if (source.getLong(key).isPresent()) {
            target.putLong(key, source.getLong(key).get());
            return;
        }
        if (source.getBoolean(key).isPresent()) {
            target.putBoolean(key, source.getBooleanOr(key, false));
            return;
        }
        if (source.getString(key).isPresent()) {
            String value = source.getStringOr(key, "");
            if (!value.isBlank()) {
                target.putString(key, value);
            }
        }
    }

    private static void extractVillagerVariantData(LivingEntity entity, CompoundTag variant) {
        if (entity == null || variant == null) {
            return;
        }
        Object villagerData = invokeNoArg(entity, "getVillagerData");
        if (villagerData == null) {
            return;
        }

        Object profession = invokeNoArg(villagerData, "getProfession");
        ResourceLocation professionId = resolveRegistryResourceLocation("VILLAGER_PROFESSION", profession);
        if (professionId != null) {
            variant.putString("VillagerProfession", professionId.toString());
        }

        Object villagerType = invokeNoArg(villagerData, "getType");
        ResourceLocation typeId = resolveRegistryResourceLocation("VILLAGER_TYPE", villagerType);
        if (typeId != null) {
            variant.putString("VillagerType", typeId.toString());
        }

        Object level = invokeNoArg(villagerData, "getLevel");
        if (level instanceof Number number) {
            variant.putInt("VillagerLevel", Math.max(1, number.intValue()));
        }
    }

    private static void extractAnimalVariantData(LivingEntity entity, CompoundTag variant) {
        if (entity == null || variant == null) {
            return;
        }

        // Sheep and other dyeable entities expose color through getColor() and may omit it in default NBT.
        if (!variant.contains("Color")) {
            Object color = invokeNoArg(entity, "getColor");
            Integer colorId = resolveDyeColorId(color);
            if (colorId != null) {
                int clamped = Math.max(0, Math.min(255, colorId));
                variant.putByte("Color", (byte) clamped);
            }
        }

        Object variantValue = invokeNoArg(entity, "getVariant");
        if (!variant.contains("Variant") && !variant.contains("variant")) {
            Integer variantId = resolveNumericVariantValue(variantValue);
            if (variantId != null) {
                variant.putInt("Variant", variantId);
            } else {
                String variantName = resolveVariantStringValue(variantValue);
                if (variantName != null && !variantName.isBlank()) {
                    variant.putString("Variant", variantName);
                }
            }
        }
        ResourceLocation catVariantId = resolveRegistryResourceLocation("CAT_VARIANT", variantValue);
        if (catVariantId != null) {
            variant.putString("CatVariant", catVariantId.toString());
        }

        ResourceLocation wolfVariantId = resolveRegistryResourceLocation("WOLF_VARIANT", variantValue);
        if (wolfVariantId != null) {
            variant.putString("WolfVariant", wolfVariantId.toString());
        }

        ResourceLocation frogVariantId = resolveRegistryResourceLocation("FROG_VARIANT", variantValue);
        if (frogVariantId != null) {
            variant.putString("FrogVariant", frogVariantId.toString());
        }

        Object collarColor = invokeNoArg(entity, "getCollarColor");
        Integer collarColorId = resolveDyeColorId(collarColor);
        if (collarColorId != null) {
            variant.putInt("CollarColor", Math.max(0, collarColorId));
        }
    }

    private static Integer resolveNumericVariantValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.ordinal();
        }
        Object id = invokeNoArg(value, "getId");
        if (id instanceof Number number) {
            return number.intValue();
        }
        Object ordinal = invokeNoArg(value, "ordinal");
        if (ordinal instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String resolveVariantStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ResourceLocation identifier) {
            return identifier.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name().toLowerCase(Locale.ROOT);
        }
        Object serialized = invokeNoArg(value, "getSerializedName");
        if (serialized instanceof String string && !string.isBlank()) {
            return string;
        }
        Object asString = invokeNoArg(value, "asString");
        if (asString instanceof String string && !string.isBlank()) {
            return string;
        }
        return null;
    }

    private static Integer resolveDyeColorId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        Object id = invokeNoArg(value, "getId");
        if (id instanceof Number number) {
            return number.intValue();
        }
        Object ordinal = invokeNoArg(value, "ordinal");
        if (ordinal instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank() || arg == null) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!isAssignable(paramType, arg.getClass())) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                return method.invoke(target, arg);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean identity2$hasCompletedAdvancement(ServerPlayer player, String advancementId) {
        if (player == null || advancementId == null || advancementId.isBlank() || player.level() == null || player.level().getServer() == null) {
            return false;
        }

        ResourceLocation advancementIdentifier;
        try {
            advancementIdentifier = ResourceLocation.parse(advancementId.trim());
        } catch (Exception ignored) {
            return false;
        }

        Object advancementManager = invokeNoArg(player.level().getServer(), "getAdvancements");
        if (advancementManager == null) {
            return false;
        }

        Object holder = invokeOneArg(advancementManager, "get", advancementIdentifier);
        if (holder == null) {
            return false;
        }

        Object playerAdvancements = invokeNoArg(player, "getAdvancements");
        if (playerAdvancements == null) {
            return false;
        }

        Object progress = invokeOneArg(playerAdvancements, "getOrStartProgress", holder);
        if (progress == null) {
            return false;
        }

        Object completed = invokeNoArg(progress, "isDone");
        return completed instanceof Boolean done && done;
    }

    private static ResourceLocation resolveRegistryResourceLocation(String registryField, Object value) {
        if (registryField == null || registryField.isBlank() || value == null) {
            return null;
        }
        Registry<?> registry = getBuiltInRegistry(registryField);
        if (registry == null) {
            return null;
        }
        ResourceLocation direct = getRegistryKey(registry, value);
        if (direct != null) {
            return direct;
        }
        Object unwrapped = unwrapHolderValue(value);
        if (unwrapped != null) {
            return getRegistryKey(registry, unwrapped);
        }
        return null;
    }

    private static Registry<?> getBuiltInRegistry(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Object value = BuiltInRegistries.class.getField(fieldName).get(null);
            if (value instanceof Registry<?> registry) {
                return registry;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object key = Registries.class.getField(fieldName).get(null);
            if (key instanceof net.minecraft.resources.ResourceKey<?> resourceKey) {
                ResourceLocation location = null;
                Object byLocation = invokeNoArg(resourceKey, "location");
                if (byLocation instanceof ResourceLocation id) {
                    location = id;
                } else {
                    Object byResourceLocation = invokeNoArg(resourceKey, "ResourceLocation");
                    if (byResourceLocation instanceof ResourceLocation id) {
                        location = id;
                    } else {
                        Object byRegistry = invokeNoArg(resourceKey, "registry");
                        if (byRegistry instanceof ResourceLocation id2) {
                            location = id2;
                        }
                    }
                }
                if (location != null) {
                    Object value = BuiltInRegistries.REGISTRY.getValue(location);
                    if (value instanceof Registry<?> registry) {
                        return registry;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation getRegistryKey(Registry<?> registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        try {
            return ((Registry<Object>) registry).getKey(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object unwrapHolderValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("value");
            if (method.getParameterCount() == 0) {
                return method.invoke(value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static List<Method> getAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> param : params) {
                    signature += ":" + param.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType == int.class && argType == Integer.class) {
            return true;
        }
        if (paramType == boolean.class && argType == Boolean.class) {
            return true;
        }
        if (paramType == byte.class && argType == Byte.class) {
            return true;
        }
        if (paramType == short.class && argType == Short.class) {
            return true;
        }
        if (paramType == long.class && argType == Long.class) {
            return true;
        }
        if (paramType == float.class && argType == Float.class) {
            return true;
        }
        return paramType == double.class && argType == Double.class;
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

    private record UnlockTarget(ResourceLocation identityId, CompoundTag variantNbt) {
    }
}
