package net.Gabou.identity2;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    static {
        registerVanillaItem("end_gateway", "minecraft");
        registerVanillaItem("fire", "minecraft");
        registerVanillaItem("soul_fire", "minecraft");
        registerVanillaItem("nether_portal", "minecraft");
        registerVanillaItem("end_portal", "minecraft");
        registerVanillaItem("water_cauldron", "minecraft");
        registerVanillaItem("lava_cauldron", "minecraft");
        registerVanillaItem("powder_snow_cauldron", "minecraft");
        registerVanillaItem("carrots", "minecraft");
        registerVanillaItem("beetroots", "minecraft");
        registerVanillaItem("frosted_ice", "minecraft");
        registerVanillaItem("sweet_berry_bush", "minecraft");
        registerVanillaItem("water", "minecraft");
        registerVanillaItem("lava", "minecraft");
        registerVanillaItem("powder_snow", "minecraft");
    }

    private ModBlocks() {
    }

    public static void initialize() {
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
        ResourceKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(settings.setId(blockKey));

        if (shouldRegisterItem) {
            ResourceKey<Item> itemKey = keyOfItem(name);
            BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static Item registerVanillaItem(String name, String namespace) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, name));
        BlockItem blockItem = new BlockItem(
            BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(namespace, name)),
            new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()
        );
        return Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
    }

    public static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Identity2.MOD_ID, name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Identity2.MOD_ID, name));
    }
}
