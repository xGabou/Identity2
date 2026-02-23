package net.Gabou.identity2;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Identity2.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> MUD_JAR = register("mud_jar");
    public static final RegistrySupplier<Item> GLASS_JAR = register("glass_jar");
    public static final RegistrySupplier<Item> REINFORCED_JAR = register("reinforced_jar");
    public static final RegistrySupplier<Item> ENDGAME_JAR = register("endgame_jar");
    public static final RegistrySupplier<Item> SOUL_CATALYST = register("soul_catalyst");

    private ModItems() {
    }

    public static void initialize() {
        ITEMS.register();
    }

    private static RegistrySupplier<Item> register(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Identity2.MOD_ID, name));
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
    }
}
