package net.Gabou.identity2.identity;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class IdentityProgression {
    private static final String UNLOCKED_IDENTITIES_KEY = "identity2.unlocked_identities";
    private static final String IDENTITY_KILL_COUNTS_KEY = "identity2.identity_kill_counts";
    public static final String UNLOCKED_IDENTITIES_CACHE_KEY = "identity2.unlocked_identities_cache";
    public static final String SELECTED_IDENTITY_TYPE_KEY = "identity2.identity_type";
    public static final String SELECTED_IDENTITY_VARIANT_KEY = "identity2.identity_variant";
    private static final Codec<List<String>> STRING_LIST_CODEC = Codec.STRING.listOf();
    private static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
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

    public static List<String> getUnlockedIdentities(ServerPlayerEntity player) {
        ensureClientUnlockCache(player);
        return new ArrayList<>(getCustomData(player).get(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
    }

    public static boolean isUnlocked(ServerPlayerEntity player, Identifier identityId) {
        return getUnlockedIdentities(player).contains(identityId.toString());
    }

    public static boolean isMorphableIdentity(Identifier identityId) {
        if (identityId == null || !Registries.ENTITY_TYPE.containsId(identityId)) {
            return false;
        }
        return isMorphableType(Registries.ENTITY_TYPE.get(identityId));
    }

    public static boolean isMorphableType(EntityType<?> entityType) {
        if (entityType == null || entityType == EntityType.PLAYER) {
            return false;
        }
        if (entityType.getSpawnGroup() == SpawnGroup.MISC) {
            return false;
        }
        // 1.21.11 mappings return Entity.class from EntityType#getBaseClass(),
        // so class-based living checks are unreliable here.
        return true;
    }

    public static void morph(ServerPlayerEntity player, Identifier identityId) {
        morph(player, identityId, new NbtCompound());
    }

    public static void morph(ServerPlayerEntity player, Identifier identityId, NbtCompound variantNbt) {
        NbtComponent customData = ((EntityAccessor) player).getCustomData();
        String value = identityId.toString();
        NbtCompound safeVariant = variantNbt == null ? new NbtCompound() : variantNbt.copy();
        String serializedVariant = serializeVariantNbt(safeVariant);
        NbtCompound nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        nbt.putString("model_override", value);
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, value);
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, serializedVariant);
        ((EntityAccessor) player).setCurrentIdentity(identityId.toString(), safeVariant);

        double widthOverride = 0.0;
        double heightOverride = 0.0;
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity != null) {
            widthOverride = identity.getDimensions(identity.getPose()).width();
            heightOverride = identity.getDimensions(identity.getPose()).height();
            ((EntityAccessor) player).setEntityDimensions(identity.getDimensions(identity.getPose()));
            ((EntityAccessor) player).setStandingEyeHeight(identity.getStandingEyeHeight());
        }

        nbt.putDouble("width_override", widthOverride);
        nbt.putDouble("height_override", heightOverride);
        syncMorphData(player, value, serializedVariant, widthOverride, heightOverride);
    }

    public static void clearMorph(ServerPlayerEntity player) {
        NbtComponent customData = ((EntityAccessor) player).getCustomData();
        NbtCompound nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        nbt.putString("model_override", "");
        nbt.putString(SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        ((EntityAccessor) player).setCurrentIdentity("");
        ((EntityAccessor) player).setEntityDimensions(player.getDimensions(player.getPose()));
        ((EntityAccessor) player).setStandingEyeHeight(player.getStandingEyeHeight());
        syncMorphData(player, "", "", 0.0, 0.0);
    }

    public static void ensureClientUnlockCache(ServerPlayerEntity player) {
        NbtCompound customData = getCustomData(player);
        List<String> unlocked = new ArrayList<>(customData.get(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC).orElse(List.of()));
        customData.putString(UNLOCKED_IDENTITIES_CACHE_KEY, serializeUnlocked(unlocked));
    }

    private static EventResult onLivingDeath(LivingEntity killed, DamageSource source) {
        if (!IdentitySettings.enableIdentityKillUnlocks || killed.getEntityWorld().isClient()) {
            return EventResult.pass();
        }

        Entity attacker = source.getAttacker();
        ServerPlayerEntity player = null;
        if (attacker instanceof ServerPlayerEntity directPlayer) {
            player = directPlayer;
        } else if (attacker != null) {
            Entity owner = ((EntityAccessor) attacker).getIdentityOwner();
            if (owner instanceof ServerPlayerEntity ownerPlayer) {
                player = ownerPlayer;
            }
        }

        if (player == null) {
            return EventResult.pass();
        }

        Identifier identityId = Registries.ENTITY_TYPE.getId(killed.getType());
        if (!isMorphableIdentity(identityId)) {
            return EventResult.pass();
        }

        int kills = incrementKillCount(player, identityId);
        int requiredKills = Math.max(1, IdentitySettings.identityKillsRequired);
        if (kills < requiredKills) {
            return EventResult.pass();
        }

        if (unlockIdentity(player, identityId)) {
            player.sendMessage(Text.literal("Unlocked identity: " + identityId), false);
            Identity2.LOGGER.info("Unlocked identity {} for {}", identityId, player.getName().getString());
        }

        return EventResult.pass();
    }

    private static int incrementKillCount(ServerPlayerEntity player, Identifier identityId) {
        NbtCompound nbt = getCustomData(player);
        Map<String, Integer> killMap = new HashMap<>(nbt.get(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
        int kills = killMap.getOrDefault(identityId.toString(), 0) + 1;
        killMap.put(identityId.toString(), kills);
        nbt.put(IDENTITY_KILL_COUNTS_KEY, STRING_INT_MAP_CODEC, killMap);
        return kills;
    }

    private static boolean unlockIdentity(ServerPlayerEntity player, Identifier identityId) {
        List<String> unlocked = getUnlockedIdentities(player);
        String key = identityId.toString();
        if (unlocked.contains(key)) {
            return false;
        }

        unlocked.add(key);
        NbtCompound customData = getCustomData(player);
        customData.put(UNLOCKED_IDENTITIES_KEY, STRING_LIST_CODEC, unlocked);
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

    private static void syncMorphData(ServerPlayerEntity player, String modelOverride, String variant, double widthOverride, double heightOverride) {
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
        if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity other : serverWorld.getPlayers()) {
                if (other != player) {
                    NetworkManager.sendToPlayer(other, modelPayload);
                    NetworkManager.sendToPlayer(other, shapePayload);
                }
            }
        }
    }

    private static NbtCompound getCustomData(ServerPlayerEntity player) {
        NbtComponent customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
    }

    public static String serializeVariantNbt(NbtCompound variantNbt) {
        if (variantNbt == null || variantNbt.isEmpty()) {
            return "";
        }
        return variantNbt.copy().toString();
    }

    public static NbtCompound parseVariantNbt(String raw) {
        if (raw == null || raw.isBlank()) {
            return new NbtCompound();
        }
        try {
            return NbtCompoundArgumentType.nbtCompound().parse(new StringReader(raw));
        } catch (Exception ignored) {
            return new NbtCompound();
        }
    }
}
