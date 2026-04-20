package ember.qualitycommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import net.minecraft.server.command.ServerCommandSource;



public class AccelerateCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("accelerate")
				.requires(source -> source.hasPermissionLevel(2))
				.then(
					CommandManager.argument("target", EntityArgumentType.entity())
						.then(
							CommandManager.argument("xyz", Vec3ArgumentType.vec3())
						    
                                        .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
                                                Vec3ArgumentType.getVec3(context,"xyz")
                                            )
                                        )
                                
                            
						)
				)
		);
	}

	private static int execute(CommandSource source, Entity target, Vec3d xyz) throws CommandSyntaxException {
		target.setVelocity(xyz.add(target.getVelocity()).multiply(1));
        target.velocityModified=true;
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
}
