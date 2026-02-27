package net.Gabou.identity2;

import net.minecraft.world.item.ItemStack;

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
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(SOULBOUND);
    }
}
