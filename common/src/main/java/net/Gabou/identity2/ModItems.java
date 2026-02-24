package net.Gabou.identity2;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.Function;
import net.Gabou.identity2.items.IdentityBookItem;
import net.Gabou.identity2.items.SoulJarItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Identity2.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> MUD_JAR = register("mud_jar", SoulJarItem::new);
    public static final RegistrySupplier<Item> GLASS_JAR = register("glass_jar", SoulJarItem::new);
    public static final RegistrySupplier<Item> REINFORCED_JAR = register("reinforced_jar", SoulJarItem::new);
    public static final RegistrySupplier<Item> ENDGAME_JAR = register("endgame_jar", SoulJarItem::new);
    public static final RegistrySupplier<Item> SOUL_CATALYST = register("soul_catalyst");
    public static final RegistrySupplier<Item> IDENTITY_BOOK = register("identity_book", IdentityBookItem::new);

    private ModItems() {
    }

    public static void initialize() {
        ITEMS.register();
    }

    private static RegistrySupplier<Item> register(String name) {
        return register(name, Item::new);
    }

    private static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Identity2.MOD_ID, name));
        return ITEMS.register(name, () -> itemFactory.apply(new Item.Properties().setId(key)));
    }
}
