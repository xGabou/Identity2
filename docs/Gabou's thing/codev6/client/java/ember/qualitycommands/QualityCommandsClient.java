package ember.qualitycommands;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import com.mojang.blaze3d.systems.RenderSystem;

import ember.qualitycommands.featurerenderer.ExtraModelFeatureRenderer;
import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import ember.qualitycommands.packets.CustomEntityDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityNBTDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityStringDataS2CPacketPayload;
import ember.qualitycommands.packets.IdentityAbilityPacketPayload;
import ember.qualitycommands.packets.ParticleWithBaseVelPacketPayload;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.IdentityAbilityDefinition;
import ember.qualitycommands.util.MinecraftClientAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import ember.qualitycommands.util.RegistriesAccessor;
//import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.mixin.resource.conditions.RegistryOpsAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.SerializableRegistries.SerializedRegistryEntry;
import net.minecraft.registry.entry.RegistryEntry;
/*import ember.qualitycommands.blocks.AbstractColoredRedstoneWireBlock;
import ember.qualitycommands.blocks.RedRedstoneWireBlock;
import ember.qualitycommands.blocks.GreenRedstoneWireBlock;
import ember.qualitycommands.blocks.BlueRedstoneWireBlock;*/
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class QualityCommandsClient implements ClientModInitializer{
	
    /*public void onUpdateCustomData(CustomEntityDataS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler)(Object)entity, ((ClientPlayNetworkHandler)(Object)entity).client.getPacketApplyBatcher());
		Entity entity = entity.world.getEntityById(packet.getEntityId());
		if (entity != null) {
            NbtComponent n=((EntityAccessor)entity).getCustomData();
            for (ember.qualitycommands.packets.CustomEntityDataS2CPacket.Entry entry : packet.getEntries()) {
                ((NbtComponentAccessor)(Object)n).getNbt().putDouble(entry.key(),entry.value());
            }
		}
	}*/
    public void onUpdateCustomData(CustomEntityDataS2CPacketPayload packet) {
		Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityid());
		if (entity != null) {
            NbtComponent n=((EntityAccessor)entity).getCustomData();
            for (ember.qualitycommands.packets.CustomEntityDataS2CPacket.Entry entry : packet.entries()) {
                ((NbtComponentAccessor)(Object)n).getNbt().putDouble(entry.key(),entry.value());
            }
		}else{
            QualityCommands.LOGGER.info("Entity null.");
        }
	}
    public void onUpdateCustomData(CustomEntityStringDataS2CPacketPayload packet) {
		Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityid());
		if (entity != null) {
            NbtComponent n=((EntityAccessor)entity).getCustomData();
            for (ember.qualitycommands.packets.CustomEntityDataS2CPacket.EntryString entry : packet.entries()) {
                ((NbtComponentAccessor)(Object)n).getNbt().putString(entry.key(),entry.value());
                if(entry.key().matches("model_override")){
                    ((EntityAccessor)entity).setCurrentIdentity(entry.value());
                }
            }
            
		}else{
            QualityCommands.LOGGER.info("Entity null.");
        }
	}
	public void onUpdateCustomData(CustomEntityBoolDataS2CPacketPayload packet) {
		Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityid());
		if (entity != null) {
            NbtComponent n=((EntityAccessor)entity).getCustomData();
            for (ember.qualitycommands.packets.CustomEntityDataS2CPacket.EntryBool entry : packet.entries()) {
                ((NbtComponentAccessor)(Object)n).getNbt().putBoolean(entry.key(),entry.value());
                
            }
            
		}else{
            QualityCommands.LOGGER.info("Entity null.");
        }
	}
    public void onUpdateCustomData(CustomEntityNBTDataS2CPacketPayload packet) {
		Entity entity = MinecraftClient.getInstance().world.getEntityById(packet.entityid());
		if (entity != null) {
            NbtComponent n=((EntityAccessor)entity).getCustomData();
            for (ember.qualitycommands.packets.CustomEntityDataS2CPacket.EntryNBT entry : packet.entries()) {
                ((NbtComponentAccessor)(Object)n).getNbt().put(entry.key(),entry.value());
                
            }
            
		}else{
            QualityCommands.LOGGER.info("Entity null.");
        }
	}
    Random random=new Random();
	public void onParticle(ParticleWithBaseVelPacketPayload packet) {
		//NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
        ClientWorld world=MinecraftClient.getInstance().world;
		if (packet.count() == 0) {
			double d = packet.speed() * packet.offsetX();
			double e = packet.speed() * packet.offsetY();
			double f = packet.speed() * packet.offsetZ();

			try {
				world.addParticleClient(packet.parameters(), packet.forceSpawn(), packet.important(), packet.x(), packet.y(), packet.z(), d, e, f);
			} catch (Throwable var17) {
				QualityCommands.LOGGER.warn("Could not spawn particle effect {}", packet.parameters());
			}
		} else {
            /*QualityCommands.LOGGER.info(

                "Pos: <"+String.valueOf(packet.x())+", "+String.valueOf(packet.y())+", "+String.valueOf(packet.z())+">"+
                "Delta: <"+String.valueOf(packet.offsetX())+", "+String.valueOf(packet.offsetY())+", "+String.valueOf(packet.offsetZ())+">"+
                "Base Vel: <"+String.valueOf(packet.basevelx())+", "+String.valueOf(packet.basevely())+", "+String.valueOf(packet.basevelz())+">"

            );*/
			for (int i = 0; i < packet.count(); i++) {
				double g = this.random.nextGaussian() * packet.offsetX();
				double h = this.random.nextGaussian() * packet.offsetY();
				double j = this.random.nextGaussian() * packet.offsetZ();
				double k = this.random.nextGaussian() * packet.speed()          +packet.basevelx()-packet.x();
				double l = this.random.nextGaussian() * packet.speed()          +packet.basevely()-packet.y();
				double m = this.random.nextGaussian() * packet.speed()          +packet.basevelz()-packet.z();

				try {
					world
						.addParticleClient(
							packet.parameters(), packet.forceSpawn(), packet.important(), packet.x() + g, packet.y() + h, packet.z() + j, k, l, m
						);
				} catch (Throwable var16) {
					QualityCommands.LOGGER.warn("Could not spawn particle effect {}", packet.parameters());
					return;
				}
			}
		}
	}
    public static ArrayList<BiFunction<Entity,Entity,Entity>> visualPatchValues=new ArrayList(0);
    public static ArrayList<Identifier> visualPatchKeys=new ArrayList(0);
    public static void addVisualPatch(BiFunction<Entity,Entity,Entity> value,Identifier id){
        visualPatchKeys.ensureCapacity(visualPatchKeys.size()+1);
        visualPatchValues.ensureCapacity(visualPatchValues.size()+1);
        visualPatchKeys.add(id);
        visualPatchValues.add(value);
    }
    static{
	addVisualPatch((identity,entity)->{
		if(identity instanceof EnderDragonEntity dragonIdentity){
                dragonIdentity.yawAcceleration+=MathHelper.wrapDegrees(entity.getYaw()-identity.getYaw())*0.1F;
            }
			return identity;
	},Identifier.of("minecraft:ender_dragon"));
	/*
	
    addVisualPatch((e)->{
            EnderDragonEntity entity=(EnderDragonEntity)e;
            ((EntityAccessor)entity).runAddAirTravelEffects();
		if (entity.getEntityWorld().isClient()) {
			entity.setHealth(entity.getHealth());
			if (!entity.isSilent() && !entity.getPhaseManager().getCurrent().isSittingOrHovering() && ((EnderDragonEntityAccessor)entity).setTicksUntilNextGrowl(((EnderDragonEntityAccessor)entity).getTicksUntilNextGrowl()-1) < 0) {
				entity.getEntityWorld()
					.playSoundClient(
						entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, entity.getSoundCategory(), 2.5F, 0.8F + entity.getRandom().nextFloat() * 0.3F, false
					);
				((EnderDragonEntityAccessor)entity).setTicksUntilNextGrowl(200 + entity.getRandom().nextInt(200));
			}
		}

		/*if (entity.fight == null && entity.getEntityWorld() instanceof ServerWorld serverWorld) {
			EnderDragonFight enderDragonFight = serverWorld.getEnderDragonFight();
			if (enderDragonFight != null && entity.getUuid().equals(enderDragonFight.getDragonUuid())) {
				entity.fight = enderDragonFight;
			}
		}*v/

		entity.lastWingPosition = entity.wingPosition;
		if (entity.isDead()) {
			float f = (entity.getRandom().nextFloat() - 0.5F) * 8.0F;
			float g = (entity.getRandom().nextFloat() - 0.5F) * 4.0F;
			float h = (entity.getRandom().nextFloat() - 0.5F) * 8.0F;
			entity.getEntityWorld().addParticleClient(ParticleTypes.EXPLOSION, entity.getX() + f, entity.getY() + 2.0 + g, entity.getZ() + h, 0.0, 0.0, 0.0);
		} else {
			((EnderDragonEntityAccessor)entity).runTickWithEndCrystals();
			Vec3d vec3d = entity.getVelocity();
			float g = 0.2F / ((float)vec3d.horizontalLength() * 10.0F + 1.0F);
			g *= (float)Math.pow(2.0, vec3d.y);
			if (entity.getPhaseManager().getCurrent().isSittingOrHovering()) {
				entity.wingPosition += 0.1F;
			} else if (entity.slowedDownByBlock) {
				entity.wingPosition += g * 0.5F;
			} else {
				entity.wingPosition += g;
			}

			entity.setYaw(MathHelper.wrapDegrees(entity.getYaw()));
        }



            this.bodyYaw = this.getYaw();
				Vec3d[] vec3ds = new Vec3d[this.parts.length];

				for (int q = 0; q < this.parts.length; q++) {
					vec3ds[q] = new Vec3d(this.parts[q].getX(), this.parts[q].getY(), this.parts[q].getZ());
				}

				float r = (float)(this.frameTracker.getFrame(5).y() - this.frameTracker.getFrame(10).y()) * 10.0F * (float) (Math.PI / 180.0);
				float s = MathHelper.cos(r);
				float t = MathHelper.sin(r);
				float u = this.getYaw() * (float) (Math.PI / 180.0);
				float v = MathHelper.sin(u);
				float w = MathHelper.cos(u);
				this.movePart(this.body, v * 0.5F, 0.0, -w * 0.5F);
				this.movePart(this.rightWing, w * 4.5F, 2.0, v * 4.5F);
				this.movePart(this.leftWing, w * -4.5F, 2.0, v * -4.5F);
				float x = MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0) - this.yawAcceleration * 0.01F);
				float y = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0) - this.yawAcceleration * 0.01F);
				float z = this.getHeadVerticalMovement();
				this.movePart(this.head, x * 6.5F * s, z + t * 6.5F, -y * 6.5F * s);
				this.movePart(this.neck, x * 5.5F * s, z + t * 5.5F, -y * 5.5F * s);
				EnderDragonFrameTracker.Frame frame = this.frameTracker.getFrame(5);

				for (int aa = 0; aa < 3; aa++) {
					EnderDragonPart enderDragonPart = null;
					if (aa == 0) {
						enderDragonPart = this.tail1;
					}

					if (aa == 1) {
						enderDragonPart = this.tail2;
					}

					if (aa == 2) {
						enderDragonPart = this.tail3;
					}

					EnderDragonFrameTracker.Frame frame2 = this.frameTracker.getFrame(12 + aa * 2);
					float ab = this.getYaw() * (float) (Math.PI / 180.0) + this.wrapYawChange(frame2.yRot() - frame.yRot()) * (float) (Math.PI / 180.0);
					float ac = MathHelper.sin(ab);
					float mx = MathHelper.cos(ab);
					float n = 1.5F;
					float o = (aa + 1) * 2.0F;
					this.movePart(enderDragonPart, -(v * 1.5F + ac * o) * s, frame2.y() - frame.y() - (o + 1.5F) * t + 1.5, (w * 1.5F + mx * o) * s);
				}


				for (int aa = 0; aa < this.parts.length; aa++) {
					this.parts[aa].lastX = vec3ds[aa].x;
					this.parts[aa].lastY = vec3ds[aa].y;
					this.parts[aa].lastZ = vec3ds[aa].z;
					this.parts[aa].lastRenderX = vec3ds[aa].x;
					this.parts[aa].lastRenderY = vec3ds[aa].y;
					this.parts[aa].lastRenderZ = vec3ds[aa].z;
				}


            return entity;
        },
        Identifier.of("minecraft:ender_dragon"));*/
    }
    // In your client-only initializer method
    
    /*static float dashSpeed=1;
    public static float getDashSpeed(){
        return QualityCommandsClient.dashSpeed;
    }
    public static void setDashSpeed(float value){
        QualityCommandsClient.dashSpeed=value;
    }
    private static KeyBinding keyBindingA=KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.qualitycommands.dash",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_O,
        "category.qualitycommands.test"
    ));
    private static KeyBinding keyBindingB=KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.qualitycommands.dashplus",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_C,
        "category.qualitycommands.test"
    ));
    */
    private static KeyBinding keyBindingC=KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.qualitycommands.dashminus",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_V,
        KeyBinding.Category.create(Identifier.of("category.qualitycommands.test"))
    ));
    @Override
    public void onInitializeClient(){


        for(RegistryLoader.Entry entry:DynamicRegistries.getDynamicRegistries()){
            QualityCommands.LOGGER.info("Client Dynamic registry at: "+entry.key().getRegistry()+"/"+entry.key().getValue());
        }
        for(RegistryKey entry:net.minecraft.registry.Registries.REGISTRIES.getKeys()){
            QualityCommands.LOGGER.info("Client registry at: "+entry.getRegistry()+"/"+entry.getValue());
        }
        for(RegistryKey entry:net.minecraft.registry.BuiltinRegistries.createWrapperLookup().streamAllRegistryKeys().toList()){
            QualityCommands.LOGGER.info("??? registry at: "+entry.getRegistry()+"/"+entry.getValue());
        }

        ClientPlayNetworking.registerGlobalReceiver(ParticleWithBaseVelPacketPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				onParticle(payload);
                QualityCommands.LOGGER.info("Packet Recieved!");
			});
		});
        ClientPlayNetworking.registerGlobalReceiver(CustomEntityDataS2CPacketPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				onUpdateCustomData(payload);
                QualityCommands.LOGGER.info("Packet Recieved!");
			});
		});
        ClientPlayNetworking.registerGlobalReceiver(CustomEntityStringDataS2CPacketPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				onUpdateCustomData(payload);
                QualityCommands.LOGGER.info("Packet Recieved!");
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(CustomEntityBoolDataS2CPacketPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				onUpdateCustomData(payload);
                QualityCommands.LOGGER.info("Packet Recieved!");
			});
		});
        ClientPlayNetworking.registerGlobalReceiver(CustomEntityNBTDataS2CPacketPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				onUpdateCustomData(payload);
                QualityCommands.LOGGER.info("Packet Recieved!");
			});
		});

        /*ColorProviderRegistry.BLOCK.register(
			(state, world, pos, tintIndex) -> RedRedstoneWireBlock.getWireColor((Integer)state.get(RedstoneWireBlock.POWER)), ModBlocks.REDSTONE_WIRE_COLORED.get(0)
		);
        ColorProviderRegistry.BLOCK.register(
			(state, world, pos, tintIndex) -> GreenRedstoneWireBlock.getWireColor((Integer)state.get(RedstoneWireBlock.POWER)), ModBlocks.REDSTONE_WIRE_COLORED.get(1)
		);
        ColorProviderRegistry.BLOCK.register(
			(state, world, pos, tintIndex) -> BlueRedstoneWireBlock.getWireColor((Integer)state.get(RedstoneWireBlock.POWER)), ModBlocks.REDSTONE_WIRE_COLORED.get(2)
		);
        // To make some parts of the block transparent (like glass, saplings and doors):
        BlockRenderLayerMap.putBlock(ModBlocks.REDSTONE_WIRE_COLORED.get(0), BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.REDSTONE_WIRE_COLORED.get(1), BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.REDSTONE_WIRE_COLORED.get(2), BlockRenderLayer.CUTOUT);*/
 
        // To make some parts of the block translucent (like ice, stained glass and portal)
        //BlockRenderLayerMap.putBlock(TutorialBlocks.MY_BLOCK, BlockRenderLayer.TRANSLUCENT);
        /*ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBindingA.wasPressed()){
                //client.player.sendMessage(Text.literal("Key was pressed"),false);
                PacketByteBuf buf=PacketByteBufs.create();
                buf.writeString("execute positioned 0 0 0 positioned ^ ^ ^%f run setVel @s ~-0.5 ~0.0 ~-0.5".formatted((QualityCommandsClient.getDashSpeed())));
                ClientPlayNetworking.send(CommandTriggerPacket.PACKET_ID, buf);
            }//execute positioned 0 0 0 positioned ^ ^ ^1 run accelerate @s ~-0.5 ~0.0 ~-0.5
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBindingB.wasPressed()){
                setDashSpeed(getDashSpeed()-0.1F);
                client.player.sendMessage(Text.literal("Dash Strength %f".formatted(QualityCommandsClient.getDashSpeed())),false);
            }//execute positioned 0 0 0 positioned ^ ^ ^1 run accelerate @s ~-0.5 ~0.0 ~-0.5
        });\
        
        */ClientTickEvents.END_CLIENT_TICK.register(client -> {
            int usedAbility=0;
            if (keyBindingC.isPressed()){
                
            ClientPlayerEntity player = client.player;
            Window window = client.getWindow();
            Entity identity = ((EntityAccessor)player).getCurrentIdentity();

            if(identity == null) {
                return;
            }
//TODO make this tick less often
            /*net.minecraft.registry.Registry<IdentityAbilityDefinition> identityAbilityRegistry=ModRegistries.identityAbilityRegistry;
            if(identityAbilityRegistry==null){
                QualityCommands.LOGGER.info("Identity Ability Registry missing!");
                return;
            }*/
            
            if(ModRegistries.identityAbilityRegistry==null){
                QualityCommands.LOGGER.info("Identity Ability Registry missing!");
                return;
            }
            IdentityAbilityDefinition identityAbility = ModRegistries.identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(identity.getType()));
            if(identityAbility!=null){
                if(((EntityAccessor)player).getAbilityCooldown()==0){
                ((EntityAccessor)player).setAbilityCooldown(identityAbility.cooldown()+identityAbility.useduration());
                ClientPlayNetworking.send(new IdentityAbilityPacketPayload(0));
                usedAbility=1;
                }
            }else{
                QualityCommands.LOGGER.info("No Identity Ability");
            }
            }//execute positioned 0 0 0 positioned ^ ^ ^1 run accelerate @s ~-0.5 ~0.0 ~-0.5
            if(client.player!=null){
                if(((EntityAccessor)client.player).getCurrentIdentity()!=null){
                    
                    IdentityAbilityDefinition identityAbility = ModRegistries.identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(((EntityAccessor)client.player).getCurrentIdentity().getType()));
                    if(identityAbility!=null){
                    int cd=((EntityAccessor)client.player).getAbilityCooldown();
                    //QualityCommands.LOGGER.info(String.valueOf(cd));
                    if(cd>identityAbility.cooldown()){
                        QualityCommands.LOGGER.info("Trying to run ability tick");
                        ClientPlayNetworking.send(new IdentityAbilityPacketPayload(identityAbility.cooldown()+identityAbility.useduration()-cd+1));
                    }

                    ClientPlayNetworking.send(new IdentityAbilityPacketPayload(-1-usedAbility));
                    }
                }
            }


        });
    net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(QualityCommands.MOD_ID, "before_chat"), QualityCommandsClient::renderIdentityCooldown);
    LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper,context) -> {
        if (entityType ==EntityType.PLAYER) {
                registrationHelper.register(new ExtraModelFeatureRenderer(entityRenderer));
        }
     });
	}
    private static final int fadingTickRequirement = 0;
    private static int lastCooldown = 0;
    private static int ticksSinceUpdate = 0;
    private static boolean isFading = false;
    private static int fadingProgress = 0;
	private static void renderIdentityCooldown(net.minecraft.client.gui.DrawContext matrices, net.minecraft.client.render.RenderTickCounter deltax) {
        float delta=deltax.getTickProgress(false);
         MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            Window window = client.getWindow();
            Entity identity = ((EntityAccessor)player).getCurrentIdentity();

            if(identity == null) {
                return;
            }
//TODO make this tick less often
            if(ModRegistries.identityAbilityRegistry==null){
                ModRegistries.identityAbilityRegistry=(Registry)net.minecraft.registry.BuiltinRegistries.createWrapperLookup().getOptional(ModRegistries.IDENTITY_ABILITY_KEY).orElse(null);
            }
            if(ModRegistries.identityAbilityRegistry==null){
                return;
            }
            IdentityAbilityDefinition identityAbility = ModRegistries.identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(identity.getType()));

            if(identityAbility == null) {
                return;
            }

            if(client.currentScreen instanceof ChatScreen) {
                return;
            }

            double d = client.getWindow().getScaleFactor();
            int cd = ((EntityAccessor)(player)).getAbilityCooldown();
            float lerpedCooldown = MathHelper.lerp(delta, cd - 1, cd);
            int max = identityAbility.cooldown()+identityAbility.useduration();
            float cooldownScale = 1 - cd / (float) max;

            // CD has NOT updated since last tick. It is most likely full.
            if(cd == lastCooldown) {
                ticksSinceUpdate++;

                // If the CD has not updated, we are above the requirement, and we are not fading, start fading.
                if(ticksSinceUpdate > fadingTickRequirement && !isFading) {
                    isFading = true;
                    fadingProgress = 0;
                }
            }

            // CD updated in the last tick, and we are fading. Stop fading.
            else if(ticksSinceUpdate > fadingProgress) {
                ticksSinceUpdate = 0;
                isFading = false;
            }

            // Tick fading
            if(isFading) {
                fadingProgress = Math.min(50, fadingProgress + 1);
            } else {
                fadingProgress = Math.max(0, fadingProgress - 1);
            }

            if(player != null) {
                int start = (int) (window.getWidth() / d * .804);
                int end = (int) (window.getWidth() / d * .948);
                
                int diff = end - start;
                

//                DrawableHelper.fill(
//                        matrices,
//                        (int) (window.getWidth() / d * .8),
//                        (int) (window.getHeight() / d * .93),
//                        (int) (window.getWidth() / d * .95),
//                        (int) (window.getHeight() / d * .97),
//                        -1);

                int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
                int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
                
                int iconwidth=17;
                int top = 245;
                matrices.getMatrices().pushMatrix();
                if(cooldownScale != 1) {
                    /*matrices.enableScissor(
                            (int) ((double) 0 * d),
                            (int) ((double) 0 * d),
                            (int) ((double) width * d),
                            (int) ((double) height * .92 + iconwidth * cooldownScale)); // min is 0.92, max is 0.. dif = .55*/
                    matrices.enableScissor(
                            (int) ((double) 0*d),
                            (int) ((double) height * .92 + iconwidth * (1-cooldownScale)),
                            (int) ((double) width * d),
                            (int) ((double) height * d)); // min is 0.92, max is 0.. dif = .55
                }

                // ending pop
                if((isFading)&&(cooldownScale==1)) {
                    float fadeScalar = fadingProgress / 50f; // 0f -> 1f, 0 is start, 1 is end
                    float scale = 1f + (float) Math.sin(fadeScalar * 1.5 * Math.PI) - .25f;
                    scale = Math.max(scale, 0.01F);
                    
                    matrices.getMatrices().scaleAround(scale, (int) (width * .95f+iconwidth*.5f), (int) (height * .92f+iconwidth*.5f));
                }

                // TODO: cache ability stack?
//                MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(new ItemStack(identityAbility.getIcon()), (int) (width * .95f), (int) (height * .92f));
                ItemStack stack = new ItemStack(identityAbility.icon());
//                BakedModel heldItemModel = MinecraftClient.getInstance().getItemRenderer().getHeldItemModel(stack, client.world, player);
//                renderGuiItemModel(matrices, stack, (int) (width * .95f), (int) (height * .92f), heldItemModel);
                matrices.drawItem(stack, (int) (width * .95f), (int) (height * .92f));
                if(cooldownScale != 1) {
                matrices.disableScissor();
                }
                
                matrices.getMatrices().popMatrix();
            }
    }
	

	private static java.lang.reflect.Field getFieldFromClassHeirarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true); // Strip Java access checks
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass(); // Move up the hierarchy
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }
	public static EntityModel getModel(Entity e){
		EntityRenderer idrenderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(e);
        
        EntityModel eModel=null;
        if(idrenderer instanceof LivingEntityRenderer){
            try{
            eModel=((LivingEntityRenderer)idrenderer).getModel();
            }catch(Exception f){
				try{
                eModel=(EntityModel)getFieldFromClassHeirarchy(eModel.getClass(),"model").get((Object)eModel);
				}catch(Exception g){
					int x=0;
				}
            }
        }
        if(idrenderer instanceof EnderDragonEntityRenderer){
            eModel=((ember.qualitycommands.util.EnderDragonEntityRendererAccessor)(EnderDragonEntityRenderer)idrenderer).getModel();
        }
		return eModel;
	}



}