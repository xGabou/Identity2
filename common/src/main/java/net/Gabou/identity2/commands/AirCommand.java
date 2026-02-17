package net.Gabou.identity2.commands;

import java.math.MathContext;
import java.util.EnumSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.math.MathHelper;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
//import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.entity.EntityStatuses;

public class AirCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		
		dispatcher.register(
			CommandManager.literal("air")
				.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
				.then(
					CommandManager.argument("target", EntityArgumentType.entity())

					/*.then(
						CommandManager.literal("if")
						.then(
							CommandManager.literal("submerged")
							.then(CommandManager.argument("command", StringArgumentType.greedyString())
								.executes(
									context -> {
										Entity target=EntityArgumentType.getEntity(context, "target");
										if(target.isSubmergedIn(FluidTags.WATER)){
											context.getSource().getServer().getCommandManager().parseAndExecute(context.getSource(),StringArgumentType.getString(context, "command"));
										}
										return 1;
									}
								)
							)
						)
						.then(
							CommandManager.literal("notsubmerged")
							.then(CommandManager.argument("command", StringArgumentType.greedyString())
								.executes(
									context -> {
										Entity target=EntityArgumentType.getEntity(context, "target");
										if(target.isSubmergedIn(FluidTags.WATER)==false){
											context.getSource().getServer().getCommandManager().parseAndExecute(context.getSource(),StringArgumentType.getString(context, "command"));
										}
										return 1;
									}
								)
							)
						)
					)*/
					
					.then(
						CommandManager.literal("remove")
						.then(
							CommandManager.argument("amount", IntegerArgumentType.integer())
							.executes(
								context -> execute(
									context.getSource(),
									EntityArgumentType.getEntity(context, "target"),
									0,
									IntegerArgumentType.getInteger(context, "amount")
								)
							)
						)
					)
					.then(
						CommandManager.literal("add")
						.then(
							CommandManager.argument("amount", IntegerArgumentType.integer())
							.executes(
								context -> execute(
									context.getSource(),
									EntityArgumentType.getEntity(context, "target"),
									1,
									IntegerArgumentType.getInteger(context, "amount")
								)
							)
						)
					)
					.then(
						CommandManager.literal("set")
						.then(
							CommandManager.argument("amount", IntegerArgumentType.integer())
							.executes(
								context -> execute(
									context.getSource(),
									EntityArgumentType.getEntity(context, "target"),
									2,
									IntegerArgumentType.getInteger(context, "amount")
								)
							)
						)
					)
					
					
				
		));
	}

	/*
	if (this.isSubmergedIn(FluidTags.WATER)
		&& !source.getServer().getBlockState(BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ())).isOf(Blocks.BUBBLE_COLUMN)) {
		boolean bl2 = !this.canBreatheInWater() && !StatusEffectUtil.hasWaterBreathing(this) && (!bl || !((PlayerEntity)this).getAbilities().invulnerable);
		if (bl2) {
			this.setAir(this.getNextAirUnderwater(this.getAir()));
			if (this.getAir() == -20) {
				this.setAir(0);
				source.getServer().sendEntityStatus(this, EntityStatuses.ADD_BUBBLE_PARTICLES);
				this.damage(source.getServer(), this.getDamageSources().drown(), 2.0F);
			}
		} else if (this.getAir() < this.getMaxAir()) {
			this.setAir(this.getNextAirOnLand(this.getAir()));
		}

		if (this.hasVehicle() && this.getVehicle() != null && this.getVehicle().shouldDismountUnderwater()) {
			this.stopRiding();
		}
	} else if (this.getAir() < this.getMaxAir()) {
		this.setAir(this.getNextAirOnLand(this.getAir()));
	}
	*/
	private static int execute(ServerCommandSource source, Entity target,int type, int amount) throws CommandSyntaxException {
		if(type==0){
			target.setAir(target.getAir()-amount);
			if (target.getAir() <= -20) {
				target.setAir(0);
				source.getWorld().sendEntityStatus(target, EntityStatuses.ADD_BUBBLE_PARTICLES);
				target.damage(source.getWorld(), target.getDamageSources().drown(), 2.0F);
			}
		}else if(type==1){
			target.setAir(target.getAir()+amount);
			if(target.getAir()>target.getMaxAir()){
				target.setAir(target.getMaxAir());
			}
		}else if(type==2){
			target.setAir(amount);
			if (target.getAir() <= -20) {
				target.setAir(0);
				source.getWorld().sendEntityStatus(target, EntityStatuses.ADD_BUBBLE_PARTICLES);
				target.damage(source.getWorld(), target.getDamageSources().drown(), 2.0F);
			}
			if(target.getAir()>target.getMaxAir()){
				target.setAir(target.getMaxAir());
			}
		}
		//target.heal(amount);
        //target.positionModified=true;
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
	
}
