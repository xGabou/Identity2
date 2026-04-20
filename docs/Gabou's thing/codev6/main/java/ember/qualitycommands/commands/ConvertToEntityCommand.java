package ember.qualitycommands.commands;
import net.minecraft.state.StateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.OptionalInt;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.lang.Double;
import java.lang.String;
import ember.qualitycommands.QualityCommands;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import com.google.common.base.MoreObjects;
import net.minecraft.state.property.Property;
import java.util.stream.Collectors;
import net.minecraft.registry.Registries;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import com.mojang.brigadier.arguments.BoolArgumentType;
public class ConvertToEntityCommand {
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		CommandFunctionManager commandFunctionManager = context.getSource().getServer().getCommandFunctionManager();
		CommandSource.suggestIdentifiers(commandFunctionManager.getFunctionTags(), builder, "#");
		return CommandSource.suggestIdentifiers(commandFunctionManager.getAllFunctions(), builder);
	};

	public static String stateManagerToBetterString(BlockState state,StateManager manager){
		
		return MoreObjects.toStringHelper(manager)
			.add("block", manager.getOwner())
			.add("properties", manager.getProperties().stream().map(e -> {return ((Property)e).getName()+((String)"=")+(((Object)state.get(((Property)e))).toString());}).collect(Collectors.toList()))
			.toString();
		//result: StateManager{block=Block{minecraft:stone_stairs}, properties=[facing=east, half=bottom, shape=inner_left, waterlogged=false]}
	}
	public static String stateManagerToFallingBlockDataString(BlockState state,StateManager manager,BlockPos pos){
		String blockProperties=manager.getProperties().stream().map(e -> {return ((Property)e).getName()+((String)":\"")+(((Object)state.get(((Property)e))).toString())+((String)"\"");}).collect(Collectors.toList()).toString();		
		return "{Pos:["+String.valueOf(pos.getX()+0.5)+"f,"+String.valueOf(pos.getY())+"f,"+String.valueOf(pos.getZ()+0.5)+"f],"+
			(String)"BlockState:{Name:\""+(String)Registries.BLOCK.getId((Block)(manager.getOwner())).toString()+(String)"\",Properties:"+blockProperties.replaceAll("\\[","{").replaceAll("\\]","}")+"},NoGravity:1b,Time:-2147483647}";
	}
	public static void registerOptionless(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("entityify")
				.requires(source -> source.hasPermissionLevel(2))
				.then(
					CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						.suggests(SUGGESTION_PROVIDER)
						.executes(context -> execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"),false))
				)
		);
	}
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("entityify")
				.requires(source -> source.hasPermissionLevel(2))
				.then(
					CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						.suggests(SUGGESTION_PROVIDER)
						.executes(context -> execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"),false))
							.then(
								CommandManager.argument("removeBlock", BoolArgumentType.bool())
									.executes(context -> execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"),BoolArgumentType.getBool(context, "removeBlock")))
							)
				)
		);
	}

	private static int execute(ServerCommandSource source, BlockPos pos, boolean shouldKeepBlock) {
		if(shouldKeepBlock){
			return executeBlockEntity(source,pos);
		}else{
			return executeNormal(source,pos);
		}
	}
	private static int executeNormal(ServerCommandSource source, BlockPos pos) {
		ServerWorld serverWorld = source.getWorld();
		BlockState blockState = serverWorld.getBlockState(pos);
		QualityCommands.LOGGER.info(stateManagerToFallingBlockDataString(blockState,blockState.getBlock().getStateManager(),pos));
		source.getServer().getCommandManager().parseAndExecute(
			source.getServer().getCommandSource().withSilent(),
			(String)("summon falling_block ")+
			(String)(String.valueOf(pos.getX()+0.5))+
			(String)(" ")+
			(String)(String.valueOf(pos.getY()))+
			(String)(" ")+
			(String)(String.valueOf(pos.getZ()+0.5))+
			(String)(" ")+
			stateManagerToFallingBlockDataString(blockState,blockState.getBlock().getStateManager(),pos)
			);
		return 1;
	}
	private static int executeBlockEntity(ServerCommandSource source, BlockPos pos) {
		ServerWorld serverWorld = source.getWorld();
		BlockState blockState = serverWorld.getBlockState(pos);
		QualityCommands.LOGGER.info(stateManagerToFallingBlockDataString(blockState,blockState.getBlock().getStateManager(),pos));
		source.getServer().getCommandManager().parseAndExecute(
			source.getServer().getCommandSource().withSilent(),
			(String)("summon falling_block ")+
			(String)(String.valueOf(pos.getX()+0.5))+
			(String)(" ")+
			(String)(String.valueOf(pos.getY()))+
			(String)(" ")+
			(String)(String.valueOf(pos.getZ()+0.5))+
			(String)(" ")+
			stateManagerToFallingBlockDataString(blockState,blockState.getBlock().getStateManager(),pos)
			);
		serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
		return 1;
	}
}
