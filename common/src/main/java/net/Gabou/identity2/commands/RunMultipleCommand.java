package net.Gabou.identity2.commands;

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

import net.Gabou.identity2.Identity2;
public class RunMultipleCommand {
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		CommandFunctionManager commandFunctionManager = context.getSource().getServer().getCommandFunctionManager();
		CommandSource.suggestIdentifiers(commandFunctionManager.getFunctionTags(), builder, "#");
		return CommandSource.suggestIdentifiers(commandFunctionManager.getAllFunctions(), builder);
	};

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("runmulti")
				.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
					.then(
									CommandManager.argument("name", StringArgumentType.string())
						
							.then(
								CommandManager.argument("command", StringArgumentType.greedyString())
									.suggests(SUGGESTION_PROVIDER)
									.executes(context -> execute(context.getSource(),StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "command")))
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
	public static String[] splitString(String base,String splitable){
		String[] returnval=base.split(splitable,1000);
		return returnval;
	}
	private static int execute(ServerCommandSource source, String seperator, String command) {
		Vec3d originalPosition=source.getPosition();
		for(String singleCommand : splitString(command,"@<"+seperator+">")){
			Identity2.LOGGER.info(singleCommand);
		source.getServer().getCommandManager().parseAndExecute(source.withPosition(originalPosition),singleCommand);
		}
		
		return 1;
	}
}
