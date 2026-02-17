package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
//import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.component.type.NbtComponent;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
public class ModifyCustomEntityDataCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("custom_attribute")
				.requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
				.then(
					CommandManager.argument("target", EntityArgumentType.entity())
						.then(
							CommandManager.literal("reset")
                                        .executes(
                                            context -> executeReset(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target")
                                            )
                                        )
							
                            
						)
						.then(
							CommandManager.literal("horizontal_collision_speed_multiplier_override").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"horizontal_collision_speed_multiplier_override",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            
						)
						.then(
							CommandManager.literal("land_speed_multiplier_override").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"land_speed_multiplier_override",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("width_override").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"width_override",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("height_override").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"height_override",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("model_override").then(
							CommandManager.argument("value", StringArgumentType.string())
						    
                                        .executes(
                                            context -> executeString( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"model_override",
                                                StringArgumentType.getString(context,"value")
                                            )
                                        )
							).then(
									CommandManager.literal("identity").then(
									
								CommandManager.argument("value", StringArgumentType.string())
						    			
                                        .executes(
                                            context -> {
												String value=StringArgumentType.getString(context,"value");
												Entity target=EntityArgumentType.getEntity(context, "target");
												

												
												
												executeString( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"model_override",
                                                StringArgumentType.getString(context,"value")
                                            );
										execute(context.getSource(),EntityArgumentType.getEntity(context, "target"),"height_override",0);
										execute(context.getSource(),EntityArgumentType.getEntity(context, "target"),"width_override",0);
										
										if(value.length()!=0){
										if(((EntityAccessor)target).getCurrentIdentity()!=null){
										//if(EntityArgumentType.getEntity(context, "target") instanceof LivingEntity){
										//	((LivingEntity)EntityArgumentType.getEntity(context, "target")).getAttributes().setFrom(((LivingEntity)((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCurrentIdentity()).getAttributes());
										//}

										execute(context.getSource(),EntityArgumentType.getEntity(context, "target"),"height_override",((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCurrentIdentity().getHeight());
										execute(context.getSource(),EntityArgumentType.getEntity(context, "target"),"width_override",((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCurrentIdentity().getWidth());
										
										}
										}
										return 1;
										}
                                        )
									)
							)
                            
						).then(
							CommandManager.literal("width_override").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"width_override",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("model_hide_part").then(
							CommandManager.argument("part", StringArgumentType.string())
						    
                                        .executes(
                                            context -> executeBool(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"hidden_parts."+
                                                StringArgumentType.getString(context,"part"),
												true
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("model_show_part").then(
							CommandManager.argument("part", StringArgumentType.string())
						    
                                        .executes(
                                            context -> executeBool(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"hidden_parts."+
                                                StringArgumentType.getString(context,"part"),
												false
                                            )
                                        )
							)
                            
						)










						.then(
							CommandManager.literal("on_removed").then(
							CommandManager.argument("value", StringArgumentType.greedyString())
						    
                                        .executes(
                                            context -> executeString(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"on_removed",
                                                StringArgumentType.getString(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("on_removed_killed").then(
							CommandManager.argument("value", StringArgumentType.greedyString())
						    
                                        .executes(
                                            context -> executeString(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"on_removed_killed",
                                                StringArgumentType.getString(context,"value")
                                            )
                                        )
							)
                            
						).then(
							CommandManager.literal("on_removed_discarded").then(
							CommandManager.argument("value", StringArgumentType.greedyString())
						    
                                        .executes(
                                            context -> executeString(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"on_removed_discarded",
                                                StringArgumentType.getString(context,"value")
                                            )
                                        )
							)
                            
						)




				)
		);
	}

	private static int execute(CommandSource source, Entity target,String targetS, float amount) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		((NbtComponentAccessor)(Object)n).getNbt().putDouble(targetS,amount);
        CustomEntityDataS2CPacketPayload payload = new CustomEntityDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.Entry(targetS,amount)));
        sendToTrackingPlayers(target, payload);
		if(target instanceof PlayerEntity player){
			NetworkManager.sendToPlayer((ServerPlayerEntity) player, payload);
			net.Gabou.identity2.Identity2.LOGGER.info("Packet Sent! (to owner)");
		}
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
	
	private static int executeString(CommandSource source, Entity target,String targetS, String amount) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		((NbtComponentAccessor)(Object)n).getNbt().putString(targetS,amount);
        CustomEntityStringDataS2CPacketPayload payload = new CustomEntityStringDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryString(targetS,amount)));
        sendToTrackingPlayers(target, payload);
		if(target instanceof PlayerEntity player){
			NetworkManager.sendToPlayer((ServerPlayerEntity) player, payload);
			net.Gabou.identity2.Identity2.LOGGER.info("Packet Sent! (to owner)");
		}
		if(targetS.matches("model_override")){
			((EntityAccessor)target).setCurrentIdentity(amount);
		}
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}

	private static int executeBool(CommandSource source, Entity target,String targetS,boolean z) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		((NbtComponentAccessor)(Object)n).getNbt().putBoolean(targetS,z);
        CustomEntityBoolDataS2CPacketPayload payload = new CustomEntityBoolDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryBool(targetS,z)));
        sendToTrackingPlayers(target, payload);
		if(target instanceof PlayerEntity player){
			NetworkManager.sendToPlayer((ServerPlayerEntity) player, payload);
			net.Gabou.identity2.Identity2.LOGGER.info("Packet Sent! (to owner)");
		}
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}

    private static <T extends net.minecraft.network.packet.CustomPayload> void sendToTrackingPlayers(Entity target, T payload) {
        if (target.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity tracking : serverWorld.getPlayers()) {
                if (tracking != target) {
                    NetworkManager.sendToPlayer(tracking, payload);
                    net.Gabou.identity2.Identity2.LOGGER.info("Packet Sent!");
                }
            }
        }
    }

	private static int executeReset(CommandSource source, Entity target) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		while(((NbtComponentAccessor)(Object)n).getNbt().getKeys().size()!=0){
            ((NbtComponentAccessor)(Object)n).getNbt().remove((String)((NbtComponentAccessor)(Object)n).getNbt().getKeys().toArray()[0]);
        }
		
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
}
