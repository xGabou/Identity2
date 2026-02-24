package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;



public class AccelerateToPosCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("acceleratetopos")
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
				.then(
					Commands.argument("target", EntityArgument.entity())
						.then(
							Commands.argument("pos", Vec3Argument.vec3())
						    
                                        .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                Vec3Argument.getVec3(context,"pos"),
												1.0F,
												0.0F
                                            )
                                        )
							.then(Commands.argument("mult", FloatArgumentType.floatArg())
									 .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                Vec3Argument.getVec3(context,"pos"),
                                                FloatArgumentType.getFloat(context,"mult"),
												0.0F
                                            )
                                        )
								  .then(Commands.argument("antimult", FloatArgumentType.floatArg())
									 .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                Vec3Argument.getVec3(context,"pos"),
                                                FloatArgumentType.getFloat(context,"mult"),
												FloatArgumentType.getFloat(context,"antimult")
                                            )
                                        )
								  
								  )
								 )
                                
                            
						)
				)
		);
	}

	private static int execute(SharedSuggestionProvider source, Entity target, Vec3 xyz,float mult,float antimult) throws CommandSyntaxException {
		Vec3 pos=target.position();
		target.setDeltaMovement(target.getDeltaMovement().scale(antimult).add(xyz.subtract(pos).scale(mult)));
        target.hurtMarked = true;
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
}

