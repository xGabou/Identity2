package ember.qualitycommands;

import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;
import java.util.Set;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.piston.PistonBehavior;
import ember.qualitycommands.blocks.MagicBarrierBlock;
//import ember.qualitycommands.blocks.FastRepeaterBlock;
//import ember.qualitycommands.blocks.BouncySlimeBlock;
//import ember.qualitycommands.blocks.BouncySlimeLayerBlock;
//import com.example.blocks.LevitatorBlock;
//import com.example.items.CustomItem;
//import com.example.blocks.BlockEntityTypes;
//import ember.qualitycommands.blocks.ComparandorBlock;
//import ember.qualitycommands.blocks.ComparandorBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import ember.qualitycommands.QualityCommands;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import java.util.function.Function;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.block.entity.BlockEntity;
/*import ember.qualitycommands.blocks.RedRedstoneWireBlock;
import ember.qualitycommands.blocks.GreenRedstoneWireBlock;
import ember.qualitycommands.blocks.BlueRedstoneWireBlock;*/
public class ModBlocks {
	private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
		// Create a registry key for the block
		RegistryKey<Block> blockKey = keyOfBlock(name);
		// Create the block instance
		Block block = blockFactory.apply(settings.registryKey(blockKey));

		// Sometimes, you may not want to register an item for the block.
		// Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
		if (shouldRegisterItem) {
			// Items need to be registered with a different type of registry key, but the ID
			// can be the same.
			RegistryKey<Item> itemKey = keyOfItem(name);

			BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
			Registry.register(Registries.ITEM, itemKey, blockItem);
		}

		return Registry.register(Registries.BLOCK, blockKey, block);
	}
	public static Block register(Identifier name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
		// Create a registry key for the block
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, name);
		// Create the block instance
		Block block = blockFactory.apply(settings.registryKey(blockKey));

		// Sometimes, you may not want to register an item for the block.
		// Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
		/*if (shouldRegisterItem) {
			// Items need to be registered with a different type of registry key, but the ID
			// can be the same.
			RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, name);

			BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
			Registry.register(Registries.ITEM, itemKey, blockItem);
		}*/

		//return Registry.register(Registries.BLOCK, blockKey, block);
		((net.minecraft.registry.SimpleDefaultedRegistry)Registries.BLOCK).add(blockKey,block,net.minecraft.registry.entry.RegistryEntryInfo.DEFAULT);
		return block;
	}
	private static Item registerItem(String name,String id,Block block) {
		// Create a registry key for the block
		// Items need to be registered with a different type of registry key, but the ID
		// can be the same.
		RegistryKey<Item> itemKey = keyOfAnyItem(name,id);
		BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
		return Registry.register(Registries.ITEM, itemKey, blockItem);
	}
	private static Item registerVanillaItem(String name,String id) {
		// Create a registry key for the block
		// Items need to be registered with a different type of registry key, but the ID
		// can be the same.
		RegistryKey<Item> itemKey = keyOfAnyItem(name,id);
		BlockItem blockItem = new BlockItem(net.minecraft.registry.Registries.BLOCK.get(Identifier.of(id,name)), new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
		return Registry.register(Registries.ITEM, itemKey, blockItem);
	}
	static{
		registerVanillaItem("end_gateway","minecraft");
		registerVanillaItem("fire","minecraft");
		registerVanillaItem("soul_fire","minecraft");
		registerVanillaItem("nether_portal","minecraft");
		registerVanillaItem("end_portal","minecraft");
		registerVanillaItem("water_cauldron","minecraft");
		registerVanillaItem("lava_cauldron","minecraft");
		registerVanillaItem("powder_snow_cauldron","minecraft");
		registerVanillaItem("carrots","minecraft");
		registerVanillaItem("beetroots","minecraft");
		registerVanillaItem("frosted_ice","minecraft");
		registerVanillaItem("sweet_berry_bush","minecraft");
		registerVanillaItem("water","minecraft");
		registerVanillaItem("lava","minecraft");
		registerVanillaItem("powder_snow","minecraft");
		//registerVanillaItem("","minecraft");
		
		
	}
	public static RegistryKey<Block> keyOfBlock(String name) {
		return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(QualityCommands.MOD_ID, name));
	}

	private static RegistryKey<Item> keyOfItem(String name) {
		return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(QualityCommands.MOD_ID, name));
	}
	private static RegistryKey<Item> keyOfAnyItem(String name,String id) {
		return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(id, name));
	}

	public static final Block MAGIC_BARRIER_BLOCK = register(
		"barier",
		MagicBarrierBlock::new,
		AbstractBlock.Settings.create().sounds(BlockSoundGroup.GRASS).nonOpaque()
				.allowsSpawning(Blocks::never)
				.solidBlock(Blocks::never)
				.suffocates(Blocks::never)
				.blockVision(Blocks::never).slipperiness(1.0F),
		true
	);
	
	/*public static final Block COMPARANDOR = register(
		"comparandor", ComparandorBlock::new, AbstractBlock.Settings.create().breakInstantly().sounds(BlockSoundGroup.STONE).pistonBehavior(PistonBehavior.DESTROY),true
	);
	public static <T extends BlockEntityType<?>> T build(String path, T blockEntityType) {
    return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("quality_commands", path), blockEntityType);
  }
 
  public static final BlockEntityType<ComparandorBlockEntity> COMPARANDORENTITY = build(
      "comparandor",
      // For versions before 1.21.2, please use BlockEntityType.Builder.
      FabricBlockEntityTypeBuilder.create(ComparandorBlockEntity::new, COMPARANDOR).build()
  );
	public static final Block FAST_REPEATER = register(
		"fastrepeater", FastRepeaterBlock::new, AbstractBlock.Settings.create().breakInstantly().sounds(BlockSoundGroup.STONE).pistonBehavior(PistonBehavior.DESTROY),true
	);
	public static final List<Block> REDSTONE_WIRE_COLORED = List.of(register(
		"red_redstone_wire", RedRedstoneWireBlock::new, AbstractBlock.Settings.create().noCollision().breakInstantly().pistonBehavior(PistonBehavior.DESTROY),true
	),register(
		"green_redstone_wire", GreenRedstoneWireBlock::new, AbstractBlock.Settings.create().noCollision().breakInstantly().pistonBehavior(PistonBehavior.DESTROY),true
	),register(
		"blue_redstone_wire", BlueRedstoneWireBlock::new, AbstractBlock.Settings.create().noCollision().breakInstantly().pistonBehavior(PistonBehavior.DESTROY),true
	));*/
	/*public static final Block BOUNCY_SLIME_BLOCK = register(
		new BouncySlimeBlock(AbstractBlock.Settings.create().slipperiness(1.1F).sounds(BlockSoundGroup.SLIME).nonOpaque()),
		"slime_block",
		true
	);
	public static final Block BOUNCY_SLIME_LAYER_BLOCK = register(
		new BouncySlimeLayerBlock(
			AbstractBlock.Settings.create()
				//.mapColor(MapColor.DARK_GREEN)
				.replaceable()
				.noCollision()
				.sounds(BlockSoundGroup.VINE)
				//.pistonBehavior(PistonBehavior.DESTROY)
		),"slime_layer",true
	);*/
	/*public static final Block LEVITATOR_BLOCK = register(
		new LevitatorBlock(Settings.create().sounds(BlockSoundGroup.GRASS).luminance(LevitatorBlock::getLuminance),
		() -> BlockEntityTypes.LEVITATOR_BLOCK
		),
		"levitator",
		true
	);*/
	public static void initialize(){
		//BlockEntityTypes.initialize();
}
}