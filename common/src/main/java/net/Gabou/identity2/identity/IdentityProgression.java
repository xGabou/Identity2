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
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.component.CustomData;

public final class IdentityProgression {
    // Sheep wool visual shape looks wider than the base collision box in this morph setup.
    // Keep this tunable to match in-game feel.
    private static final double SHEEP_WIDTH_COLLISION_SCALE = 1.2D;

    private static final String UNLOCKED_IDENTITIES_KEY = "identity2.unlocked_identities";
    private static final String IDENTITY_KILL_COUNTS_KEY = "identity2.identity_kill_counts";
    public static final String UNLOCKED_IDENTITIES_CACHE_KEY = "identity2.unlocked_identities_cache";
    public static final String SELECTED_IDENTITY_TYPE_KEY = "identity2.identity_type";
    public static final String SELECTED_IDENTITY_VARIANT_KEY = "identity2.identity_variant";
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
            syncMorphData(player, "", "", 0.0, 0.0);
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
        syncMorphData(player, value, serializedVariant, widthOverride, heightOverride);
    }

    public static void clearMorph(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        nbt.putString("model_override", "");
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        ((EntityAccessor) player).setCurrentIdentity("");
        ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
        ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
        syncMorphData(player, "", "", 0.0, 0.0);
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
            ((EntityAccessor) player).setCurrentIdentity("");
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
            return;
        }

        CompoundTag variant = parseVariantNbt(nbt.getStringOr(SELECTED_IDENTITY_VARIANT_KEY, ""));
        ((EntityAccessor) player).setCurrentIdentity(type, variant);
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            nbt.putDouble("width_override", 0.0);
            nbt.putDouble("height_override", 0.0);
            ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(player.getEyeHeight());
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
        syncMorphData(player, modelOverride, variant, widthOverride, heightOverride);
    }

    public static void ensureClientUnlockCache(ServerPlayer player) {
        CompoundTag customData = getCustomData(player);
        List<String> unlocked = new ArrayList<>(customData.read(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
        customData.putString(UNLOCKED_IDENTITIES_CACHE_KEY, serializeUnlocked(unlocked));
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

        Identifier identityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
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
        }

        return EventResult.pass();
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

    private static void syncMorphData(ServerPlayer player, String modelOverride, String variant, double widthOverride, double heightOverride) {
        CustomEntityStringDataS2CPacketPayload modelPayload = new CustomEntityStringDataS2CPacketPayload(
            player.getId(),
            List.of(
                new CustomEntityDataS2CPacket.EntryString("model_override", modelOverride),
                new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_TYPE_KEY, modelOverride),
                new CustomEntityDataS2CPacket.EntryString(SELECTED_IDENTITY_VARIANT_KEY, variant)
            )
        );
        CustomEntityDataS2CPacketPayload shapePayload = new CustomEntityDataS2CPacketPayload(
            player.getId(),
            List.of(
                new CustomEntityDataS2CPacket.Entry("width_override", widthOverride),
                new CustomEntityDataS2CPacket.Entry("height_override", heightOverride)
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

    private static CompoundTag getCustomData(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
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
