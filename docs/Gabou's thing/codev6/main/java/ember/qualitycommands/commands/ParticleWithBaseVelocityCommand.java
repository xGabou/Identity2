package ember.qualitycommands.commands;

import com.ibm.icu.text.MessagePattern.Part;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import ember.qualitycommands.packets.ParticleWithBaseVelPacketPayload;

import java.util.Collection;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.CustomPayload;
public class ParticleWithBaseVelocityCommand {
	private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.particle.failed"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(
			CommandManager.literal("particlev")
				.requires(CommandManager.requirePermissionLevel(2))
				.then(
					CommandManager.argument("name", ParticleEffectArgumentType.particleEffect(registryAccess))
						.executes(
							context -> execute(
								context.getSource(),
								ParticleEffectArgumentType.getParticle(context, "name"),
								context.getSource().getPosition(),
								Vec3d.ZERO,
								0.0F,
                                Vec3d.ZERO,
								0,
								false,
								context.getSource().getServer().getPlayerManager().getPlayerList()
							)
						)
						.then(
							CommandManager.argument("pos", Vec3ArgumentType.vec3())
								.executes(
									context -> execute(
										context.getSource(),
										ParticleEffectArgumentType.getParticle(context, "name"),
										Vec3ArgumentType.getVec3(context, "pos"),
										Vec3d.ZERO,
										0.0F,
                                        Vec3d.ZERO,
										0,
										false,
										context.getSource().getServer().getPlayerManager().getPlayerList()
									)
								)
								.then(
									CommandManager.argument("delta", Vec3ArgumentType.vec3(false))
										.then(
											CommandManager.argument("speed", FloatArgumentType.floatArg(0.0F))
												.then(
                                                    
									CommandManager.argument("speedtarget", Vec3ArgumentType.vec3(false)).then(
													CommandManager.argument("count", IntegerArgumentType.integer(0))
														.executes(
															context -> execute(
																context.getSource(),
																ParticleEffectArgumentType.getParticle(context, "name"),
																Vec3ArgumentType.getVec3(context, "pos"),
																Vec3ArgumentType.getVec3(context, "delta"),
																FloatArgumentType.getFloat(context, "speed"),
																Vec3ArgumentType.getVec3(context, "speedtarget"),
																IntegerArgumentType.getInteger(context, "count"),
																false,
																context.getSource().getServer().getPlayerManager().getPlayerList()
															)
														)
														.then(
															CommandManager.literal("force")
																.executes(
																	context -> execute(
																		context.getSource(),
																		ParticleEffectArgumentType.getParticle(context, "name"),
																		Vec3ArgumentType.getVec3(context, "pos"),
																		Vec3ArgumentType.getVec3(context, "delta"),
																		FloatArgumentType.getFloat(context, "speed"),
																Vec3ArgumentType.getVec3(context, "speedtarget"),
																		IntegerArgumentType.getInteger(context, "count"),
																		true,
																		context.getSource().getServer().getPlayerManager().getPlayerList()
																	)
																)
																.then(
																	CommandManager.argument("viewers", EntityArgumentType.players())
																		.executes(
																			context -> execute(
																				context.getSource(),
																				ParticleEffectArgumentType.getParticle(context, "name"),
																				Vec3ArgumentType.getVec3(context, "pos"),
																				Vec3ArgumentType.getVec3(context, "delta"),
																				FloatArgumentType.getFloat(context, "speed"),
																Vec3ArgumentType.getVec3(context, "speedtarget"),
																				IntegerArgumentType.getInteger(context, "count"),
																				true,
																				EntityArgumentType.getPlayers(context, "viewers")
																			)
																		)
																)
														)
														.then(
															CommandManager.literal("normal")
																.executes(
																	context -> execute(
																		context.getSource(),
																		ParticleEffectArgumentType.getParticle(context, "name"),
																		Vec3ArgumentType.getVec3(context, "pos"),
																		Vec3ArgumentType.getVec3(context, "delta"),
																		FloatArgumentType.getFloat(context, "speed"),
																Vec3ArgumentType.getVec3(context, "speedtarget"),
																		IntegerArgumentType.getInteger(context, "count"),
																		false,
																		context.getSource().getServer().getPlayerManager().getPlayerList()
																	)
																)
																.then(
																	CommandManager.argument("viewers", EntityArgumentType.players())
																		.executes(
																			context -> execute(
																				context.getSource(),
																				ParticleEffectArgumentType.getParticle(context, "name"),
																				Vec3ArgumentType.getVec3(context, "pos"),
																				Vec3ArgumentType.getVec3(context, "delta"),
																				FloatArgumentType.getFloat(context, "speed"),
																Vec3ArgumentType.getVec3(context, "speedtarget"),
																				IntegerArgumentType.getInteger(context, "count"),
																				false,
																				EntityArgumentType.getPlayers(context, "viewers")
																			)
																		)
																)
														)
												))
										)
								)
						)
				)
		);
	}
	public static <T extends ParticleEffect> boolean spawnParticles(
		ServerWorld world,ServerPlayerEntity player,T parameters, boolean force, boolean important, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double speed, double basevelx,double basevely,double basevelz
	) {
		ParticleWithBaseVelPacketPayload particleS2CPacket = new ParticleWithBaseVelPacketPayload(
			(ParticleEffect)parameters, force, important, (float)x, (float)y, (float)z, (float)offsetX, (float)offsetY, (float)offsetZ, (float)speed, count,(float)basevelx,(float)basevely,(float)basevelz
		);
		int i = 0;

			if (sendToPlayerIfNearby(world,player, force, x, y, z, particleS2CPacket)) {
				i++;
			}
		

		return (i!=0);
	}
	public static final boolean sendToPlayerIfNearby(ServerWorld world, ServerPlayerEntity player, boolean force, double x, double y, double z, CustomPayload packet) {
      if (player.getEntityWorld() != world) {
         return false;
      } else {
         BlockPos blockPos = player.getBlockPos();
         if (blockPos.isWithinDistance(new Vec3d(x, y, z), force ? (double)512.0F : (double)32.0F)) {
            //player.networkHandler.sendPacket(packet);
			ServerPlayNetworking.send(player, packet);
            return true;
         } else {
            return false;
         }
      }
   }
	private static int execute(
		ServerCommandSource source, ParticleEffect parameters, Vec3d pos, Vec3d delta, float speed, Vec3d target, int count, boolean force, Collection<ServerPlayerEntity> viewers
	) throws CommandSyntaxException {
		int i = 0;

		for (ServerPlayerEntity serverPlayerEntity : viewers) {
			if (spawnParticles(source.getWorld(),serverPlayerEntity, parameters, force, false, pos.x, pos.y, pos.z, count, delta.x, delta.y, delta.z, speed,target.x,target.y,target.z)) {
				i++;
			}
		}

		if (i == 0) {
			throw FAILED_EXCEPTION.create();
		} else {
			source.sendFeedback(() -> Text.translatable("commands.particle.success", Registries.PARTICLE_TYPE.getId(parameters.getType()).toString()), true);
			return i;
		}
	}
}
