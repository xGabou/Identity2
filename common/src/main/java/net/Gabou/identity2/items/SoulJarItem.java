package net.Gabou.identity2.items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.Gabou.identity2.progression.SoulJarChargeStorage;
import net.Gabou.identity2.progression.SoulJarManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

public final class SoulJarItem extends Item {
    private static final String ITEM_SOUL_JAR_KEY = "identity2_soul_jar";
    private static final int MAX_CHARGE_LINES = 4;

    public SoulJarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        SoulJarChargeStorage.JarSnapshot snapshot = SoulJarChargeStorage.read(stack);
        String tier = snapshot == null ? inferTier(stack) : snapshot.tier();
        String jarId = snapshot == null ? "" : snapshot.jarId();
        int morphCount = readStoredMorphCount(stack);
        int capacity = SoulJarManager.getJarCapacity(tier);

        tooltipAdder.accept(Component.literal("Tier: " + formatTier(tier)).withStyle(ChatFormatting.AQUA));
        if (!jarId.isBlank()) {
            tooltipAdder.accept(Component.literal("Jar ID: " + jarId).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltipAdder.accept(Component.literal("Stored Morphs: " + morphCount + "/" + capacity).withStyle(ChatFormatting.GRAY));

        if (snapshot == null || snapshot.charges().isEmpty()) {
            tooltipAdder.accept(Component.literal("Stored Charges: 0").withStyle(ChatFormatting.GOLD));
            return;
        }

        int totalCharges = snapshot.charges().values().stream().mapToInt(Integer::intValue).sum();
        tooltipAdder.accept(Component.literal("Stored Charges: " + totalCharges).withStyle(ChatFormatting.GOLD));

        List<Map.Entry<String, Integer>> chargeLines = new ArrayList<>(snapshot.charges().entrySet());
        chargeLines.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed().thenComparing(Map.Entry::getKey));

        int shown = 0;
        for (Map.Entry<String, Integer> entry : chargeLines) {
            if (shown >= MAX_CHARGE_LINES) {
                break;
            }
            tooltipAdder.accept(
                Component.literal("- " + entry.getKey() + " x" + Math.max(0, entry.getValue())).withStyle(ChatFormatting.DARK_AQUA)
            );
            shown++;
        }
    }

    private static int readStoredMorphCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData == null || customData.isEmpty()) {
            return 0;
        }
        CompoundTag root = customData.copyTag();
        CompoundTag jarTag = root.getCompound(ITEM_SOUL_JAR_KEY).orElse(null);
        if (jarTag == null || jarTag.isEmpty()) {
            return 0;
        }
        return jarTag.read("morphs", SoulJarManager.StoredMorphData.CODEC.listOf()).orElse(List.of()).size();
    }

    private static String inferTier(ItemStack stack) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return "mud";
        }
        String path = itemId.getPath();
        if ("mud_jar".equals(path)) {
            return "mud";
        }
        if ("glass_jar".equals(path)) {
            return "glass";
        }
        if ("reinforced_jar".equals(path)) {
            return "reinforced";
        }
        if ("endgame_jar".equals(path)) {
            return "true_soul";
        }
        return "mud";
    }

    private static String formatTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return "Mud";
        }
        String normalized = tier.trim().toLowerCase();
        return switch (normalized) {
            case "true_soul" -> "True Soul";
            case "reinforced" -> "Reinforced";
            case "glass" -> "Glass";
            default -> "Mud";
        };
    }
}
