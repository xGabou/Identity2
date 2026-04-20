package ember.qualitycommands.mixin;

import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.BlockPosArgumentType;
import ember.qualitycommands.commands.argument.FluidPredicateArgumentType;
import net.minecraft.block.pattern.CachedBlockPosition;
@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin{
	@Inject(at = @At("RETURN"), method = "addConditionArguments")
	private static void addConditionArgumentsMixin(
		CommandNode<ServerCommandSource> root,
		LiteralArgumentBuilder<ServerCommandSource> argumentBuilder,
		boolean positive,
		CommandRegistryAccess commandRegistryAccess,
		CallbackInfoReturnable info
	)
	{
		argumentBuilder.then(
			CommandManager.literal("submerged")
				.then(
					ExecuteCommand.addConditionLogic(
						root,
						CommandManager.argument("entity", EntityArgumentType.entity()),
						positive,
						context -> EntityArgumentType.getEntity(context, "entity").isSubmergedIn(FluidTags.WATER) == true
					)
				)
		);
		argumentBuilder.then(
			CommandManager.literal("fluid")
			.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
				.then(
					ExecuteCommand.addConditionLogic(
						root,
						CommandManager.argument("fluid", FluidPredicateArgumentType.fluidPredicate(commandRegistryAccess)),
						positive,
						context ->  FluidPredicateArgumentType.getFluidPredicate(context, "fluid")
										.test(new CachedBlockPosition(context.getSource().getWorld(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), true))
					)
				)
			)
		);
		argumentBuilder.then(
			CommandManager.literal("blockstate")
				.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
					.then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("solid"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isSolid()
						)
					)
					  .then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("blocks_movement"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).blocksMovement()
						)
					)
					  .then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("transparent"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isTransparent()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("exceeds_cube"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).exceedsCube()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("air"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isAir()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("burnable"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isBurnable()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("liquid"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isLiquid()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("random_ticking"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).hasRandomTicks()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("tool_required"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isToolRequired()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("has_block_break_particles"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).hasBlockBreakParticles()
						)
					).then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("replaceable"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).isReplaceable()
						)
					)/*.then(
						ExecuteCommand.addConditionLogic(
							root,
							CommandManager.literal("matches_fluid"),
							positive,
							context -> context.getSource().getWorld().getBlockState(BlockPosArgumentType.getBlockPos(context, "pos")).getFluidState()
						)
					)*/
				)
		);
	}
}


