package net.Gabou.identity2.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;
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
import net.Gabou.identity2.ModEffects;
import net.Gabou.identity2.Identity2Client;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Item;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.registry.Registries;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.MinecraftClient;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.client.model.ModelPart;
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
                if(Registries.ENTITY_TYPE.containsId(Identifier.of(d))){
                    if(((EntityAccessor)entity).getCurrentIdentity()!=null){
                //Sync identity to entity
                Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                EntityRenderer renderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(identity);
                EntityRenderer currentRenderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(entity);
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
                        //currentRenderer.updateRenderState(entity,entityRenderState,tickProgress);
                    }
                }
            }
        }
        NbtCompound nbt = ((NbtComponentAccessor) (Object) (((EntityAccessor) entity).getCustomData())).getNbt();
        boolean hasHiddenPartOverrides = false;
        for (String key : nbt.getKeys()) {
            if (key.startsWith("hidden_parts.")) {
                hasHiddenPartOverrides = true;
                break;
            }
        }

        boolean shouldHideHead = false;
        if (MinecraftClient.getInstance().player != null) {
            Entity playerIdentity = ((EntityAccessor) MinecraftClient.getInstance().player).getCurrentIdentity();
            shouldHideHead = playerIdentity instanceof ShulkerEntity;
        }

        if (hasHiddenPartOverrides || shouldHideHead) {
            net.minecraft.client.render.entity.model.EntityModel model = Identity2Client.getModel(entity);
            if (model != null) {
                if (hasHiddenPartOverrides) {
                    for (String key : nbt.getKeys()) {
                        if (key.startsWith("hidden_parts.")) {
                            ModelPart part = model.getRootPart().createPartGetter().apply(key.substring(13));
                            if (part != null) {
                                part.hidden = nbt.getBoolean(key, false);
                            }
                        }
                    }
                }

                if (shouldHideHead) {
                    ModelPart head = model.getRootPart().createPartGetter().apply("head");
                    if (head != null) {
                        head.hidden = true;
                        head.xScale = 0;
                    }
                }
            }
        }

		info.setReturnValue(entityRenderState);
	}
}

