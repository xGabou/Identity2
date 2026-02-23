package net.Gabou.identity2.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class SoulJarManager {
    private static final String SOUL_JARS_KEY = "identity2.progression.soul_jars";
    static final String SOUL_JAR_KILL_PROGRESS_KEY = "identity2.progression.soul_jar_kill_progress";
    private static final String ITEM_SOUL_JAR_KEY = "identity2_soul_jar";
    private static final Codec<List<SoulJarData>> SOUL_JAR_LIST_CODEC = SoulJarData.CODEC.listOf();
    static final Codec<Map<String, Integer>> STRING_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private SoulJarManager() {
    }

    public static List<SoulJarData> getSoulJars(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        List<SoulJarData> jars = new ArrayList<>(getInventorySoulJars(player));
        jars.addAll(getLegacySoulJars(player));
        return jars;
    }

    public static int getJarCapacity(String tier) {
        return ProgressionConfigHelper.resolveJarCapacity(tier);
    }

    public static String getTrueSoulTier() {
        return ProgressionConfigHelper.normalizeTier(IdentitySettings.trueSoulJarTier);
    }

    public static boolean createJar(ServerPlayer player, String jarId, String tier) {
        if (player == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }

        String normalizedId = normalizeJarId(jarId);
        if (normalizedId.isBlank()) {
            return false;
        }

        if (findInventoryJar(player, normalizedId) != null || findLegacyJar(getLegacySoulJars(player), normalizedId) != null) {
            return false;
        }

        String normalizedTier = ProgressionConfigHelper.normalizeTier(tier);
        ItemStack stack = new ItemStack(resolveJarItem(normalizedTier));
        writeJarToStack(stack, new SoulJarData(normalizedId, normalizedTier, List.of()));

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
        return true;
    }

    public static boolean upgradeJar(ServerPlayer player, String jarId, String newTier) {
        if (player == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }

        String normalizedId = normalizeJarId(jarId);
        String normalizedTier = ProgressionConfigHelper.normalizeTier(newTier);

        InventoryJarRef inventoryJar = findInventoryJar(player, normalizedId);
        if (inventoryJar != null) {
            SoulJarData jarData = inventoryJar.data();
            SoulJarData updatedData = new SoulJarData(jarData.jarId(), normalizedTier, jarData.morphs());
            writeJarAtSlot(player, inventoryJar.slot(), updatedData);
            return true;
        }

        List<SoulJarData> legacy = getLegacySoulJars(player);
        boolean changed = false;
        List<SoulJarData> updated = new ArrayList<>(legacy.size());
        for (SoulJarData jar : legacy) {
            if (jar.jarId().equalsIgnoreCase(normalizedId)) {
                updated.add(new SoulJarData(jar.jarId(), normalizedTier, jar.morphs()));
                changed = true;
            } else {
                updated.add(jar);
            }
        }
        if (changed) {
            setLegacySoulJars(player, updated);
        }
        return changed;
    }

    public static boolean storeMorphInJar(ServerPlayer player, String jarId, Identifier identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }

        String normalizedId = normalizeJarId(jarId);
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);

        InventoryJarRef inventoryJar = findInventoryJar(player, normalizedId);
        if (inventoryJar != null) {
            SoulJarData updated = withStoredMorph(inventoryJar.data(), identityId, variantToken);
            if (updated == null) {
                return false;
            }
            writeJarAtSlot(player, inventoryJar.slot(), updated);
            return true;
        }

        List<SoulJarData> legacy = getLegacySoulJars(player);
        boolean changed = false;
        List<SoulJarData> updatedLegacy = new ArrayList<>(legacy.size());
        for (SoulJarData jar : legacy) {
            if (!jar.jarId().equalsIgnoreCase(normalizedId)) {
                updatedLegacy.add(jar);
                continue;
            }
            SoulJarData updated = withStoredMorph(jar, identityId, variantToken);
            if (updated == null) {
                updatedLegacy.add(jar);
                return false;
            }
            updatedLegacy.add(updated);
            changed = true;
        }

        if (changed) {
            setLegacySoulJars(player, updatedLegacy);
        }
        return changed;
    }

    public static boolean removeMorphFromJar(ServerPlayer player, String jarId, Identifier identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }

        String normalizedId = normalizeJarId(jarId);
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);

        InventoryJarRef inventoryJar = findInventoryJar(player, normalizedId);
        if (inventoryJar != null) {
            SoulJarData updated = withRemovedMorph(inventoryJar.data(), identityId, variantToken);
            if (updated == null) {
                return false;
            }
            writeJarAtSlot(player, inventoryJar.slot(), updated);
            return true;
        }

        List<SoulJarData> legacy = getLegacySoulJars(player);
        boolean changed = false;
        List<SoulJarData> updatedLegacy = new ArrayList<>(legacy.size());
        for (SoulJarData jar : legacy) {
            if (!jar.jarId().equalsIgnoreCase(normalizedId)) {
                updatedLegacy.add(jar);
                continue;
            }

            SoulJarData updated = withRemovedMorph(jar, identityId, variantToken);
            if (updated == null) {
                updatedLegacy.add(jar);
                return false;
            }

            updatedLegacy.add(updated);
            changed = true;
        }

        if (changed) {
            setLegacySoulJars(player, updatedLegacy);
        }
        return changed;
    }

    public static boolean isStoredInAnyJar(ServerPlayer player, Identifier identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);
        for (SoulJarData jar : getSoulJars(player)) {
            for (StoredMorphData stored : jar.morphs()) {
                if (matchesStoredMorph(stored, identityId, variantToken)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isStoredInTrueSoulJar(ServerPlayer player, Identifier identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);
        String trueTier = ProgressionConfigHelper.normalizeTier(IdentitySettings.trueSoulJarTier);
        for (SoulJarData jar : getSoulJars(player)) {
            if (!ProgressionConfigHelper.normalizeTier(jar.tier()).equals(trueTier)) {
                continue;
            }
            for (StoredMorphData stored : jar.morphs()) {
                if (matchesStoredMorph(stored, identityId, variantToken)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPermanentInJar(ServerPlayer player, Identifier identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulJars()) {
            return false;
        }
        String variantToken = IdentityProgression.toVariantUnlockToken(variantNbt);
        String trueTier = ProgressionConfigHelper.normalizeTier(IdentitySettings.trueSoulJarTier);
        for (SoulJarData jar : getSoulJars(player)) {
            boolean trueSoulPermanent = IdentitySettings.trueSoulJarGrantsPermanent
                && ProgressionConfigHelper.normalizeTier(jar.tier()).equals(trueTier);
            for (StoredMorphData stored : jar.morphs()) {
                if (!matchesStoredMorph(stored, identityId, variantToken)) {
                    continue;
                }
                if (stored.permanent() || trueSoulPermanent) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getKillProgress(ServerPlayer player, String jarId, Identifier identityId, CompoundTag variantNbt) {
        return JarProgressionManager.getKillProgress(player, jarId, identityId, variantNbt);
    }

    public static void onIdentityKilled(ServerPlayer player, Identifier identityId, CompoundTag variantNbt) {
        JarProgressionManager.onIdentityKilled(player, identityId, variantNbt);
    }

    static Map<String, Integer> getKillProgressMap(ServerPlayer player) {
        CompoundTag customData = getCustomData(player);
        return new HashMap<>(customData.read(SOUL_JAR_KILL_PROGRESS_KEY, STRING_INT_MAP_CODEC).orElse(Map.of()));
    }

    static void setKillProgressMap(ServerPlayer player, Map<String, Integer> progress) {
        CompoundTag customData = getCustomData(player);
        customData.store(SOUL_JAR_KILL_PROGRESS_KEY, STRING_INT_MAP_CODEC, progress == null ? Map.of() : progress);
    }

    static boolean applyKillProgressToStoredMorphs(ServerPlayer player, Identifier identityId, String variantToken, int required, Map<String, Integer> progress) {
        if (player == null || identityId == null || variantToken == null || progress == null) {
            return false;
        }

        boolean changed = false;

        List<InventoryJarRef> inventoryJars = getInventoryJarRefs(player);
        for (InventoryJarRef ref : inventoryJars) {
            SoulJarData updated = applyKillProgressToJar(ref.data(), identityId, variantToken, required, progress);
            if (updated == null) {
                continue;
            }
            writeJarAtSlot(player, ref.slot(), updated);
            changed = true;
        }

        List<SoulJarData> legacy = getLegacySoulJars(player);
        List<SoulJarData> updatedLegacy = new ArrayList<>(legacy.size());
        boolean legacyChanged = false;
        for (SoulJarData jar : legacy) {
            SoulJarData updated = applyKillProgressToJar(jar, identityId, variantToken, required, progress);
            if (updated == null) {
                updatedLegacy.add(jar);
                continue;
            }
            updatedLegacy.add(updated);
            legacyChanged = true;
        }

        if (legacyChanged) {
            setLegacySoulJars(player, updatedLegacy);
            changed = true;
        }

        return changed;
    }

    static String progressKey(String jarId, Identifier identityId, String variantToken) {
        return normalizeJarId(jarId) + "|" + identityId + "|" + (variantToken == null || variantToken.isBlank() ? "-" : variantToken);
    }

    static String normalizeJarId(String jarId) {
        if (jarId == null) {
            return "";
        }
        return jarId.trim().toLowerCase();
    }

    private static SoulJarData applyKillProgressToJar(SoulJarData jar, Identifier identityId, String variantToken, int required, Map<String, Integer> progress) {
        String trueTier = ProgressionConfigHelper.normalizeTier(IdentitySettings.trueSoulJarTier);
        boolean trueSoulPermanent = IdentitySettings.trueSoulJarGrantsPermanent
            && ProgressionConfigHelper.normalizeTier(jar.tier()).equals(trueTier);

        boolean jarChanged = false;
        List<StoredMorphData> updatedMorphs = new ArrayList<>(jar.morphs().size());
        for (StoredMorphData stored : jar.morphs()) {
            if (!matchesStoredMorph(stored, identityId, variantToken) || stored.permanent() || trueSoulPermanent) {
                updatedMorphs.add(stored);
                continue;
            }

            String key = progressKey(jar.jarId(), identityId, stored.variantToken());
            int next = progress.getOrDefault(key, 0) + 1;
            progress.put(key, next);
            if (next >= required) {
                updatedMorphs.add(new StoredMorphData(stored.identityId(), stored.variantToken(), true));
                jarChanged = true;
            } else {
                updatedMorphs.add(stored);
            }
        }

        if (!jarChanged) {
            return null;
        }

        return new SoulJarData(jar.jarId(), jar.tier(), updatedMorphs);
    }

    private static SoulJarData withStoredMorph(SoulJarData jar, Identifier identityId, String variantToken) {
        List<StoredMorphData> morphs = new ArrayList<>(jar.morphs());
        for (StoredMorphData stored : morphs) {
            if (stored.identityId().equals(identityId.toString()) && stored.variantToken().equals(variantToken)) {
                return null;
            }
        }

        int capacity = ProgressionConfigHelper.resolveJarCapacity(jar.tier());
        if (morphs.size() >= capacity) {
            return null;
        }

        morphs.add(new StoredMorphData(identityId.toString(), variantToken, false));
        return new SoulJarData(jar.jarId(), jar.tier(), morphs);
    }

    private static SoulJarData withRemovedMorph(SoulJarData jar, Identifier identityId, String variantToken) {
        List<StoredMorphData> remaining = new ArrayList<>();
        boolean removed = false;
        for (StoredMorphData stored : jar.morphs()) {
            if (!matchesStoredMorph(stored, identityId, variantToken)) {
                remaining.add(stored);
                continue;
            }
            removed = true;
        }

        if (!removed) {
            return null;
        }

        return new SoulJarData(jar.jarId(), jar.tier(), remaining);
    }

    private static boolean matchesStoredMorph(StoredMorphData stored, Identifier identityId, String variantToken) {
        if (stored == null || identityId == null) {
            return false;
        }
        if (!stored.identityId().equals(identityId.toString())) {
            return false;
        }
        return "-".equals(stored.variantToken()) || stored.variantToken().equals(variantToken);
    }

    private static List<SoulJarData> getLegacySoulJars(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag customData = getCustomData(player);
        return new ArrayList<>(customData.read(SOUL_JARS_KEY, SOUL_JAR_LIST_CODEC).orElse(List.of()));
    }

    private static void setLegacySoulJars(ServerPlayer player, List<SoulJarData> jars) {
        CompoundTag customData = getCustomData(player);
        customData.store(SOUL_JARS_KEY, SOUL_JAR_LIST_CODEC, jars == null ? List.of() : jars);
    }

    private static InventoryJarRef findInventoryJar(ServerPlayer player, String normalizedId) {
        for (InventoryJarRef ref : getInventoryJarRefs(player)) {
            if (ref.data().jarId().equalsIgnoreCase(normalizedId)) {
                return ref;
            }
        }
        return null;
    }

    private static SoulJarData findLegacyJar(List<SoulJarData> jars, String normalizedId) {
        for (SoulJarData jar : jars) {
            if (jar.jarId().equalsIgnoreCase(normalizedId)) {
                return jar;
            }
        }
        return null;
    }

    private static List<InventoryJarRef> getInventoryJarRefs(ServerPlayer player) {
        List<InventoryJarRef> refs = new ArrayList<>();
        if (player == null) {
            return refs;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            SoulJarData jarData = readJarFromStack(stack);
            if (jarData == null) {
                continue;
            }
            refs.add(new InventoryJarRef(slot, stack, jarData));
        }
        return refs;
    }

    private static List<SoulJarData> getInventorySoulJars(ServerPlayer player) {
        List<SoulJarData> jars = new ArrayList<>();
        for (InventoryJarRef ref : getInventoryJarRefs(player)) {
            jars.add(ref.data());
        }
        return jars;
    }

    private static SoulJarData readJarFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData == null || customData.isEmpty()) {
            return null;
        }

        CompoundTag root = customData.copyTag();
        CompoundTag jarTag = root.getCompound(ITEM_SOUL_JAR_KEY).orElse(null);
        if (jarTag == null || jarTag.isEmpty()) {
            return null;
        }

        String jarId = normalizeJarId(jarTag.getStringOr("jar_id", ""));
        if (jarId.isBlank()) {
            return null;
        }

        String tier = ProgressionConfigHelper.normalizeTier(jarTag.getStringOr("tier", "mud"));
        List<StoredMorphData> morphs = new ArrayList<>(jarTag.read("morphs", StoredMorphData.CODEC.listOf()).orElse(List.of()));
        return new SoulJarData(jarId, tier, morphs);
    }

    private static void writeJarAtSlot(ServerPlayer player, int slot, SoulJarData jarData) {
        Inventory inventory = player.getInventory();
        ItemStack current = inventory.getItem(slot);
        if (current.isEmpty()) {
            return;
        }

        Item targetItem = resolveJarItem(jarData.tier());
        ItemStack targetStack = current;
        if (!current.is(targetItem)) {
            targetStack = current.transmuteCopy(targetItem, Math.max(1, current.getCount()));
            inventory.setItem(slot, targetStack);
        }

        writeJarToStack(targetStack, jarData);
        inventory.setChanged();
    }

    private static void writeJarToStack(ItemStack stack, SoulJarData jarData) {
        if (stack == null || stack.isEmpty() || jarData == null) {
            return;
        }

        CustomData currentData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = currentData == null ? new CompoundTag() : currentData.copyTag();

        CompoundTag jarTag = new CompoundTag();
        jarTag.putString("jar_id", normalizeJarId(jarData.jarId()));
        jarTag.putString("tier", ProgressionConfigHelper.normalizeTier(jarData.tier()));
        jarTag.store("morphs", StoredMorphData.CODEC.listOf(), jarData.morphs() == null ? List.of() : jarData.morphs());

        root.put(ITEM_SOUL_JAR_KEY, jarTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static Item resolveJarItem(String tier) {
        Identifier itemId = ProgressionConfig.resolveJarItemId(tier);
        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }
        return Items.CLAY_BALL;
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
    }

    private record InventoryJarRef(int slot, ItemStack stack, SoulJarData data) {
    }

    public record StoredMorphData(String identityId, String variantToken, boolean permanent) {
        public static final Codec<StoredMorphData> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                Codec.STRING.fieldOf("identity").forGetter(StoredMorphData::identityId),
                Codec.STRING.optionalFieldOf("variant", "-").forGetter(StoredMorphData::variantToken),
                Codec.BOOL.optionalFieldOf("permanent", false).forGetter(StoredMorphData::permanent)
            ).apply(builder, StoredMorphData::new)
        );
    }

    public record SoulJarData(String jarId, String tier, List<StoredMorphData> morphs) {
        public static final Codec<SoulJarData> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                Codec.STRING.fieldOf("jar_id").forGetter(SoulJarData::jarId),
                Codec.STRING.optionalFieldOf("tier", "mud").forGetter(SoulJarData::tier),
                StoredMorphData.CODEC.listOf().optionalFieldOf("morphs", List.of()).forGetter(SoulJarData::morphs)
            ).apply(builder, SoulJarData::new)
        );
    }
}
