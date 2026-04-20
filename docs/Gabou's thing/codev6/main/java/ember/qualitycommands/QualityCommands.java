package ember.qualitycommands;

import net.fabricmc.api.ModInitializer;
import ember.qualitycommands.ModCommands;
import ember.qualitycommands.ModBlocks;
import ember.qualitycommands.ModEffects;
import ember.qualitycommands.ModComponents;
import ember.qualitycommands.ModRegistries;
import ember.qualitycommands.util.Calculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ember.qualitycommands.CommandTriggerPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import ember.qualitycommands.ModPackets;
public class QualityCommands implements ModInitializer {
	public static final String MOD_ID = "quality_commands";
	public static int indexOverrideActive=0;//If set to true, ALWAYS unset later!
	

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static int maxWorldSize=30000000;
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModCommands.initialize();
		ModPackets.initialize();
		ModBlocks.initialize();
		ModEffects.initialize();
		ModComponents.initialize();
		ModRegistries.init();
		LOGGER.info("Hello Fabric world!");
		LOGGER.info(Calculator.calculate("5+6*(7-8/(9))").toString());
		LOGGER.info(Calculator.calculate("min(6,0-1)+4.3").toString());
		/*ServerPlayNetworking.registerGlobalReceiver(CommandTriggerPacket.PACKET_ID, (server,player, handler, buf, responseSender) -> {
    		// Read packet data on the event loop
		    String command = buf.readString();
		    server.execute(() -> {
        		LOGGER.info("Recieved custom packet with command "+command);
        		server.getCommandManager().executeWithPrefix(player.getCommandSource(),command);
	    	});
		});*/
		/*Registries.BLOCK.getIds().forEach(id->{
			QualityCommands.LOGGER.info(id.getNamespace()+":"+id.getPath());
			Registries.BLOCK.get(id).isShapeFullCube()
		});*/
	}
	/*public static void setupLoadData(){
		val loader1 = MassDatapackResolver.loader
        ResourceManagerHelper.get(SERVER_DATA)
            .registerReloadListener(new IdentifiableResourceReloadListener{
                @Override
				public ResourceLocation getFabricId(){
                    return ResourceLocation(ValkyrienSkiesMod.MOD_ID, "blocks")
                }
				@Override
                public CompletableFuture<Void> reload(
                    ResourceReloader.Synchronizer synchronizer,
                    ResourceManager resourceManager,
                    Executor prepareExecutor,
                    Executor gameExecutor
                ) {
                    return loader1.reload(
                        stage, resourceManager, preparationsProfiler, reloadProfiler,
                        backgroundExecutor, gameExecutor
                    )
                }
            })
	}*/
}