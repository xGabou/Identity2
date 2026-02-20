package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.OptionalInt;
import org.apache.commons.lang3.mutable.MutableObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.Gabou.identity2.Identity2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.world.phys.Vec3;
public class RunMultipleCommand {
	public static final SuggestionProvider<CommandSourceStack> SUGGESTION_PROVIDER = (context, builder) -> {
		ServerFunctionManager commandFunctionManager = context.getSource().getServer().getFunctions();
		SharedSuggestionProvider.suggestResource(commandFunctionManager.getTagNames(), builder, "#");
		return SharedSuggestionProvider.suggestResource(commandFunctionManager.getFunctionNames(), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("runmulti")
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
					.then(
									Commands.argument("name", StringArgumentType.string())
						
							.then(
								Commands.argument("command", StringArgumentType.greedyString())
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
	private static int execute(CommandSourceStack source, String seperator, String command) {
		Vec3 originalPosition=source.getPosition();
		for(String singleCommand : splitString(command,"@<"+seperator+">")){
			Identity2.LOGGER.info(singleCommand);
		source.getServer().getCommands().performPrefixedCommand(source.withPosition(originalPosition),singleCommand);
		}
		
		return 1;
	}
}
