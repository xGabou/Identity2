package ember.qualitycommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.function.IntFunction;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.BossBarCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import org.apache.commons.lang3.mutable.MutableObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.command.CommandFunctionAction;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ControlFlowAware;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.ExecutionControl;
import net.minecraft.command.ExecutionFlags;
import net.minecraft.command.FallthroughCommandAction;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import ember.qualitycommands.QualityCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ResourceLocationArgumentType;
import net.minecraft.server.command.DataCommand;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.random.Random;
import java.text.DecimalFormat;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtLong;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import ember.qualitycommands.util.Calculator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
public class WithCommand {
	private static final Dynamic2CommandExceptionType PLAYERS_GET_NULL_EXCEPTION = new Dynamic2CommandExceptionType(
		(objective, target) -> Text.stringifiedTranslatable("commands.scoreboard.players.get.null", objective, target)
	);
	private static final Dynamic2CommandExceptionType NO_ATTRIBUTE_EXCEPTION = new Dynamic2CommandExceptionType(
		(entityName, attributeName) -> Text.stringifiedTranslatable("commands.attribute.failed.no_attribute", entityName, attributeName)
	);
	private static final DynamicCommandExceptionType ARGUMENT_NOT_COMPOUND_EXCEPTION = new DynamicCommandExceptionType(
             argument -> Text.stringifiedTranslatable("commands.function.error.argument_not_compound", argument)
	);
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		CommandFunctionManager commandFunctionManager = context.getSource().getServer().getCommandFunctionManager();
		CommandSource.suggestResourceLocations(commandFunctionManager.getFunctionTags(), builder, "#");
		return CommandSource.suggestResourceLocations(commandFunctionManager.getAllFunctions(), builder);
	};
	private static final DynamicCommandExceptionType ENTITY_FAILED_EXCEPTION = new DynamicCommandExceptionType(
		name -> Text.stringifiedTranslatable("commands.attribute.failed.entity", name)
	);

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		LiteralArgumentBuilder<ServerCommandSource> dataSubcommand = CommandManager.literal("data");

		for (DataCommand.ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
		objectType.addArgumentsToBuilder(dataSubcommand, builder -> builder.then(
			CommandManager.literal(".*")
			.then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		return WithCommand.toString(objectType.getObject(context).getNbt());
		//return (DataCommand.getNbt(NbtPathArgumentType.getNbtPath(context, "path"), objectType.getObject(context))).toString();
		}
		})  )).then(
			CommandManager.argument("path", NbtPathArgumentType.nbtPath())
			.then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		return WithCommand.toString(DataCommand.getNbt(NbtPathArgumentType.getNbtPath(context, "path"), objectType.getObject(context)));
		//return (DataCommand.getNbt(NbtPathArgumentType.getNbtPath(context, "path"), objectType.getObject(context))).toString();
		}
		})  )));
		}
		
		LiteralArgumentBuilder<ServerCommandSource> valueSubcommand = CommandManager.literal("value");
		
		
		valueSubcommand.then(
		CommandManager.argument("value", StringArgumentType.string()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return StringArgumentType.getString(context, "value");
		}
		}
		)));

		LiteralArgumentBuilder<ServerCommandSource> calculateSubcommand = CommandManager.literal("calculate");
		
		
		calculateSubcommand.then(
		CommandManager.argument("math", StringArgumentType.string()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return Calculator.calculate(StringArgumentType.getString(context, "math")).toString();
		}
		}
		)));
		LiteralArgumentBuilder<ServerCommandSource> calculateIntSubcommand = CommandManager.literal("calculateint");
		
		
		calculateIntSubcommand.then(
		CommandManager.argument("math", StringArgumentType.string()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return String.valueOf((int)(float)Calculator.calculate(StringArgumentType.getString(context, "math")));
		}
		}
		)));
		// /for x 0 21 1 with y calculateint "(0-1*@(x)*@(x)+@(x)*20)/5" runmulti 1 setblock ~@(x) ~@(y) ~ glowstone@<1>with y2 calculateint "(0-1*(@(x)-1)*(@(x)-1)+(@(x)-1)*20)/5" for y3 @(y) @(y2) 1 setblock ~@(x) ~@(y3) ~ glowstone
		// /for x 0 21 1 with y calculate "(0-1*@(x)*@(x)+@(x)*20)/5" setblock ~@(x) ~@(y) ~ dirt
				// /runmulti 1 setblock ~ ~ ~ air@<1>execute as @p at @s align xyz positioned ~0.5 ~ ~0.5 run for theta 0 361 5 runmulti 0 tp @s ~ ~ ~ @(theta) 0@<0>execute rotated as @s run for d 0 11 1 for db 0 10 1 with y calculateint "(100-(@(d).@(db)^2))^0.5" execute positioned ^ ^ ^@(d).@(db) run setblock ~ ~@(y) ~ stone_bricks
		LiteralArgumentBuilder<ServerCommandSource> uuidSubcommand = CommandManager.literal("uuid");
		
		
		uuidSubcommand.then(
		CommandManager.argument("target", EntityArgumentType.entity()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return EntityArgumentType.getEntity(context, "target").getUuidAsString();
		}
		}
		)));

		LiteralArgumentBuilder<ServerCommandSource> randomSubcommand = CommandManager.literal("random");
		LiteralArgumentBuilder<ServerCommandSource> randomIntSubcommand = CommandManager.literal("int");
		LiteralArgumentBuilder<ServerCommandSource> randomFloatSubcommand = CommandManager.literal("float");
		
		
		randomIntSubcommand.then(
		CommandManager.argument("min", IntegerArgumentType.integer()).then(
		CommandManager.argument("max", IntegerArgumentType.integer()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return String.valueOf(WithCommand.RANDOM.nextBetween(IntegerArgumentType.getInteger(context, "min"),IntegerArgumentType.getInteger(context, "max")));
		}
		}
		)))
		);
		randomFloatSubcommand.then(
		CommandManager.argument("min", FloatArgumentType.floatArg()).then(
		CommandManager.argument("max", FloatArgumentType.floatArg()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			float min=FloatArgumentType.getFloat(context, "min");
			float max=FloatArgumentType.getFloat(context, "max");
			return String.valueOf(WithCommand.RANDOM.nextFloat()*(max-min)+min);
		}
		}
		)))
		);
		randomSubcommand.then(randomIntSubcommand).then(randomFloatSubcommand);







		LiteralArgumentBuilder<ServerCommandSource> attributeSubCommand = CommandManager.literal("attribute");
		


		attributeSubCommand.then(CommandManager.argument("target", EntityArgumentType.entity())
						.then(
							CommandManager.argument("attribute", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ATTRIBUTE))
								.then(
									CommandManager.argument("command", StringArgumentType.greedyString())
										.executes(
											new WithCommand.Command() {
											@Override
											protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
												return String.valueOf(executeValueGet(
													context.getSource(),
													EntityArgumentType.getEntity(context, "target"),
													RegistryEntryReferenceArgumentType.getEntityAttribute(context, "attribute"),
													1.0
												));
											}
											}
										)
										
								)
							)
		);

		
			
		





		LiteralArgumentBuilder<ServerCommandSource> scoreboardSubCommand = CommandManager.literal("score");
		scoreboardSubCommand.then(
		
		CommandManager.argument("target", ScoreHolderArgumentType.scoreHolder())
			.suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER)
				.then(
		CommandManager.argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective()).then(
		CommandManager.argument("command", StringArgumentType.greedyString()).executes(
		new WithCommand.Command() {
		@Override
		protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			return String.valueOf(getScoreboardScore(
														context.getSource(),
														ScoreHolderArgumentType.getScoreHolder(context, "target"),
														ScoreboardObjectiveArgumentType.getObjective(context, "objective")
													));
		}
		}
		)))
		);

		LiteralArgumentBuilder<ServerCommandSource> bossbarSubCommand = CommandManager.literal("bossbar");
		bossbarSubCommand.then(
							CommandManager.argument("id", ResourceLocationArgumentType.ResourceLocation())
								.suggests(SUGGESTION_PROVIDER)
								.then(CommandManager.literal("value").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(
									new WithCommand.Command() {
										@Override
										protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
											return String.valueOf(bbgetValue(context.getSource(), BossBarCommand.getBossBar(context)));
									}}
							)))
								.then(CommandManager.literal("max").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(
									new WithCommand.Command() {
										@Override
										protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
											return String.valueOf(bbgetMaxValue(context.getSource(), BossBarCommand.getBossBar(context)));
									}}
							)))
								.then(CommandManager.literal("visible").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(
									new WithCommand.Command() {
										@Override
										protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
											return String.valueOf(bbisVisible(context.getSource(), BossBarCommand.getBossBar(context)));
									}}
							)))
								.then(CommandManager.literal("players").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(
									new WithCommand.Command() {
										@Override
										protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
											return String.valueOf(bbgetPlayers(context.getSource(), BossBarCommand.getBossBar(context)));
									}}
							)))
						);
		
		
		
		
		
		LiteralArgumentBuilder<ServerCommandSource> blockPropertiesSubCommand = CommandManager.literal("blockproperties");
		blockPropertiesSubCommand.then(
							CommandManager.argument("pos", BlockPosArgumentType.blockPos())
								.then(CommandManager.literal("hardness").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(
									new WithCommand.Command() {
										@Override
										protected String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
											return String.valueOf(context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context,"pos")).getBlock().getHardness());
									}}
							))));
		
		
		



		
		dispatcher.register(
		CommandManager.literal("with")
			.requires(CommandManager.requirePermissionLevel(2))
			.then(
				CommandManager.argument("name", StringArgumentType.string())
				.then(valueSubcommand)
				.then(uuidSubcommand)
				.then(randomSubcommand)
				.then(calculateSubcommand)
				.then(calculateIntSubcommand)
				.then(attributeSubCommand)
				.then(dataSubcommand)
				.then(scoreboardSubCommand)
				.then(bossbarSubCommand)
				.then(blockPropertiesSubCommand)
			)
		);

	}
	private static final Random RANDOM=Random.create();
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
	public static String toString(NbtElement nbt) {
		if (nbt.getType()==NbtElement.BYTE_TYPE) {
			return String.valueOf(nbt.asByte().get());
		} else if (nbt.getType()==NbtElement.SHORT_TYPE) {
			return String.valueOf(nbt.asShort().get());
		} else if (nbt.getType()==NbtElement.INT_TYPE) {
			return String.valueOf(nbt.asInt().get());
		} else if (nbt.getType()==NbtElement.LONG_TYPE) {
			return String.valueOf(nbt.asLong().get());
		} else if (nbt.getType()==NbtElement.DOUBLE_TYPE) {
			return String.valueOf(nbt.asDouble().get());
		} else if (nbt.getType()==NbtElement.STRING_TYPE) {
			return (nbt.asString().get());
		} else {
			return nbt.toString();
		}
	}
	static NbtCompound getArgument(NbtPathArgumentType.NbtPath path, DataCommandObject object) throws CommandSyntaxException {
              NbtElement nbtElement = DataCommand.getNbt(path, object);
             if (nbtElement instanceof NbtCompound nbtCompound) {
                     return nbtCompound;
             } else {
                     throw ARGUMENT_NOT_COMPOUND_EXCEPTION.create(nbtElement.getNbtType().getCrashReportName());
             }
    }


	abstract static class Command extends ControlFlowAware.Helper<ServerCommandSource> implements ControlFlowAware.Command<ServerCommandSource> {
	            @Nullable
	            protected abstract String getArguments(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
	
	            public void executeInner(
	                    ServerCommandSource serverCommandSource,
	                    ContextChain<ServerCommandSource> contextChain,
	                    ExecutionFlags executionFlags,
	                    ExecutionControl<ServerCommandSource> executionControl
	            ) throws CommandSyntaxException {
	                    CommandContext<ServerCommandSource> commandContext = contextChain.getTopContext().copyFor(serverCommandSource);	                    
						String arg = this.getArguments(commandContext);
						String varname = StringArgumentType.getString(commandContext, "name");
						String commandToRun = StringArgumentType.getString(commandContext, "command");
						QualityCommands.LOGGER.info(
							WithCommand.replaceAllInString(
								commandToRun,
								"@("+varname+")",
								arg
							));
						serverCommandSource.getServer().getCommandManager().parseAndExecute(
							serverCommandSource,
							WithCommand.replaceAllInString(
								commandToRun,
								"@("+varname+")",
								arg
							)
						);
	                    
	            }
	     }

	public static String replaceAllInString(String base, String from, String replace){
		//for(int j=0;j<4;j+=1){
			base=base.replace(from,replace);
		//}
		return base;
	}
	public static String[] splitString(String base,String splitable){
		String[] returnval=base.split(splitable,1000);
		return returnval;
	}
	private static double executeValueGet(ServerCommandSource source, Entity target, RegistryEntry<EntityAttribute> attribute, double multiplier) throws CommandSyntaxException {
		LivingEntity livingEntity = getLivingEntityWithAttribute(target, attribute);
		double d = livingEntity.getAttributeValue(attribute);
		return d;
	}
	private static LivingEntity getLivingEntityWithAttribute(Entity entity, RegistryEntry<EntityAttribute> attribute) throws CommandSyntaxException {
		LivingEntity livingEntity = getLivingEntity(entity);
		if (!livingEntity.getAttributes().hasAttribute(attribute)) {
			throw NO_ATTRIBUTE_EXCEPTION.create(entity.getName(), getName(attribute));
		} else {
			return livingEntity;
		}
	}
	private static LivingEntity getLivingEntity(Entity entity) throws CommandSyntaxException {
		if (!(entity instanceof LivingEntity)) {
			throw ENTITY_FAILED_EXCEPTION.create(entity.getName());
		} else {
			return (LivingEntity)entity;
		}
	}
	private static Text getName(RegistryEntry<EntityAttribute> attribute) {
		return Text.translatable(attribute.value().getTranslationKey());
	}
	/*private static ItemStack getStackInSlot(Entity entity, int slotId) throws CommandSyntaxException {
		StackReference stackReference = entity.getStackReference(slotId);
		if (stackReference == StackReference.EMPTY) {
			throw NO_SUCH_SLOT_SOURCE_EXCEPTION.create(slotId);
		} else {
			return stackReference.get().copy();
		}
	}*/
	private static int getScoreboardScore(ServerCommandSource source, ScoreHolder scoreHolder, ScoreboardObjective objective) throws CommandSyntaxException {
		Scoreboard scoreboard = source.getServer().getScoreboard();
		ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, objective);
		if (readableScoreboardScore == null) {
			throw PLAYERS_GET_NULL_EXCEPTION.create(objective.getName(), scoreHolder.getStyledDisplayName());
		} else {
			/*source.sendFeedback(
				() -> Text.translatable(
					"commands.scoreboard.players.get.success", scoreHolder.getStyledDisplayName(), readableScoreboardScore.getScore(), objective.toHoverableText()
				),
				false
			);*/
			return readableScoreboardScore.getScore();
		}
	}

	private static ServerCommandSource executeStoreBossbar(ServerCommandSource source, CommandBossBar bossBar, boolean storeInValue, boolean requestResult) {
		return source.mergeReturnValueConsumers((successful, returnValue) -> {
			int i = requestResult ? returnValue : (successful ? 1 : 0);
			if (storeInValue) {
				bossBar.setValue(i);
			} else {
				bossBar.setMaxValue(i);
			}
		}, ReturnValueConsumer::chain);
	}

	private static int bbgetValue(ServerCommandSource source, CommandBossBar bossBar) {
		//source.sendFeedback(() -> Text.translatable("commands.bossbar.get.value", bossBar.toHoverableText(), bossBar.getValue()), true);
		return bossBar.getValue();
	}

	private static int bbgetMaxValue(ServerCommandSource source, CommandBossBar bossBar) {
		//source.sendFeedback(() -> Text.translatable("commands.bossbar.get.max", bossBar.toHoverableText(), bossBar.getMaxValue()), true);
		return bossBar.getMaxValue();
	}

	private static int bbisVisible(ServerCommandSource source, CommandBossBar bossBar) {
		if (bossBar.isVisible()) {
			//source.sendFeedback(() -> Text.translatable("commands.bossbar.get.visible.visible", bossBar.toHoverableText()), true);
			return 1;
		} else {
			//source.sendFeedback(() -> Text.translatable("commands.bossbar.get.visible.hidden", bossBar.toHoverableText()), true);
			return 0;
		}
	}

	private static String bbgetPlayers(ServerCommandSource source, CommandBossBar bossBar) {
		if (bossBar.getPlayers().isEmpty()) {
			//source.sendFeedback(() -> Text.translatable("commands.bossbar.get.players.none", bossBar.toHoverableText()), true);
			return "[]";
		} else {
			/*source.sendFeedback(
				() -> Text.translatable(
					"commands.bossbar.get.players.some",
					bossBar.toHoverableText(),
					bossBar.getPlayers().size(),
					Texts.join(bossBar.getPlayers(), PlayerEntity::getDisplayName)

				),
				true
			);*/
			String players="[";
			for(PlayerEntity player:bossBar.getPlayers()){
				String name=player.getDisplayName().getString();
				if(name.charAt(name.length()-1)!='['){
					players+=",";
				}
				players+=name;
			}
			return players+"]";
		}

	}
}
//execute as @n[type=item] run with d data entity @s Item summon item ~ ~5 ~ {Item:@(d)}