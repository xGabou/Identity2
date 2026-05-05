package ember.qualitycommands.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.entity.MovementType;
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;

import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import ember.qualitycommands.ModComponents;
import ember.qualitycommands.QualityCommands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.entity.Entity;
import net.minecraft.component.type.NbtComponent;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.world.BlockView;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import ember.qualitycommands.util.LivingEntityAccessor;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Box;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.attribute.EntityAttribute;
import ember.qualitycommands.util.AttributeContainerAccessor;
import ember.qualitycommands.util.DefaultAttributeContainerAccessor;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import ember.qualitycommands.IdentitySettings;
@Mixin(LivingEntity.class)
public class LivingEntityMixin extends EntityMixin implements LivingEntityAccessor{

    @Mutable
    @Shadow
    private AttributeContainer attributes;
    public void fixAttributes(Entity entity, Entity identity){
        if((identity instanceof LivingEntity livingIdentity)&&(entity instanceof LivingEntity livingEntity)){
            this.attributes=createMangled(livingEntity.getAttributes(), livingIdentity.getAttributes());
            /*QualityCommands.LOGGER.info("Attributes mangled!");
            for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)this.attributes).getDefaultAttributes()).getInstances().values()){
                QualityCommands.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(this.attributes.getValue(attr.getAttribute())));
            }*/
        }
        
    }


    public AttributeContainer createMangled(AttributeContainer a, AttributeContainer b){
        DefaultAttributeContainer.Builder builder=DefaultAttributeContainer.builder();
        for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)a).getDefaultAttributes()).getInstances().values()){
            builder.add(attr.getAttribute(),attr.getBaseValue());
            //QualityCommands.LOGGER.info("Mangling A: "+attr.getAttribute().toString());
        }
        for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)b).getDefaultAttributes()).getInstances().values()){
            builder.add(attr.getAttribute(),attr.getBaseValue());
            //QualityCommands.LOGGER.info("Mangling B: "+attr.getAttribute().toString());
        }
        AttributeContainer newContainer=new AttributeContainer(builder.build());
        newContainer.setFrom(a);
        newContainer.setFrom(b);
        newContainer.setBaseFrom(a);
        newContainer.setBaseFrom(b);
        /*for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)newContainer).getDefaultAttributes()).getInstances().values()){
            QualityCommands.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(newContainer.getValue(attr.getAttribute())));
        }*/
        return newContainer;
    }
    @Shadow
    public int getNextAirUnderwater(int air){
        return 0;
    };
    @Shadow
	public int getNextAirOnLand(int air){
        return 0;
    };
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

@Inject(method = "getAttributes()Lnet/minecraft/entity/attribute/AttributeContainer;", at=@At("HEAD"),cancellable=true)
private void getAttributesIdentity(CallbackInfoReturnable info){
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

@Inject(method = "getNextAirUnderwater(I)I", at=@At("HEAD"),cancellable=true)
private void getNextAirUnderwaterIdentity(int air,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            //info.setReturnValue(((LivingEntityAccessor)livingIdentity).getNextAirUnderwater(air));
            info.setReturnValue(air);
        }
    }
}
@Inject(method = "getNextAirOnLand(I)I", at=@At("HEAD"),cancellable=true)
private void getNextAirOnLandIdentity(int air,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            //info.setReturnValue(((LivingEntityAccessor)livingIdentity).getNextAirOnLand(air));
            info.setReturnValue(air);
        }
    }
}

@Inject(method = "hasInvertedHealingAndHarm()Z", at=@At("HEAD"),cancellable=true)
private void hasInvertedHealingAndHarmIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.hasInvertedHealingAndHarm());
        }
    }
}
@Inject(method = "canBreatheInWater()Z", at=@At("HEAD"),cancellable=true)
private void canBreatheInWaterIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canBreatheInWater());
        }
    }
}




@Shadow
@Nullable
public SoundEvent getHurtSound(DamageSource source){return null;}
@Shadow
@Nullable
public SoundEvent getDeathSound(){return null;}




