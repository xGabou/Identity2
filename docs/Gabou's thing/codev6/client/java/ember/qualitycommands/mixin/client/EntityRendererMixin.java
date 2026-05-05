package ember.qualitycommands.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.render.Camera;
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
import ember.qualitycommands.QualityCommands;
import ember.qualitycommands.QualityCommandsClient;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.registry.Registries;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.EntityRenderStateModifier;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.MinecraftClient;
import ember.qualitycommands.util.MinecraftClientAccessor;
import ember.qualitycommands.util.LimbAnimatorAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{
    @Inject(method = "getAndUpdateRenderState", at = @At("RETURN"), cancellable = true)
	private void getAndUpdateRenderStateModifier(T entity, float tickProgress,CallbackInfoReturnable info) {
		EntityRenderState entityRenderState=(EntityRenderState)info.getReturnValue();
        if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").isPresent()){
            if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get().length()!=0){
                String d=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get();
                if(d.contains("{")){
                    d=d.substring(0,d.indexOf('{'));
                }
                if(Registries.ENTITY_TYPE.containsId(ResourceLocation.of(d))){
                    if(((EntityAccessor)entity).getCurrentIdentity()!=null){
                //Sync identity to entity
                Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                EntityRenderer renderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(identity);
                EntityRenderer currentRenderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(entity);
                EntityHitboxAndView oldHitbox=entityRenderState.hitbox;
                {
            //living only:
            
            identity.setPos(
                ((Entity)entity).getEntityPos().x,
                ((Entity)entity).getEntityPos().y,
                ((Entity)entity).getEntityPos().z
            );
            if(identity instanceof EnderDragonEntity dragonIdentity){
                identity.setYaw(entity.getYaw()+180);
            }else{
                identity.setYaw(entity.getYaw());
            }
            ((EntityAccessor)identity).setLastPosition(
                ((Entity)entity).getLastRenderPos()
            );
            if((identity instanceof LivingEntity livingIdentity)&&(entity instanceof LivingEntity livingEntity)){
                if(livingIdentity.isJumping()!=livingEntity.isJumping()){
                    livingIdentity.setJumping(livingEntity.isJumping());
                }
                livingIdentity.getAttributes().setFrom(livingEntity.getAttributes());
            LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.limbAnimator;
            LimbAnimatorAccessor source = (LimbAnimatorAccessor) livingEntity.limbAnimator;

            target.setPrevSpeed(source.getPrevSpeed());
            target.setSpeed(source.getSpeed());
            //target.setPos(source.getPos());

            livingIdentity.handSwinging = livingEntity.handSwinging;//LivingEntity only
            livingIdentity.handSwingTicks = livingEntity.handSwingTicks;//living only
            livingIdentity.lastHandSwingProgress = livingEntity.lastHandSwingProgress;//living only
            livingIdentity.handSwingProgress = livingEntity.handSwingProgress;//living only
            if((livingIdentity instanceof net.minecraft.entity.mob.ShulkerEntity)==false){
                livingIdentity.bodyYaw = livingEntity.bodyYaw;//living only
                livingIdentity.lastBodyYaw = livingEntity.lastBodyYaw;//living only
            }
            livingIdentity.headYaw = livingEntity.headYaw;//living only
            livingIdentity.lastHeadYaw = livingEntity.lastHeadYaw;//living only
            livingIdentity.preferredHand = livingEntity.preferredHand;//livingonly
            livingIdentity.setCurrentHand(livingEntity.getActiveHand());//living only
            }
            identity.age = ((Entity)entity).age;//all
            identity.setOnGround(((Entity)entity).isOnGround());//all entities
            identity.setVelocity(((Entity)entity).getVelocity());//all entities
            identity.setSneaking(((Entity)entity).isSneaking());//all entities
            identity.setSprinting(((Entity)entity).isSprinting());//all entities
            identity.setSwimming(((Entity)entity).isSwimming());//all entities
            identity.setPose(((Entity)entity).getPose());//all entities

            ((EntityAccessor) identity).setVehicle(((Entity)entity).getVehicle());
            ((EntityAccessor) identity).setTouchingWater(((Entity)entity).isTouchingWater());

            if (identity instanceof PhantomEntity) {
                identity.setPitch(-((Entity)entity).getPitch());
                identity.lastPitch = -((Entity)entity).lastPitch;//used to be prevPitch
            } else if((identity instanceof net.minecraft.entity.mob.ShulkerEntity)==false){
                identity.setPitch(((Entity)entity).getPitch());
                identity.lastPitch = ((Entity)entity).lastPitch;
            }
            //living only
            if((entity instanceof LivingEntity livingEntity)&&(identity instanceof LivingEntity livingIdentity)){
                //if (IdentityConfig.getInstance().identitiesEquipItems()) {
                    livingIdentity.equipStack(EquipmentSlot.MAINHAND, livingEntity.getEquippedStack(EquipmentSlot.MAINHAND));
                    livingIdentity.equipStack(EquipmentSlot.OFFHAND, livingEntity.getEquippedStack(EquipmentSlot.OFFHAND));
                //}

                //if (IdentityConfig.getInstance().identitiesEquipArmor()) {
                    livingIdentity.equipStack(EquipmentSlot.HEAD, livingEntity.getEquippedStack(EquipmentSlot.HEAD));
                    livingIdentity.equipStack(EquipmentSlot.CHEST, livingEntity.getEquippedStack(EquipmentSlot.CHEST));
                    livingIdentity.equipStack(EquipmentSlot.LEGS, livingEntity.getEquippedStack(EquipmentSlot.LEGS));
                    livingIdentity.equipStack(EquipmentSlot.FEET, livingEntity.getEquippedStack(EquipmentSlot.FEET));
                //}
            }

            if(entity instanceof LivingEntity){
                if (identity instanceof MobEntity) {
                    ((MobEntity) identity).setAttacking(((LivingEntity)entity).isUsingItem());
                }
            }

            /*identity.setPose(entity.getPose());

            identity.setCurrentHand(entity.getActiveHand() == null ? Hand.MAIN_HAND : entity.getActiveHand());
            ((LivingEntityCompatAccessor) identity).callSetLivingFlag(1, entity.isUsingItem());
            identity.getItemUseTime();
            ((LivingEntityCompatAccessor) identity).callTickActiveItemStack();*/

            /*EntityUpdater updater = EntityUpdaters.getUpdater((EntityType<? extends LivingEntity>) identity.getType());
            if (updater != null) {
                updater.update(player, identity);
            }*/


                identity.setOnFire(entity.isOnFire());
                }
                EntityRenderState oldState=entityRenderState;
                entityRenderState=renderer.createRenderState();
                //entityRenderState.onFire=oldState.onFire;


                renderer.updateRenderState(((EntityAccessor)entity).getCurrentIdentity(),entityRenderState,tickProgress);
                entityRenderState.hitbox=oldHitbox;
                        //currentRenderer.updateRenderState(entity,entityRenderState,tickProgress);
                    }
                }
            }
        }


        net.minecraft.client.render.entity.model.EntityModel model= QualityCommandsClient.getModel(entity);

        if(model!=null){
            NbtCompound nbt=((ember.qualitycommands.util.NbtComponentAccessor)(Object)(((EntityAccessor)entity).getCustomData())).getNbt();
            for(String key:nbt.getKeys()){
                if((key.length()>13)&&key.substring(0, 13).matches("hidden_parts."))
                if(nbt.getBoolean(key,false)){
                    net.minecraft.client.model.ModelPart part=model.getRootPart().createPartGetter().apply(key.substring(13));
                    if(part!=null){
                    part.hidden=true;
                    }
                }else{
                    net.minecraft.client.model.ModelPart part=model.getRootPart().createPartGetter().apply(key.substring(13));
                    if(part!=null){
                    part.hidden=false;
                    }
                }
                
            }
            
        }
        NbtCompound extraParts=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getCompoundOrEmpty("model_extra_parts");
        Optional<String> overlays=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("overlays");
        Optional<String> overlaysE=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("overlays_emissive");
        HashMap<String,List<String>> targetMap=new HashMap<String,List<String>>(0);
        for(String part:extraParts.getKeys()){
            ArrayList models=new ArrayList<String>(0);
            for(String modelName:extraParts.getString(part, "").split(",")){
                models.ensureCapacity(models.size()+1);
                models.add(modelName);
            }
            targetMap.put(part, models);
        }
        ArrayList tOverlays=new ArrayList<String>(0);
        try{
        QualityCommands.LOGGER.info("Overlays:"+overlays.orElse(""));
        if(overlays.orElse("").length()>0){
        for(String textureName:overlays.orElse("").split(",")){
            tOverlays.ensureCapacity(tOverlays.size()+1);
            tOverlays.add(textureName);
            QualityCommands.LOGGER.info("adding overlay to list "+textureName);
        }
        }
        }catch(Exception e){
            QualityCommands.LOGGER.info("ERROR");
        }
        ArrayList tOverlaysE=new ArrayList<String>(0);
        if(overlaysE.orElse("").length()>0){
        for(String textureName:overlaysE.orElse("").split(",")){
            tOverlaysE.ensureCapacity(tOverlaysE.size()+1);
            tOverlaysE.add(textureName.replace('.','/'));
        }
        }
        ((EntityRenderStateModifier)entityRenderState).setTargets(targetMap);
        ((EntityRenderStateModifier)entityRenderState).setOverlays(tOverlays);
        ((EntityRenderStateModifier)entityRenderState).setOverlaysE(tOverlaysE);
        //ArrayList L
        //.getKeys()
		info.setReturnValue(entityRenderState);
	}
}
