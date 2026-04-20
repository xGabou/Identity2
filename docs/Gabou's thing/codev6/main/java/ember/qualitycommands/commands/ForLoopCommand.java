package ember.qualitycommands.commands;

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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.util.math.Vec3d;

import ember.qualitycommands.QualityCommands;
public class ForLoopCommand {
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		CommandFunctionManager commandFunctionManager = context.getSource().getServer().getCommandFunctionManager();
		CommandSource.suggestIdentifiers(commandFunctionManager.getFunctionTags(), builder, "#");
		return CommandSource.suggestIdentifiers(commandFunctionManager.getAllFunctions(), builder);
	};

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("for")
				.requires(source -> source.hasPermissionLevel(2))
					.then(
									CommandManager.argument("name", StringArgumentType.string())
						.then(
									CommandManager.argument("start", IntegerArgumentType.integer())
							.then(
									CommandManager.argument("end", IntegerArgumentType.integer())
								.then(
									CommandManager.argument("increment", IntegerArgumentType.integer())
							.then(
								CommandManager.argument("command", StringArgumentType.greedyString())
									.suggests(SUGGESTION_PROVIDER)
									.executes(context -> execute(context.getSource(),IntegerArgumentType.getInteger(context, "start"),IntegerArgumentType.getInteger(context, "end"),IntegerArgumentType.getInteger(context, "increment"),StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "command")))
							)
						)
					)
				)
			)
		);
	}
	public static String replaceAllInString(String base, String from, String replace){
		for(int j=0;j<4;j+=1){
			base=base.replace(from,replace);
		}
		return base;
	}
	private static int execute(ServerCommandSource source,int start, int end, int incr, String varname, String command) {
		Vec3d originalPosition=source.getPosition();
		if(incr==0){
			throw new IllegalArgumentException(
						"Infinite Loop Detected. Loop Increments of 0 are not allowed!"
					);
		}else if(incr<0){
			
			throw new IllegalArgumentException(
						"Infinite Loop Detected. Loop Increments less than 0 are not allowed!"
					);
		}
		if(start>end){
			for(int i=start;i>end;i-=incr){
				source.getServer().getCommandManager().parseAndExecute(source.withPosition(originalPosition),replaceAllInString(command,"@("+varname+")",String.valueOf(i)));
			}

		}else{
			for(int i=start;i<end;i+=incr){
				source.getServer().getCommandManager().parseAndExecute(source.withPosition(originalPosition),replaceAllInString(command,"@("+varname+")",String.valueOf(i)));
			}
		}
		return 1;
	}
}