@Inject(method = "getHurtSound(Lnet/minecraft/entity/damage/DamageSource;)Lnet/minecraft/sound/SoundEvent;", at=@At("HEAD"),cancellable=true)
private void getHurtSoundIdentity(DamageSource source,CallbackInfoReturnable info){
    if(IdentitySettings.useIdentitySounds){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(((LivingEntityAccessor)this.currentIdentity).getHurtSound(source));
        }
    }
    }
}

@Shadow
private boolean canEnterTrapdoor(BlockPos pos, BlockState state){
    return false;
}
@Shadow
public Optional<BlockPos> climbingPos;

@ModifyReturnValue(method = "isClimbing()Z", at=@At("RETURN"))
private boolean isClimbingOverride(boolean original){
	if(original){
        return true;
    }
    if(this.isSpectator()){
        return false;
    }
    if(((NbtComponentAccessor)(Object)this.getCustomData()).getNbt().getDouble("climboneverything").isPresent()){
            if(((NbtComponentAccessor)(Object)this.getCustomData()).getNbt().getDouble("climboneverything").get()!=0.0){
                if((this.collidedwithwall>0)){
                this.climbingPos=Optional.of(this.getBlockPos());
                return true;
                }
            }
        }
    return false;
}
@Inject(method = "getDeathSound()Lnet/minecraft/sound/SoundEvent;", at=@At("HEAD"),cancellable=true)
private void getDeathSoundIdentity(CallbackInfoReturnable info){
    if(IdentitySettings.useIdentitySounds){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(((LivingEntityAccessor)this.currentIdentity).getDeathSound());
        }
    }
    }
}





@Inject(method = "tickMovement()V", at=@At("HEAD"),cancellable=true)
private void tickMovementIdentity(CallbackInfo info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            
            livingIdentity.setPosition(this.getEntityPos());
            livingIdentity.setVelocity(this.getVelocity());
            if(livingIdentity instanceof MobEntity mobIdentity){
                mobIdentity.setAiDisabled(false);
            }
            livingIdentity.tickMovement();
            this.setPosition(livingIdentity.getEntityPos());
            this.setVelocity(livingIdentity.getVelocity());
            //info.cancel();
        }
    }
}

//getNextAir(underwater,onland) should be added

@Inject(method = "canFreeze()Z", at=@At("HEAD"),cancellable=true)
private void canFreezeIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canFreeze());
        
    }
}
@Inject(method = "canWalkOnFluid(Lnet/minecraft/fluid/FluidState;)Z", at=@At("HEAD"),cancellable=true)
private void canWalkOnFluidIdentity(net.minecraft.fluid.FluidState key,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canWalkOnFluid(key));
        }
    }
}
@Inject(method = "hurtByWater()Z", at=@At("HEAD"),cancellable=true)
private void hurtByWaterIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.hurtByWater());
        }
    }
}
@Inject(method = "isAffectedBySplashPotions()Z", at=@At("HEAD"),cancellable=true)
private void isAffectedBySplashPotionsIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isAffectedBySplashPotions());
        }
    }
}

@Inject(method = "getPlayerHitTimer()I", at=@At("HEAD"),cancellable=true)
private void getPlayerHitTimerIdentity(CallbackInfoReturnable info){
    if(this.identityOf!=null){
        if(this.identityOf instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.getPlayerHitTimer());
        }
    }
}



@Inject(method = "isInvulnerableTo(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;)Z", at=@At("HEAD"),cancellable=true)
private void isInvulnerableToIdentity(ServerWorld world,DamageSource source,CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.isInvulnerableTo(world,source));
        }
    }
}
@Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at=@At("HEAD"),cancellable=true)
private void pushAwayFromIdentity(Entity entity,CallbackInfo info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity==entity){
            info.cancel();
        }
    }
}


@Inject(method = "getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;", at=@At("HEAD"),cancellable=true)
private void getEquippedStackIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            if(livingIdentity.canUseSlot(slot)==false){
                info.setReturnValue(Items.AIR.getDefaultStack());
            }
        }
    }
}
@Inject(method = "canUseSlot(Lnet/minecraft/entity/EquipmentSlot;)Z", at=@At("HEAD"),cancellable=true)
private void canUseSlotIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.canUseSlot(slot));
        }
    }
}
//Tons of Redirects - End
}

