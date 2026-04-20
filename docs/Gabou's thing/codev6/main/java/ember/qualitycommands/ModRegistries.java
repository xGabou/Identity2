package ember.qualitycommands;

import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Dynamic;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.block.spawner.TrialSpawnerConfig;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.CatVariant;
import net.minecraft.entity.passive.ChickenVariant;
import net.minecraft.entity.passive.CowVariant;
import net.minecraft.entity.passive.FrogVariant;
import net.minecraft.entity.passive.PigVariant;
import net.minecraft.entity.passive.WolfSoundVariant;
import net.minecraft.entity.passive.WolfVariant;
import net.minecraft.item.Instrument;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.message.MessageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.test.TestEnvironmentDefinition;
import net.minecraft.test.TestInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashCallable;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import ember.qualitycommands.util.IdentityAbilityDefinition;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.World;

public class ModRegistries{



	private static <T> RegistryKey<Registry<T>> of(String id) {
		return RegistryKey.ofRegistry(Identifier.ofVanilla(id));
	}

	public static String getPath(RegistryKey<? extends Registry<?>> registryRef) {
		return registryRef.getValue().getPath();
	}

	public static String getTagPath(RegistryKey<? extends Registry<?>> registryRef) {
		return "tags/" + registryRef.getValue().getPath();
	}
/*@Inject(method = "register", at=@At("TAIL"))
    private static <T> void register(Registry<? super T> registry, String id, T entry,CallbackInfoReturnable info){
        QualityCommands.LOGGER.info(registry.getKey().getRegistry().toString()+","+registry.getKey().getValue().toString()+","+id.toString());
    }
    @Inject(method = "register(Lnet/minecraft/registry/Registry;Lnet/minecraft/util/Identifier;Ljava/lang/Object;)Ljava/lang/Object;", at=@At("TAIL"))
    private static <V, T extends V> void register(Registry<V> registry, net.minecraft.util.Identifier id, T entry,CallbackInfoReturnable info){
        QualityCommands.LOGGER.info(registry.getKey().getRegistry().toString()+","+registry.getKey().getValue().toString()+","+id.toString());
    }*/
    public static final RegistryKey<Registry<IdentityAbilityDefinition>> IDENTITY_ABILITY_KEY = RegistryKey.ofRegistry(Identifier.of("quality_commands","identity_ability"));
    public static Registry<IdentityAbilityDefinition> identityAbilityRegistry = null;// net.minecraft.registry.Registries.create(IDENTITY_ABILITY_KEY, registry -> null);
    //static final Registry<BlockSettingsRecord> DATA_BLOCK_REGISTRY = net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder.createSimple(DATA_BLOCK_KEY)
 	//												.buildAndRegister();

     public static final Codec<IdentityAbilityDefinition> IdentityAbilityCodec = RecordCodecBuilder.create(inst -> inst.group(
         Item.ENTRY_CODEC.fieldOf("icon").forGetter(IdentityAbilityDefinition::icon),
         Codec.STRING.optionalFieldOf("command","").forGetter(IdentityAbilityDefinition::command),
         Codec.INT.fieldOf("cooldown").forGetter(IdentityAbilityDefinition::cooldown),
         Codec.INT.optionalFieldOf("use_duration",0).forGetter(IdentityAbilityDefinition::useduration),
         Identifier.CODEC.optionalFieldOf("predef",Identifier.of("null")).forGetter(IdentityAbilityDefinition::bultinability),
         Codec.BOOL.optionalFieldOf("override_attack",false).forGetter(IdentityAbilityDefinition::override_attack)
     ).apply(inst, IdentityAbilityDefinition::new));
    
    /*private static Codec<Block> BlockCodec = BlockSettingsRecordCodec.xmap(
        // Convert Vec3d to BlockPos
        record -> convertBlockSettingsRecordToBlock(record),
        // Convert BlockPos to Vec3d
        block -> convertBlockToBlockSettingsRecord(block)
    );*/
    static{
        net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback.EVENT.register(registryView -> {
            for(Registry entry:registryView.stream().toList()){
                if(entry.getKey().getValue().getPath()=="identity_ability"){
                    identityAbilityRegistry=entry;
                }
                QualityCommands.LOGGER.info("???l registry at: "+entry.getKey().getRegistry()+"/"+entry.getKey().getValue());
            }
            registryView.registerEntryAdded(IDENTITY_ABILITY_KEY, (rawId, id, object) -> {
                QualityCommands.LOGGER.info("New Ability Registered for "+id.toString());
                
            });
        });
    }
    public static net.minecraft.server.MinecraftServer currentServer=null;
    public static boolean blocksLoaded=false;
    public static void init(){
        //world.getRegistryManager().get(DATA_BLOCK_KEY)
        net.fabricmc.fabric.api.event.registry.DynamicRegistries.registerSynced(IDENTITY_ABILITY_KEY, IdentityAbilityCodec,IdentityAbilityCodec);
        //identityAbilityRegistry=net.minecraft.registry.Registries.create(IDENTITY_ABILITY_KEY, registry -> null);
        //identityAbilityRegistry=(Registry)net.minecraft.registry.BuiltinRegistries.createWrapperLookup().getOrThrow(IDENTITY_ABILITY_KEY);
        //net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl.addSyncedRegistry(identityAbilityRegistry.getKey(), IdentityAbilityCodec);
        for(RegistryLoader.Entry entry:DynamicRegistries.getDynamicRegistries()){
            QualityCommands.LOGGER.info("Dynamic registry at: "+entry.key().getRegistry()+"/"+entry.key().getValue());
        }
        /*if(net.minecraft.registry.Registries.ROOT.getEntry(Identifier.of("quality_commands:identity_ability")).isPresent()){
            //net.minecraft.registry.Registries.create(DATA_BLOCK_KEY, registry -> new BlockSettingsRecord(false,"example"));
            QualityCommands.LOGGER.info("Identity ability registry inaccessible");
        }else{
            QualityCommands.LOGGER.info("Identity ability registry accessible");
            identityAbilityRegistry=(Registry)net.minecraft.registry.Registries.ROOT.getEntry(Identifier.of("quality_commands:identity_ability")).orElse(null);
        }*/
    }
    
}