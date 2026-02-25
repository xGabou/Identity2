package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;

import net.minecraft.world.entity.*;
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
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.Gabou.identity2.util.AttributeContainerAccessor;
import net.Gabou.identity2.util.DefaultAttributeContainerAccessor;
import net.Gabou.identity2.IdentitySettings;
@Mixin(LivingEntity.class)
public class LivingEntityMixin extends EntityMixin implements LivingEntityAccessor{

    @Mutable
    @Shadow
    private AttributeMap attributes;
    public void fixAttributes(Entity entity, Entity identity){
        if((identity instanceof LivingEntity livingIdentity)&&(entity instanceof LivingEntity livingEntity)){
            this.attributes=createMangled(livingEntity.getAttributes(), livingIdentity.getAttributes());
            /*Identity2.LOGGER.info("Attributes mangled!");
            for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)this.attributes).getDefaultAttributes()).getInstances().values()){
                Identity2.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(this.attributes.getValue(attr.getAttribute())));
            }*/
        }
        
    }


    public AttributeMap createMangled(AttributeMap a, AttributeMap b){
        AttributeSupplier.Builder builder=AttributeSupplier.builder();
        for(AttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)a).getDefaultAttributes()).getInstances().values()){
            builder.add(attr.getAttribute(),attr.getBaseValue());
            //Identity2.LOGGER.info("Mangling A: "+attr.getAttribute().toString());
        }
        for(AttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)b).getDefaultAttributes()).getInstances().values()){
            builder.add(attr.getAttribute(),attr.getBaseValue());
            //Identity2.LOGGER.info("Mangling B: "+attr.getAttribute().toString());
        }
        AttributeMap newContainer=new AttributeMap(builder.build());
        newContainer.assignAllValues(a);
        newContainer.assignAllValues(b);
        newContainer.assignBaseValues(a);
        newContainer.assignBaseValues(b);
        /*for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)newContainer).getDefaultAttributes()).getInstances().values()){
            Identity2.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(newContainer.getValue(attr.getAttribute())));
        }*/
        return newContainer;
    }
@Shadow
public boolean canUseSlot(EquipmentSlot slot){return false;}
/*@Inject(method = "getMaxHealth()F", at=@At("HEAD"),cancellable=true)
private void getMaxHealthIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.getMaxHealth());
        }
    }
}*/

@Inject(method = "getAttributes()Lnet/minecraft/world/entity/ai/attributes/AttributeMap;", at=@At("HEAD"),cancellable=true)
private void getAttributesIdentity(CallbackInfoReturnable info){
    // Keep vanilla attributes for the host entity (especially players) so
    // combat damage and other modded attribute changes are not replaced by identity stats.
    try{
    if(this.saving==false){
        if(this.currentIdentity!=null){
            if(this.currentIdentity instanceof LivingEntity livingIdentity){
                info.setReturnValue(livingIdentity.getAttributes());
            }
        }
    }
    }catch(Exception e){
        int x=0;
    }
}

@Inject(method = "decreaseAirSupply(I)I", at=@At("HEAD"),cancellable=true)
private void getNextAirUnderwaterIdentity(int air,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(air);
        }
    }
}
@Inject(method = "increaseAirSupply(I)I", at=@At("HEAD"),cancellable=true)
private void getNextAirOnLandIdentity(int air,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(air);
        }
    }
}

@Inject(method = "isInvertedHealAndHarm()Z", at=@At("HEAD"),cancellable=true)
private void hasInvertedHealingAndHarmIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isInvertedHealAndHarm());
        }
    }
}
@Inject(method = "canBreatheUnderwater()Z", at=@At("HEAD"),cancellable=true)
private void canBreatheInWaterIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canBreatheUnderwater());
        }
    }
}




@Shadow
@Nullable
public SoundEvent getHurtSound(DamageSource source){return null;}
@Shadow
@Nullable
public SoundEvent getDeathSound(){return null;}




