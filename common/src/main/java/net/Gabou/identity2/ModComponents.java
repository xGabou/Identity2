package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import java.util.function.UnaryOperator;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

public final class ModComponents {
    public static final ComponentType<String> USE_COMMAND_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "use_command"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> ON_ITEM_DESTROYED_COMMAND_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "item_entity_destroyed_command"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> INVENTORY_TICK_COMMAND_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "inv_tick_command"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> ON_CRAFT_ANY_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "on_craft"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> ON_CRAFT_CRAFTER_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "on_craft_crafter"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> ON_CRAFT_PLAYER_COMPONENT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(Identity2.MOD_ID, "on_craft_player"),
        ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<Unit> SOULBOUND = registerEnchantmentComponent(
        "keep_on_death",
        builder -> builder.codec(Unit.CODEC)
    );

    private ModComponents() {
    }

    public static void initialize() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ComponentType<T> registerEnchantmentComponent(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(
            Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
            Identifier.ofVanilla(id),
            ((ComponentType.Builder) builderOperator.apply(ComponentType.builder())).build()
        );
    }
}
