package ember.qualitycommands;

import net.minecraft.entity.EntityType;
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
import ember.qualitycommands.commands.WithCommand;
import ember.qualitycommands.commands.AirCommand;
import ember.qualitycommands.commands.ModifyCustomEntityDataCommand;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import ember.qualitycommands.packets.CustomEntityDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityStringDataS2CPacketPayload;
import ember.qualitycommands.packets.IdentityAbilityPacketPayload;
import ember.qualitycommands.packets.ParticleWithBaseVelPacketPayload;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import ember.qualitycommands.packets.CustomEntityNBTDataS2CPacketPayload;
public class ModPackets{
	public static void initialize(){
		PayloadTypeRegistry.playS2C().register(CustomEntityDataS2CPacketPayload.ID, CustomEntityDataS2CPacketPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CustomEntityStringDataS2CPacketPayload.ID, CustomEntityStringDataS2CPacketPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CustomEntityBoolDataS2CPacketPayload.ID, CustomEntityBoolDataS2CPacketPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CustomEntityNBTDataS2CPacketPayload.ID, CustomEntityNBTDataS2CPacketPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ParticleWithBaseVelPacketPayload.ID, ParticleWithBaseVelPacketPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(IdentityAbilityPacketPayload.ID, IdentityAbilityPacketPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(IdentityAbilityPacketPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ResourceLocation prebuilt=ModRegistries.identityAbilityRegistry.get(EntityType.getId((((EntityAccessor)context.player()).getCurrentIdentity()).getType())).bultinability();
				
				if(payload.entityid()==0){
					String cmd=ModRegistries.identityAbilityRegistry.get(EntityType.getId((((EntityAccessor)context.player()).getCurrentIdentity()).getType())).command();
					if(cmd!=""){
						context.server().getCommandManager().parseAndExecute(
							context.server().getCommandSource().withEntity(context.player()),
							cmd
						);
					}
					if(prebuilt.getPath().matches("null")==false){
						QualityCommands.LOGGER.info("Running ability "+prebuilt.toString());
						PredefIdentityAbilities.predef.get(prebuilt).execute(context.player());
					}
				}else if(prebuilt.getPath().matches("null")==false){
					if((payload.entityid()==-1)||(payload.entityid()==-2)){

						PredefIdentityAbilities.predef.get(prebuilt).passivetick(context.player(),payload.entityid()==-2);
					}else if((payload.entityid()==-3)){
						QualityCommands.LOGGER.info("using ability attack");
						PredefIdentityAbilities.predef.get(prebuilt).overrideAttack(context.player());
					}else{
					QualityCommands.LOGGER.info("Ticking ability "+String.valueOf(payload.entityid()));
						PredefIdentityAbilities.predef.get(prebuilt).tick(context.player(),payload.entityid());
					}
					}
			});
		});
	}
	//public static final PacketType<CustomEntityDataS2CPacket> SET_ENTITY_DOUBLE_DATA = s2c("set_custom_data_double");

	private static <T extends Packet<ClientPlayPacketListener>> PacketType<T> s2c(String id) {
		return new PacketType<>(NetworkSide.CLIENTBOUND, ResourceLocation.of(QualityCommands.MOD_ID,id));
	}

	private static <T extends Packet<ServerPlayPacketListener>> PacketType<T> c2s(String id) {
		return new PacketType<>(NetworkSide.SERVERBOUND, ResourceLocation.of(QualityCommands.MOD_ID,id));
	}

	
	public static final ResourceLocation CUSTOM_STRING_DATA_ID=ResourceLocation.of(QualityCommands.MOD_ID,"set_custom_data_string");
	public static final ResourceLocation CUSTOM_DOUBLE_DATA_ID=ResourceLocation.of(QualityCommands.MOD_ID,"set_custom_data_double");
	public static final ResourceLocation CUSTOM_BOOL_DATA_ID=ResourceLocation.of(QualityCommands.MOD_ID,"set_custom_data_bool");
	public static final ResourceLocation CUSTOM_NBT_DATA_ID=ResourceLocation.of(QualityCommands.MOD_ID,"set_custom_data_nbt");
	public static final ResourceLocation IDENTITY_ABILITY_PACKET_ID=ResourceLocation.of(QualityCommands.MOD_ID,"entity_ability");
	public static final ResourceLocation PARTICLE_BVEL_PACKET=ResourceLocation.of(QualityCommands.MOD_ID,"particle_bvel");
	
	
}