@Inject(method = "getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;", at=@At("HEAD"),cancellable=true)
private void getHurtSoundIdentity(DamageSource source,CallbackInfoReturnable info){
    if(IdentitySettings.useIdentitySounds){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(((LivingEntityAccessor)this.currentIdentity).getHurtSound(source));
        }
    }
    }
}
@Inject(method = "getDeathSound()Lnet/minecraft/sounds/SoundEvent;", at=@At("HEAD"),cancellable=true)
private void getDeathSoundIdentity(CallbackInfoReturnable info){
    if(IdentitySettings.useIdentitySounds){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(((LivingEntityAccessor)this.currentIdentity).getDeathSound());
        }
    }
    }
}





@Inject(method = "aiStep()V", at=@At("HEAD"),cancellable=true)
private void tickMovementIdentity(CallbackInfo info){
    //if ((Entity)(Object)this instanceof Player) {
        // Keep vanilla player movement/collision to avoid wall-sticking while morphed.
    //    return;
    //}
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            
            livingIdentity.setPos(this.position());
            livingIdentity.setDeltaMovement(this.getDeltaMovement());
            if(livingIdentity instanceof Mob mobIdentity){
                mobIdentity.setNoAi(false);
            }
            livingIdentity.aiStep();
            this.setPos(livingIdentity.position());
            this.setDeltaMovement(livingIdentity.getDeltaMovement());
            //info.cancel();
        }
    }
}

//getNextAir(underwater,onland) should be added
@Inject(method = "onClimbable()Z", at=@At("HEAD"), cancellable=true)
private void identity2$spiderWallClimb(CallbackInfoReturnable<Boolean> info){
    if (this.currentIdentity == null) {
        return;
    }
    EntityType<?> identityType = this.currentIdentity.getType();
    if (identityType != EntityType.SPIDER && identityType != EntityType.CAVE_SPIDER) {
        return;
    }
    if ((Entity)(Object)this instanceof Player player && player.isSpectator()) {
        info.setReturnValue(false);
        return;
    }
    info.setReturnValue(this.horizontalCollision);
}

@Inject(method = "canFreeze()Z", at=@At("HEAD"),cancellable=true)
private void canFreezeIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canFreeze());
        
    }
}
@Inject(method = "canStandOnFluid(Lnet/minecraft/world/level/material/FluidState;)Z", at=@At("HEAD"),cancellable=true)
private void canWalkOnFluidIdentity(net.minecraft.world.level.material.FluidState key,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canStandOnFluid(key));
        }
    }
}
@Inject(method = "isSensitiveToWater()Z", at=@At("HEAD"),cancellable=true)
private void hurtByWaterIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isSensitiveToWater());
        }
    }
}
@Inject(method = "isAffectedByPotions()Z", at=@At("HEAD"),cancellable=true)
private void isAffectedBySplashPotionsIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isAffectedByPotions());
        }
    }
}

@Inject(method = "getLastHurtByPlayerMemoryTime()I", at=@At("HEAD"),cancellable=true)
private void getPlayerHitTimerIdentity(CallbackInfoReturnable info){
    if(this.identityOf!=null){
        if(this.identityOf instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.getLastHurtByPlayerMemoryTime());
        }
    }
}



@Inject(method = "isInvulnerableTo(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)Z", at=@At("HEAD"),cancellable=true)
private void isInvulnerableToIdentity(ServerLevel world,DamageSource source,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isInvulnerableTo(world,source));
        }
    }
}
@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at=@At("HEAD"),cancellable=true)
private void pushAwayFromIdentity(Entity entity,CallbackInfo info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity==entity){
            info.cancel();
        }
    }
}


@Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at=@At("HEAD"),cancellable=true)
private void getEquippedStackIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            if(livingIdentity.canUseSlot(slot)==false){
                info.setReturnValue(Items.AIR.getDefaultInstance());
            }
        }
    }
}
@Inject(method = "canUseSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z", at=@At("HEAD"),cancellable=true)
private void canUseSlotIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canUseSlot(slot));
        }
    }
}
//Tons of Redirects - End
}

