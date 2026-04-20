package ember.qualitycommands.commands;

import java.math.MathContext;
import java.util.EnumSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
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



public class TpRelCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("tpRel")
				.requires(source -> source.hasPermissionLevel(2))
				.then(
					CommandManager.argument("target", EntityArgumentType.entity())
					.then(
					CommandManager.argument("e1", EntityArgumentType.entity())
					.then(
					CommandManager.argument("e2", EntityArgumentType.entity())
						    
                                        .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												EntityArgumentType.getEntity(context, "e1"),
												EntityArgumentType.getEntity(context, "e2")
                                            )
                                        )
						.then(
							CommandManager.argument("rot", IntegerArgumentType.integer())
						.executes(
							context -> executealt(
								context.getSource(),
								EntityArgumentType.getEntity(context, "target"),
								EntityArgumentType.getEntity(context, "e1"),
								EntityArgumentType.getEntity(context, "e2"),
								IntegerArgumentType.getInteger(context, "rot")
							)
						))
					    )
				    )
						)
				
		);
	}

	private static int execute(CommandSource source, Entity target, Entity e_a, Entity e_b) throws CommandSyntaxException {
		target.requestTeleport(target.getX()+e_b.getX()-e_a.getX(),
		                   target.getY()+e_b.getY()-e_a.getY(),
						   target.getZ()+e_b.getZ()-e_a.getZ());
        //target.positionModified=true;
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
	private static int executealt(CommandSource source, Entity target, Entity e_a, Entity e_b, int rot) throws CommandSyntaxException {
		double x1=target.getX()-e_a.getX();
		double y1=target.getY()-e_a.getY();
		double z1=target.getZ()-e_a.getZ();
		float c = MathHelper.cos(rot*3.14159265f/180f);
		float s = MathHelper.sin(rot*3.14159265f/180f);
		double x2=x1*c-z1*s;
		double z2=x1*s+z1*c;
		ServerWorld world=(ServerWorld)e_b.getEntityWorld();
		//(1,0)->(c,s)
		//(0,1)->(-s,c)
		/*target.getX()+e_b.getX()-e_a.getX(),
		target.getY()+e_b.getY()-e_a.getY(),
		target.getZ()+e_b.getZ()-e_a.getZ()*/
		/*target.requestTeleport(x2+e_b.getX(),
		                   y1+e_b.getY(),
						   z2+e_b.getZ());
		target.setYaw((target.getYaw()+rot) % 360.0F);
		*/
		target.teleport(world, x2+e_b.getX(),
		                   y1+e_b.getY(),
						   z2+e_b.getZ(), EnumSet.noneOf(PositionFlag.class), (target.getYaw()+rot) % 360.0F, target.getPitch(),true);
		Vec3d tvel=target.getVelocity();
		target.setVelocity(tvel.getX()*c-tvel.getZ()*s,tvel.getY(),tvel.getX()*s+tvel.getZ()*c);
		target.velocityModified=true;
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
