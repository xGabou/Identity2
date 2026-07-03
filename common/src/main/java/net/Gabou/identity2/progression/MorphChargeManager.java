package net.Gabou.identity2.progression;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MorphChargeManager {
    private static final String MORPH_CHARGES_KEY = "identity2.progression.morph_charges";
    private static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private MorphChargeManager() {
    }

    public static int getCharges(ServerPlayer player, ResourceLocation identityId) {
        if (player == null || identityId == null) {
            return 0;
        }
        Map<String, Integer> charges = getChargeMap(player);
        return Math.max(0, charges.getOrDefault(identityId.toString(), 0));
    }

    public static Map<String, Integer> getChargeSnapshot(ServerPlayer player) {
        if (player == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new HashMap<>(getChargeMap(player)));
    }

    public static void addCharges(ServerPlayer player, ResourceLocation identityId, int amount) {
        if (player == null || identityId == null || amount <= 0) {
            return;
        }
        Map<String, Integer> charges = getChargeMap(player);
        String key = identityId.toString();
        charges.put(key, Math.max(0, charges.getOrDefault(key, 0)) + amount);
        setChargeMap(player, charges);
    }

    public static boolean tryRemoveCharges(ServerPlayer player, ResourceLocation identityId, int amount) {
        if (player == null || identityId == null || amount <= 0) {
            return false;
        }
        Map<String, Integer> charges = getChargeMap(player);
        String key = identityId.toString();
        int available = Math.max(0, charges.getOrDefault(key, 0));
        if (available < amount) {
            return false;
        }
        int remaining = available - amount;
        if (remaining > 0) {
            charges.put(key, remaining);
        } else {
            charges.remove(key);
        }
        setChargeMap(player, charges);
        return true;
    }

    public static boolean tryConsumeMorphCharge(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt, boolean notifyPlayer) {
        if (player == null || identityId == null) {
            return false;
        }
        if (ProgressionConfig.shouldBypassMorphCharges(player)) {
            return true;
        }
        if (!ProgressionConfig.enableMorphCharges()) {
            return true;
        }
        if (!ProgressionConfig.requireChargeForMorphing()) {
            return true;
        }
        if (IdentityProgression.PLAYER_IDENTITY_ID.equals(identityId)) {
            return true;
        }
        if (PermanentMorphManager.isPermanentMorph(player, identityId, variantNbt)) {
            return true;
        }

        int cost = Math.max(1, ProgressionConfigHelper.resolveChargeCost(identityId));
        int available = getCharges(player, identityId);
        if (available < cost) {
            if (notifyPlayer) {
                player.displayClientMessage(Component.literal("Not enough charges for morph: " + identityId + " (" + available + "/" + cost + ")"), false);
            }
            return false;
        }

        Map<String, Integer> charges = getChargeMap(player);
        String key = identityId.toString();
        charges.put(key, Math.max(0, available - cost));
        setChargeMap(player, charges);
        return true;
    }

    public static void onIdentityKilled(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return;
        }
        if (!ProgressionConfig.enableMorphCharges()) {
            return;
        }
        int gain = Math.max(0, ProgressionConfigHelper.resolveChargeGain(identityId));
        if (gain <= 0) {
            return;
        }
        addCharges(player, identityId, gain);
    }

    public static void applyDeathPenalty(ServerPlayer player) {
        if (player == null || !ProgressionConfig.enableMorphCharges() || !IdentitySettings.removeMorphChargeOnDeath) {
            return;
        }
        if (ProgressionConfig.shouldBypassMorphCharges(player)) {
            return;
        }

        CompoundTag customData = getCustomData(player);
        String selectedType = NbtCompat.getStringOr(customData, IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (selectedType.isBlank()) {
            selectedType = NbtCompat.getStringOr(customData, "model_override", "");
        }
        if (selectedType.isBlank()) {
            return;
        }

        ResourceLocation identityId;
        try {
            identityId = ResourceLocation.parse(selectedType);
        } catch (Exception ignored) {
            return;
        }
        if (IdentityProgression.PLAYER_IDENTITY_ID.equals(identityId)) {
            return;
        }

        CompoundTag variantNbt = IdentityProgression.parseVariantNbt(NbtCompat.getStringOr(customData, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, ""));
        if (PermanentMorphManager.hasDeathLossProtection(player, identityId, variantNbt)) {
            return;
        }

        int penalty = Math.max(1, ProgressionConfigHelper.resolveChargeDeathPenalty(identityId));
        int available = getCharges(player, identityId);
        if (available <= 0) {
            return;
        }
        Map<String, Integer> charges = getChargeMap(player);
        String key = identityId.toString();
        charges.put(key, Math.max(0, available - penalty));
        setChargeMap(player, charges);
    }

    private static Map<String, Integer> getChargeMap(ServerPlayer player) {
        CompoundTag customData = getCustomData(player);
        return new HashMap<>(NbtCompat.read(customData, MORPH_CHARGES_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
    }

    private static void setChargeMap(ServerPlayer player, Map<String, Integer> charges) {
        CompoundTag customData = getCustomData(player);
        NbtCompat.store(customData, MORPH_CHARGES_KEY, STRING_INT_MAP_CODEC, charges == null ? Map.of() : charges);
        ProgressionUiSync.sendPlayerCharges(player, charges);
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        CompoundTag customData = ((EntityAccessor) player).getCustomData();
        return customData;
    }
}




