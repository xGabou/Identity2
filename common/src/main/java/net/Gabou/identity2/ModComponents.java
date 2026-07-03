package net.Gabou.identity2;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ModComponents {
    public static final String USE_COMMAND_COMPONENT = "identity2_use_command";
    public static final String ON_ITEM_DESTROYED_COMMAND_COMPONENT = "identity2_on_item_destroyed_command";
    public static final String INVENTORY_TICK_COMMAND_COMPONENT = "identity2_inventory_tick_command";
    public static final String ON_CRAFT_ANY_COMPONENT = "identity2_on_craft_any_command";
    public static final String ON_CRAFT_CRAFTER_COMPONENT = "identity2_on_craft_crafter_command";
    public static final String ON_CRAFT_PLAYER_COMPONENT = "identity2_on_craft_player_command";
    public static final String SOULBOUND = "identity2_soulbound";

    private ModComponents() {
    }

    public static void initialize() {
    }

    public static boolean hasSoulbound(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData == null || customData.isEmpty()) {
            return false;
        }
        CompoundTag tag = customData.copyTag();
        return tag.getBoolean(SOULBOUND);
    }
}
