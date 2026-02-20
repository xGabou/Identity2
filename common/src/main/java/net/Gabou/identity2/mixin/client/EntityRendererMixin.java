package net.Gabou.identity2.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
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
import net.Gabou.identity2.ModEffects;
import net.Gabou.identity2.Identity2Client;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"), cancellable = true)
	private void getAndUpdateRenderStateModifier(T entity, float tickProgress,CallbackInfoReturnable info) {
		EntityRenderState entityRenderState=(EntityRenderState)info.getReturnValue();
        if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").isPresent()){
            if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get().length()!=0){
                String d=((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get();
                if(d.contains("{")){
                    d=d.substring(0,d.indexOf('{'));
                }
                if(BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(d))){
                    if(((EntityAccessor)entity).getCurrentIdentity()!=null){
                //Sync identity to entity
                Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                EntityRenderer renderer=((MinecraftClientAccessor)Minecraft.getInstance()).getEntityRenderManager().getRenderer(identity);
                EntityRenderer currentRenderer=((MinecraftClientAccessor)Minecraft.getInstance()).getEntityRenderManager().getRenderer(entity);
                {
            //living only:
            
            identity.setPosRaw(
                ((Entity)entity).position().x,
                ((Entity)entity).position().y,
                ((Entity)entity).position().z
            );
            if(identity instanceof EnderDragon dragonIdentity){
                identity.setYRot(entity.getYRot()+180);
            }else{
                identity.setYRot(entity.getYRot());
            }
            ((EntityAccessor)identity).setLastPosition(
                ((Entity)entity).oldPosition()
            );
            if((identity instanceof LivingEntity livingIdentity)&&(entity instanceof LivingEntity livingEntity)){
                if(livingIdentity.isJumping()!=livingEntity.isJumping()){
                    livingIdentity.setJumping(livingEntity.isJumping());
                }
            LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
            LimbAnimatorAccessor source = (LimbAnimatorAccessor) livingEntity.walkAnimation;

            target.setPrevSpeed(source.getPrevSpeed());
            target.setSpeed(source.getSpeed());
            //target.setPos(source.getPos());

            livingIdentity.swinging = livingEntity.swinging;//LivingEntity only
            livingIdentity.swingTime = livingEntity.swingTime;//living only
            livingIdentity.oAttackAnim = livingEntity.oAttackAnim;//living only
            livingIdentity.attackAnim = livingEntity.attackAnim;//living only
            if((livingIdentity instanceof net.minecraft.world.entity.monster.Shulker)==false){
                livingIdentity.yBodyRot = livingEntity.yBodyRot;//living only
                livingIdentity.yBodyRotO = livingEntity.yBodyRotO;//living only
            }
            livingIdentity.yHeadRot = livingEntity.yHeadRot;//living only
            livingIdentity.yHeadRotO = livingEntity.yHeadRotO;//living only
            livingIdentity.swingingArm = livingEntity.swingingArm;//livingonly
            livingIdentity.startUsingItem(livingEntity.getUsedItemHand());//living only
            }
            identity.tickCount = ((Entity)entity).tickCount;//all
            identity.setOnGround(((Entity)entity).onGround());//all entities
            identity.setDeltaMovement(((Entity)entity).getDeltaMovement());//all entities
            identity.setShiftKeyDown(((Entity)entity).isShiftKeyDown());//all entities
            identity.setSprinting(((Entity)entity).isSprinting());//all entities
            identity.setSwimming(((Entity)entity).isSwimming());//all entities
            identity.setPose(((Entity)entity).getPose());//all entities

            ((EntityAccessor) identity).setVehicle(((Entity)entity).getVehicle());
            ((EntityAccessor) identity).setTouchingWater(((Entity)entity).isInWater());

            if (identity instanceof Phantom) {
                identity.setXRot(-((Entity)entity).getXRot());
                identity.xRotO = -((Entity)entity).xRotO;//used to be prevPitch
            } else if((identity instanceof net.minecraft.world.entity.monster.Shulker)==false){
                identity.setXRot(((Entity)entity).getXRot());
                identity.xRotO = ((Entity)entity).xRotO;
            }
            //living only
            if((entity instanceof LivingEntity livingEntity)&&(identity instanceof LivingEntity livingIdentity)){
                //if (IdentityConfig.getInstance().identitiesEquipItems()) {
                    livingIdentity.setItemSlot(EquipmentSlot.MAINHAND, livingEntity.getItemBySlot(EquipmentSlot.MAINHAND));
                    livingIdentity.setItemSlot(EquipmentSlot.OFFHAND, livingEntity.getItemBySlot(EquipmentSlot.OFFHAND));
                //}

                //if (IdentityConfig.getInstance().identitiesEquipArmor()) {
                    livingIdentity.setItemSlot(EquipmentSlot.HEAD, livingEntity.getItemBySlot(EquipmentSlot.HEAD));
                    livingIdentity.setItemSlot(EquipmentSlot.CHEST, livingEntity.getItemBySlot(EquipmentSlot.CHEST));
                    livingIdentity.setItemSlot(EquipmentSlot.LEGS, livingEntity.getItemBySlot(EquipmentSlot.LEGS));
                    livingIdentity.setItemSlot(EquipmentSlot.FEET, livingEntity.getItemBySlot(EquipmentSlot.FEET));
                //}
            }

            if(entity instanceof LivingEntity){
                if (identity instanceof Mob) {
                    ((Mob) identity).setAggressive(((LivingEntity)entity).isUsingItem());
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


                identity.setSharedFlagOnFire(entity.isOnFire());
                }
                EntityRenderState oldState=entityRenderState;
                entityRenderState=renderer.createRenderState();
                //entityRenderState.onFire=oldState.onFire;


                renderer.extractRenderState(((EntityAccessor)entity).getCurrentIdentity(),entityRenderState,tickProgress);
                        //currentRenderer.updateRenderState(entity,entityRenderState,tickProgress);
                    }
                }
            }
        }
        CompoundTag nbt = ((NbtComponentAccessor) (Object) (((EntityAccessor) entity).getCustomData())).getNbt();
        boolean hasHiddenPartOverrides = false;
        for (String key : nbt.keySet()) {
            if (key.startsWith("hidden_parts.")) {
                hasHiddenPartOverrides = true;
                break;
            }
        }

        boolean shouldHideHead = false;
        if (Minecraft.getInstance().player != null) {
            Entity playerIdentity = ((EntityAccessor) Minecraft.getInstance().player).getCurrentIdentity();
            shouldHideHead = playerIdentity instanceof Shulker;
        }

        if (hasHiddenPartOverrides || shouldHideHead) {
            net.minecraft.client.model.EntityModel model = Identity2Client.getModel(entity);
            if (model != null) {
                if (hasHiddenPartOverrides) {
                    for (String key : nbt.keySet()) {
                        if (key.startsWith("hidden_parts.")) {
                            ModelPart part = model.root().createPartLookup().apply(key.substring(13));
                            if (part != null) {
                                part.skipDraw = nbt.getBooleanOr(key, false);
                            }
                        }
                    }
                }

                if (shouldHideHead) {
                    ModelPart head = model.root().createPartLookup().apply("head");
                    if (head != null) {
                        head.skipDraw = true;
                        head.xScale = 0;
                    }
                }
            }
        }

		info.setReturnValue(entityRenderState);
	}
}

