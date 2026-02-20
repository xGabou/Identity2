package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

public final class ModComponents {
    // Runtime-only fallbacks:
    // NeoForge 1.21.11 freezes built-in registries before mod static init.
    // Until platform-safe registration is wired, keep stable component keys locally
    // so gameplay does not crash when these fields are referenced.
    public static final DataComponentType<String> USE_COMMAND_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();
    public static final DataComponentType<String> ON_ITEM_DESTROYED_COMMAND_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();
    public static final DataComponentType<String> INVENTORY_TICK_COMMAND_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();
    public static final DataComponentType<String> ON_CRAFT_ANY_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();
    public static final DataComponentType<String> ON_CRAFT_CRAFTER_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();
    public static final DataComponentType<String> ON_CRAFT_PLAYER_COMPONENT =
        DataComponentType.<String>builder().persistent(Codec.STRING).build();

    public static final DataComponentType<Unit> SOULBOUND = EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP;

    private ModComponents() {
    }

    public static void initialize() {
    }
}
