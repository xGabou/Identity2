package ember.qualitycommands;

import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ResourceLocation;
import ember.qualitycommands.commands.TpRelCommand;
//import ember.qualitycommands.commands.SilentFunctionCommand;
import ember.qualitycommands.commands.AccelerateCommand;
import ember.qualitycommands.commands.AccelerateToPosCommand;
import ember.qualitycommands.commands.AccelerateAltCommand;
import ember.qualitycommands.commands.ConvertToEntityCommand;
import ember.qualitycommands.commands.ForLoopCommand;
import ember.qualitycommands.commands.HealCommand;
import ember.qualitycommands.commands.RunMultipleCommand;
import ember.qualitycommands.commands.SnekyFunctionCommand;
import ember.qualitycommands.commands.WithCommand;
import ember.qualitycommands.commands.AirCommand;
import ember.qualitycommands.commands.ModifyCustomEntityDataCommand;
import ember.qualitycommands.commands.ParticleWithBaseVelocityCommand;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModCommands{
	public static void initialize(){
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			TpRelCommand.register(dispatcher);
			//SilentFunctionCommand.register(dispatcher);
			AccelerateCommand.register(dispatcher);
			AccelerateAltCommand.register(dispatcher);
			AccelerateToPosCommand.register(dispatcher);
			ConvertToEntityCommand.register(dispatcher);
			ForLoopCommand.register(dispatcher);
			RunMultipleCommand.register(dispatcher);
			HealCommand.register(dispatcher);
			AirCommand.register(dispatcher);
			ModifyCustomEntityDataCommand.register(dispatcher);
			WithCommand.register(dispatcher,registryAccess);
			ParticleWithBaseVelocityCommand.register(dispatcher,registryAccess);
			SnekyFunctionCommand.register(dispatcher);
		});
	}
}