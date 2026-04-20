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
public class SnekyFunctionCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("snekyfunction")
				.requires(source -> source.hasPermissionLevel(0))
				.then(
					CommandManager.argument("command", StringArgumentType.greedyString())
						.executes(context -> execute(context.getSource(),StringArgumentType.getString(context, "command")))
				)
		);
	}
	private static int execute(ServerCommandSource source, String command) {
		Vec3d originalPosition=source.getPosition();
		source.getServer().getCommandManager().parseAndExecute(source.withPosition(originalPosition),"function "+command);
		return 1;
	}
}
