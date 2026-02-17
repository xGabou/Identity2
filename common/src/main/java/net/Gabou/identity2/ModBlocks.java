package net.Gabou.identity2;

import java.util.function.Function;
import net.Gabou.identity2.blocks.MagicBarrierBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block MAGIC_BARRIER_BLOCK = register(
        "barier",
        MagicBarrierBlock::new,
        AbstractBlock.Settings.create()
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .allowsSpawning((state, world, pos, type) -> false)
            .solidBlock((state, world, pos) -> false)
            .suffocates((state, world, pos) -> false)
            .blockVision((state, world, pos) -> false)
            .slipperiness(1.0F),
        true
    );

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

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        RegistryKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        if (shouldRegisterItem) {
            RegistryKey<Item> itemKey = keyOfItem(name);
            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static Item registerVanillaItem(String name, String namespace) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(namespace, name));
        BlockItem blockItem = new BlockItem(
            Registries.BLOCK.get(Identifier.of(namespace, name)),
            new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()
        );
        return Registry.register(Registries.ITEM, itemKey, blockItem);
    }

    public static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Identity2.MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Identity2.MOD_ID, name));
    }
}
