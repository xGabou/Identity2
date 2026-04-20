package ember.qualitycommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
//import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.component.type.NbtComponent;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import ember.qualitycommands.packets.CustomEntityDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityNBTDataS2CPacketPayload;

import java.util.List;
import java.util.Optional;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.world.ServerWorld;

import ember.qualitycommands.packets.CustomEntityStringDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
public class ModifyCustomEntityDataCommand {
	private static final SimpleCommandExceptionType INVULNERABLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.damage.invulnerable"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("custom_attribute")
				.requires(source -> source.hasPermissionLevel(2))
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
                            //climboneverything
						).then(
							CommandManager.literal("climboneverything").then(
							CommandManager.argument("value", FloatArgumentType.floatArg())
						    
                                        .executes(
                                            context -> execute( 
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"climboneverything",
                                                FloatArgumentType.getFloat(context,"value")
                                            )
                                        )
							)
                            //climboneverything
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








						.then(
							CommandManager.literal("model_extra_parts_add_part").then(
							CommandManager.argument("part", StringArgumentType.string()).then(
							CommandManager.argument("model", StringArgumentType.greedyString())
						    
                                        .executes(
                                            (context) -> {
												String part=StringArgumentType.getString(context,"part");
												String model=StringArgumentType.getString(context,"model");
												NbtCompound current=((NbtComponentAccessor)(Object)((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCustomData()).getNbt().getCompoundOrEmpty("model_extra_parts");
												if(current.getString(part,"")==""){
													current.putString(part,model);
												}else{
													if(current.getString(part, "").contains(model)){
														return 1;
													}else{
														current.putString(part,current.getString(part, "")+","+model);
													}
													
												}
												return executeNbt(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"model_extra_parts",
                                                current
                                            );
										}
                                        )
									)
							)
                            
						)
						.then(
							CommandManager.literal("model_extra_parts_remove_part").then(
							CommandManager.argument("part", StringArgumentType.string()).then(
							CommandManager.argument("model", StringArgumentType.greedyString())
						    
                                        .executes(
                                            (context) -> {
												String part=StringArgumentType.getString(context,"part");
												String model=StringArgumentType.getString(context,"model");
												NbtCompound current=((NbtComponentAccessor)(Object)((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCustomData()).getNbt().getCompoundOrEmpty("model_extra_parts");
												if(current.getString(part,"invalid")!="invalid"){
													String newvalue="";
													for(String value:current.getString(part,"").split(",")){
														if(value.matches(model)==false){
														if(newvalue.length()==0){
															newvalue=value;
														}else{
															newvalue=newvalue+","+value;
														}
														}
												}
													if(newvalue==""){
														current.remove(part);
													}else{
														current.putString(part,newvalue);
													}
												}else{
													return 1;
												}
												return executeNbt(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"model_extra_parts",
                                                current
                                            );
										}
                                        )
									)
							)
                            
						)




























						.then(
							CommandManager.literal("model_overlays_remove_overlay").then(
							CommandManager.argument("texture", StringArgumentType.string())
						    
                                        .executes(
                                            (context) -> {
												String texture=StringArgumentType.getString(context,"texture");
												Optional<String> current=((NbtComponentAccessor)(Object)((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCustomData()).getNbt().getString("overlays");
												if(current.isPresent()){
													String newvalue="";
													for(String value:current.orElse("").split(",")){
														if(value.matches(texture)==false){
														if(newvalue==""){
															newvalue=value;
														}else{
															newvalue=newvalue+","+value;
														}
														}
												}
													if(newvalue==""){
														current=Optional.empty();
													}else{
														current=Optional.of(newvalue);
													}
												}else{
													return 1;
												}
												return executeString(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"overlays",
                                                current.orElse("")
                                            );
										}
                                        )
									)
							
                            
						).then(
							CommandManager.literal("model_overlays_add_overlay").then(
							CommandManager.argument("texture", StringArgumentType.string())
						    
                                        .executes(
                                            (context) -> {
												String texture=StringArgumentType.getString(context,"texture");
												Optional<String> current=((NbtComponentAccessor)(Object)((EntityAccessor)EntityArgumentType.getEntity(context, "target")).getCustomData()).getNbt().getString("overlays");
												if(current.isEmpty()){
													current=Optional.of(texture);
												}else{
													if(current.orElse("").contains(texture)){
														return 1;
													}else{
														current=Optional.of(current.orElse("")+","+texture);
													}
													
												}
												return executeString(
                                                context.getSource(),
                                                EntityArgumentType.getEntity(context, "target"),
												"overlays",
                                                current.orElse("")
                                            );
										}
                                        )
									)
							
                            
						)




				)
		);
	}

	private static int execute(CommandSource source, Entity target,String targetS, float amount) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		((NbtComponentAccessor)(Object)n).getNbt().putDouble(targetS,amount);
		for (ServerPlayerEntity player : PlayerLookup.tracking(target)) {
			ServerPlayNetworking.send(player, new CustomEntityDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.Entry(targetS,amount))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent!");
        }
		if(target instanceof PlayerEntity player){
			ServerPlayNetworking.send((ServerPlayerEntity)player, new CustomEntityDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.Entry(targetS,amount))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent! (to owner)");
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
		for (ServerPlayerEntity player : PlayerLookup.tracking(target)) {
			ServerPlayNetworking.send(player, new CustomEntityStringDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryString(targetS,amount))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent!");
        }
		if(target instanceof PlayerEntity player){
			ServerPlayNetworking.send((ServerPlayerEntity)player, new CustomEntityStringDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryString(targetS,amount))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent! (to owner)");
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
		for (ServerPlayerEntity player : PlayerLookup.tracking(target)) {
			ServerPlayNetworking.send(player, new CustomEntityBoolDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryBool(targetS,z))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent!");
        }
		if(target instanceof PlayerEntity player){
			ServerPlayNetworking.send((ServerPlayerEntity)player, new CustomEntityBoolDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryBool(targetS,z))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent! (to owner)");
		}
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
	}
	private static int executeNbt(CommandSource source, Entity target,String targetS,NbtCompound z) throws CommandSyntaxException {
		NbtComponent n=((EntityAccessor)target).getCustomData();
		((NbtComponentAccessor)(Object)n).getNbt().put(targetS,z);
		for (ServerPlayerEntity player : PlayerLookup.tracking(target)) {
			ServerPlayNetworking.send(player, new CustomEntityNBTDataS2CPacketPayload(target.getId(),
				List.of(
					new CustomEntityDataS2CPacket.EntryNBT(targetS,z))
				)
			);
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent!");
        }
		if(target instanceof PlayerEntity player){
			ServerPlayNetworking.send((ServerPlayerEntity)player, new CustomEntityNBTDataS2CPacketPayload(target.getId(),List.of(new CustomEntityDataS2CPacket.EntryNBT(targetS,z))));
			ember.qualitycommands.QualityCommands.LOGGER.info("Packet Sent! (to owner)");
		}
		//if (target.damage(source.getWorld(), damageSource, amount)) {
			//source.sendFeedback(() -> Text.translatable("commands.damage.success", amount, target.getDisplayName()), true);
			//return 1;
		//} else {
		//	throw INVULNERABLE_EXCEPTION.create();
		//}
		return 1;
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
