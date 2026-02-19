package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
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
import net.minecraft.util.math.Vec3i;
import net.minecraft.entity.MovementType;
import net.Gabou.identity2.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntityDataObject;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.WriteView;
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
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.commands.WithCommand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.entity.Entity;
import net.minecraft.component.type.NbtComponent;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.BlockView;
import com.llamalad7.mixinextras.sugar.Local;

import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.Gabou.identity2.identity.IdentityProgression;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
@Mixin(Entity.class)
public class EntityMixin implements net.Gabou.identity2.util.EntityAccessor{
    @Shadow
    private NbtComponent customData;
    @Shadow
    private int id;
    @Shadow
    public void setId(int id){
        this.id=id;
    }
    @Shadow
    public int getId(){
        return id;
    }

    @Shadow
    public Vec3d getVelocity(){return null;}
    @Shadow
    public int getAir(){return 0;}
    @Shadow
    public void setAir(int air){return;}
    @Shadow
    public Vec3d getEntityPos(){return null;}
    @Shadow
    public final void setPosition(Vec3d v){return;}
    @Shadow
    public void setVelocity(Vec3d v){return;}
    @ModifyConstant(constant=@Constant(doubleValue=3.0E7),method="updatePosition")
    private static double TDIOA(double x){
        return Identity2.maxWorldSize;
    }
    @ModifyConstant(constant=@Constant(doubleValue=-3.0E7),method="updatePosition")
    private static double TDIOB(double x){
        return -Identity2.maxWorldSize;
    }
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onEntityLand(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;)V"))
    private void moveOnEntityLandOverride(Block block, BlockView view,Entity entity){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").get()!=0.0){
                entity.setVelocity(entity.getVelocity().multiply(1.0, ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").get(), 1.0));
            }else{
                block.onEntityLand(view,entity);
            }
        }else{
                block.onEntityLand(view,entity);
            }
    }
    @Inject(method = "tick", at=@At("HEAD"))
	private void identityFixCanFlyCheck(CallbackInfo info) {
        if(this.currentIdentity!=null && ((Entity)(Object)this).getEntityWorld().isClient()){
            this.currentIdentity.tick();
            
        }
        if(this.identityOf!=null){
            
            if(this.entityCanFlyTickEvaluated==false){
                this.entityCanFlyTickEvaluated=true;
                this.entityCanFlyEvaluated=false;
            }
            if(this.entityCanFlyEvaluated==false){
                    this.canFly();
                }
            this.identityOf.noClip=this.noClip;
        }
    }
    @Inject(method = "isInsideWall", at=@At("HEAD"),cancellable = true)
    protected void disableNoClipSuffocate(CallbackInfoReturnable info) {
		if(this.noClip){
            info.setReturnValue(false);
        }
	}
    @Inject(method = "tick", at=@At("RETURN"))
	private void identityFix(CallbackInfo info) {
		if(this.currentIdentity!=null){
            
            this.currentIdentity.setPosition(this.getEntityPos());
            this.currentIdentity.setVelocity(this.getVelocity());
            this.currentIdentity.setAir(this.getAir());
            if(
                (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                ((Entity)(Object)this instanceof LivingEntity livingEntity)
            ){
            livingIdentity.setHealth(livingEntity.getHealth());
            }
            if(this.currentIdentity.getEntityWorld().isClient()==false){
                if(this.currentIdentity instanceof MobEntity mobIdentity){
                    mobIdentity.setAiDisabled(true);
                }
                this.currentIdentity.tick();
                //if(this.currentIdentity instanceof MobEntity mobIdentity){
                //    mobIdentity.setAiDisabled(false);
                //}
            }
            
            this.setPosition(this.currentIdentity.getEntityPos());
            this.setVelocity(this.currentIdentity.getVelocity());
            this.setAir(this.currentIdentity.getAir());
            if(
                (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                ((Entity)(Object)this instanceof LivingEntity livingEntity)
            ){
                livingEntity.setHealth(livingIdentity.getHealth());
            }
            
        }
	}
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(DDD)V"))
    private void moveOnEntityLandWallOverride(Entity entity,double x,double y,double z, @Local(ordinal=0) boolean bl, @Local(ordinal=1) boolean bl2, @Local(ordinal=2) Vec3d vec3d4){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("horizontal_collision_speed_multiplier_override").isPresent()){
            double d=((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("horizontal_collision_speed_multiplier_override").get();
            if(d!=0.0){
                entity.setVelocity(bl ? vec3d4.x*d : vec3d4.x, vec3d4.y, bl2 ? vec3d4.z*d : vec3d4.z);
            }else{
                entity.setVelocity(x,y,z);
            }
        }else{
                entity.setVelocity(x,y,z);
            }
    }







    public int abilityCooldown=0;

    public int getAbilityCooldown(){
        return this.abilityCooldown;
    }
    public void setAbilityCooldown(int cooldown){
        this.abilityCooldown=cooldown;
    }



















    @Inject(method="onRemove",at=@At("HEAD"),cancellable=true)
    private void commandOnRemoved(Entity.RemovalReason reason,CallbackInfo info){
        if(reason.shouldDestroy()){
            String reasonType="";
            if(reason==Entity.RemovalReason.KILLED){
                reasonType="killed";
            }else if(reason==Entity.RemovalReason.DISCARDED){
                reasonType="discarded";
            }else if(reason==Entity.RemovalReason.UNLOADED_TO_CHUNK){
                reasonType="unloaded_chunk";
            }else if(reason==Entity.RemovalReason.UNLOADED_WITH_PLAYER){
                reasonType="unloaded_player";
            }else if(reason==Entity.RemovalReason.CHANGED_DIMENSION){
                reasonType="dimension_change";
            }
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed").isPresent()){
            String command=((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed").get();
                if(((Entity)(Object)this).getEntityWorld().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).getEntityWorld().getServer().getCommandManager().parseAndExecute(((Entity)(Object)this).getEntityWorld().getServer().getCommandSource().withEntity((Entity)(Object)this).withPosition(this.getEntityPos()).withSilent(),/*command*/
                        command
                        );
                        
                    }
                }
            }
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed_"+reasonType).isPresent()){
            String command=((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed_"+reasonType).get();
                if(((Entity)(Object)this).getEntityWorld().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).getEntityWorld().getServer().getCommandManager().parseAndExecute(((Entity)(Object)this).getEntityWorld().getServer().getCommandSource().withEntity((Entity)(Object)this).withPosition(this.getEntityPos()).withSilent(),/*command*/
                        command
                        );
                        
                    }
                }
            }
        }
    }
    




    @Inject(method="baseTick",at=@At("HEAD"),cancellable=true)
    private void commandOnTick(CallbackInfo info){
        if(this.abilityCooldown>0){
            this.abilityCooldown-=1;
        }
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_tick").isPresent()){
            String command=((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_tick").get();
                if(((Entity)(Object)this).getEntityWorld().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).getEntityWorld().getServer().getCommandManager().parseAndExecute(((Entity)(Object)this).getEntityWorld().getServer().getCommandSource().withEntity((Entity)(Object)this).withPosition(this.getEntityPos()).withSilent(),/*command*/
                        command
                        );
                        
                    }
                }
            }
            
        
    }
    



    @Shadow
    public boolean noClip=false;
    
    public boolean entityCanFly=false;
    public boolean entityCanFlyEvaluated=false;
    public boolean entityCanFlyTickEvaluated=false;
    

    public boolean canFly(){
        if(this.entityCanFlyEvaluated==false){

        
        String onLandName=(net.Gabou.identity2.checkonly.EntityMethodChecks.class).getDeclaredMethods()[0].getName();
        try{
        this.entityCanFly=net.Gabou.identity2.util.MFCheck.isMethodEmpty(((Object)this).getClass(),onLandName);
        }catch(
            Exception e
        ){int x=0;}
        if(this.shouldTickBlockCollision()==false){
            this.entityCanFly=true;
        }
        if(this.noClip){
            this.entityCanFly=true;
        }
        this.entityCanFlyEvaluated=true;
        if(this.identityOf!=null){
        if((Entity)(Object)this.identityOf instanceof PlayerEntity player){
                Entity playerIdentity = ((EntityAccessor) player).getCurrentIdentity();
                this.applyIdentityFlightGrant(player, playerIdentity != null && ((EntityAccessor) playerIdentity).canFly());
            }
        }

        }
        return this.entityCanFly;
    }

    private void applyIdentityFlightGrant(PlayerEntity player, boolean identityCanFly) {
        // Do not touch spectator/creative abilities.
        if (player.isSpectator() || player.getAbilities().creativeMode) {
            return;
        }

        if (!identityCanFly) {
            return;
        }

        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendAbilitiesUpdate();
            }
        }
    }






    @Inject(method="isControlledByPlayer",at=@At("HEAD"),cancellable=true)
    private void isControlledByPlayerOverride(CallbackInfoReturnable info){
        if(this.identityOf!=null){
            info.setReturnValue(((Entity)this.identityOf).isControlledByPlayer());
        }
    }
    
    @Inject(method="getWidth",at=@At("HEAD"),cancellable=true)
    private void getWidthOverride(CallbackInfoReturnable info){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get()>0.0){
                info.setReturnValue((Float)(float)(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get()
                );
            }
        }
    }
    @Inject(method="getDimensions",at=@At("RETURN"),cancellable=true)
    private void getDimensionsModification(CallbackInfoReturnable info){
        EntityDimensions dimensions = (EntityDimensions) info.getReturnValue();
        float oldWidth = dimensions.width();
        float oldHeight = dimensions.height();
        float widthOverride = oldWidth;
        float heightOverride = oldHeight;

        if (((NbtComponentAccessor) (Object) this.customData).getNbt().getDouble("width_override").isPresent()) {
            double value = ((NbtComponentAccessor) (Object) this.customData).getNbt().getDouble("width_override").get();
            if (value > 0.0) {
                widthOverride = (float) value;
            }
        }
        if (((NbtComponentAccessor) (Object) this.customData).getNbt().getDouble("height_override").isPresent()) {
            double value = ((NbtComponentAccessor) (Object) this.customData).getNbt().getDouble("height_override").get();
            if (value > 0.0) {
                heightOverride = (float) value;
            }
        }

        float widthScale = oldWidth > 0.0F ? widthOverride / oldWidth : 1.0F;
        float heightScale = oldHeight > 0.0F ? heightOverride / oldHeight : 1.0F;
        info.setReturnValue(dimensions.scaled(widthScale, heightScale));
    }
    @Inject(method="getHeight",at=@At("HEAD"),cancellable=true)
    private void getHeightOverride(CallbackInfoReturnable info){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get()>0.0){
                info.setReturnValue((Float)(float)(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get()
                );
            }
        }
    }
    @Shadow
    private Box boundingBox;
    @Inject(method="setBoundingBox",at=@At("TAIL"))
    private void getBoundingBoxModification(CallbackInfo info){
        Box box=((Box)this.boundingBox);
        double old_width=box.maxX-box.minX;
        double old_height=box.maxY-box.minY;
        double center_x=(box.maxX+box.minX)/2;
        double center_z=(box.maxZ+box.minZ)/2;
        double center_y=box.minY;
        double new_width=old_width;
        double new_height=old_height;

        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get()>0.0){
                new_width=(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get();
            }
        }
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get()>0.0){
                new_height=(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get();
            }
        }
        box=box.withMaxX(center_x+new_width/2);
        box=box.withMinX(center_x-new_width/2);
        box=box.withMaxZ(center_z+new_width/2);
        box=box.withMinZ(center_z-new_width/2);
        box=box.withMaxY(center_y+new_height);
        this.boundingBox=box;
        //info.setReturnValue(box);
    }
    @Override
    public net.minecraft.component.type.NbtComponent getCustomData(){
        if(this.customData==NbtComponent.DEFAULT){
        this.customData= NbtComponent.of(((NbtComponentAccessor)(Object)this.customData).getNbt().copy());
        //Identity2.LOGGER.info("Default Custom Data detected.");
        }
        return this.customData;
    };
    
    @Nullable
    public Entity currentIdentity=null;
    @Nullable
    public Entity identityOf=null;
    @Nullable
    public Entity getCurrentIdentity(){
        return this.currentIdentity;
    }
    @Nullable
    public Entity getIdentityOwner(){
        return this.identityOf;
    }
    public void setCurrentIdentity(Entity e){
        this.currentIdentity=e;
    }
    public void setIdentityOf(Entity e){
        this.identityOf=e;
    }
    public void setCurrentIdentity(String id, NbtCompound data){
        if (data != null && !data.isEmpty()) {
            this.setCurrentIdentity(id + data.toString());
            return;
        }
        this.setCurrentIdentity(id);
    }
    public void fixAttributes(Entity entity, Entity identity){}
    public void setCurrentIdentity(String id){
        this.noClip=false;
        NbtCompound nbtCompound=null;
        if(id.contains("{")){
            try{
            nbtCompound=net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(id.substring(id.indexOf('{'))));
            id=id.substring(0,id.indexOf('{'));
            }catch(Exception e){
            id=id.substring(0,id.indexOf('{'));
            }
        }
        if(nbtCompound==null){
            nbtCompound=new NbtCompound().copy();
        }
        if (nbtCompound.isEmpty()) {
            String variantRaw = ((NbtComponentAccessor) (Object) this.customData).getNbt().getString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
            if (!variantRaw.isBlank()) {
                try {
                    nbtCompound = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound()
                        .parse(new com.mojang.brigadier.StringReader(variantRaw));
                } catch (Exception ignored) {
                    nbtCompound = new NbtCompound().copy();
                }
            }
        }
        if(id.length()==0){
            this.currentIdentity=null;
            if((Entity)(Object)this instanceof PlayerEntity player){
                this.applyIdentityFlightGrant(player, false);
            }
            return;
        }
        nbtCompound.putString("id", id);
            if(((Entity)(Object)this).getEntityWorld() instanceof ServerWorld){
                //nbtCompound.putBoolean("NoAI",true);
            }
            EntityType<?> newType=Registries.ENTITY_TYPE.get(Identifier.of(id/*((NbtComponentAccessor)(Object)this.getCustomData()).getNbt().getString("model_override").get())*/));
            Vec3d pos=new Vec3d(0,0,0);
            BlockPos blockPos = BlockPos.ofFloored(pos);
            /*if (!World.isValid(blockPos)) {
                throw INVALID_POSITION_EXCEPTION.create();
            }*//* else if (source.getWorld().getDifficulty() == Difficulty.PEACEFUL && !entityType.isAllowedInPeaceful()) {
                throw FAILED_PEACEFUL_EXCEPTION.create();
            }*/ /*else*/ {

                World serverWorld = (World)((Entity)(Object)this).getEntityWorld();
                Entity entity = EntityType.loadEntityWithPassengers(nbtCompound, serverWorld, SpawnReason.COMMAND, entityx -> {
                    entityx.refreshPositionAndAngles(pos.x, pos.y, pos.z, entityx.getYaw(), entityx.getPitch());
                    return entityx;
                });
                if (entity == null) {
                    return;
                    //throw FAILED_EXCEPTION.create();
                } else {
                    /*if (initialize && entity instanceof MobEntity mobEntity) {
                        mobEntity.initialize(source.getWorld(), source.getWorld().getLocalDifficulty(entity.getBlockPos()), SpawnReason.COMMAND, null);
                    }*/
                        entity.setId(-this.getId());
                        ((EntityAccessor)entity).fixAttributes((Entity)(Object)this, entity);
                        this.currentIdentity=entity;
                        ((EntityAccessor)this.currentIdentity).setIdentityOf((Entity)(Object)this);
                    
                }
            }
        
        if(this.currentIdentity!=null){
            ((EntityAccessor)this.currentIdentity).setIdentityOf((Entity)(Object)this);
            //if(((Entity)(Object)this).getEntityWorld().isClient()){
            ((EntityAccessor)(this.currentIdentity)).setId(((EntityAccessor)(this.currentIdentity)).getId()*-1);
            //}
            if((Entity)(Object)this instanceof PlayerEntity player){
                Entity playerIdentity = ((EntityAccessor) player).getCurrentIdentity();
                this.applyIdentityFlightGrant(player, playerIdentity != null && ((EntityAccessor) playerIdentity).canFly());
            }
        }
        
    }
    @Shadow
    protected boolean touchingWater;
    @Shadow
    @Nullable private Entity vehicle;
    public void setVehicle(Entity vehicle){
        this.vehicle=vehicle;
    }
	public void setTouchingWater(boolean isTouchingWater){
        this.touchingWater=isTouchingWater;
    }
    @Shadow
	public double lastX;
    @Shadow
	public double lastY;
    @Shadow
	public double lastZ;
    @Shadow
	public double lastRenderX;
    @Shadow
	public double lastRenderY;
    @Shadow
	public double lastRenderZ;
    @Overwrite
    public void setLastPosition(Vec3d pos){
		this.lastX = this.lastRenderX = pos.x;
		this.lastY = this.lastRenderY = pos.y;
		this.lastZ = this.lastRenderZ = pos.z;
    };
    @Shadow
    public void addAirTravelEffects(){};
    public void runAddAirTravelEffects(){
        this.addAirTravelEffects();
    }
    @Shadow
    EntityDimensions dimensions;
    @Shadow
    float standingEyeHeight;
	public EntityDimensions getEntityDimensions(){
        return this.dimensions;
    };
	public void setEntityDimensions(EntityDimensions dimensions){
        this.dimensions=dimensions;
    };

    @Shadow
	public float getStandingEyeHeight(){
        return this.standingEyeHeight;
    };
	public void setStandingEyeHeight(float standingEyeHeight){
        this.standingEyeHeight=standingEyeHeight;
    };


































    //Tons of Redirects - Begin!
@Shadow
public Entity.MoveEffect getMoveEffect(){return null;}
@Inject(method = "getMoveEffect()Lnet/minecraft/entity/Entity$MoveEffect;", at=@At("HEAD"),cancellable=true)
private void getMoveEffectIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).getMoveEffect());
    }
}


@Inject(method = "onDamaged(Lnet/minecraft/entity/damage/DamageSource;)V", at=@At("HEAD"),cancellable=true)
private void onDamagedActual(DamageSource source,CallbackInfo info){
    if(this.currentIdentity!=null){
        this.currentIdentity.onDamaged(source);
    }
}
@Inject(method = "isRemoved()Z", at=@At("HEAD"),cancellable=true)
private void isRemovedActual(CallbackInfoReturnable info){
    if(this.identityOf!=null){
        info.setReturnValue(false);
    }
}











@Shadow
public boolean isFlappingWings(){return false;}
@Inject(method = "isFlappingWings()Z", at=@At("HEAD"),cancellable=true)
private void isFlappingWingsIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).isFlappingWings());
    }
}

@Shadow
public boolean shouldTickBlockCollision(){return false;}
@Inject(method = "shouldTickBlockCollision()Z", at=@At("HEAD"),cancellable=true)
private void shouldTickBlockCollisionIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).shouldTickBlockCollision());
    }
}


@Shadow
public double getGravity(){return 0;}
@Inject(method = "getGravity()D", at=@At("HEAD"),cancellable=true)
private void getGravityIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).getGravity());
    }
}


@Inject(method = "getSoundCategory()Lnet/minecraft/sound/SoundCategory;", at=@At("HEAD"),cancellable=true)
private void getSoundCategoryIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getSoundCategory());
    }
}

@Inject(method = "getInterpolator()Lnet/minecraft/entity/PositionInterpolator;", at=@At("HEAD"),cancellable=true)
private void getInterpolatorIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getInterpolator());
    }
}

@Inject(method = "isCollidable(Lnet/minecraft/entity/Entity;)Z", at=@At("HEAD"),cancellable=true)
private void isCollidableIdentity(@Nullable Entity entity, CallbackInfoReturnable info){
    try{
        if(entity!=null){
            if(((EntityAccessor)entity).getIdentityOwner()!=null){
                info.setReturnValue(false);
                return;
            }
        }
    }catch(Exception e){int x=0;}
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isCollidable(entity));
    }
}

@Inject(method = "collidesWithStateAtPos(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", at=@At("HEAD"),cancellable=true)
private void collidesWithStateAtPosIdentity(BlockPos pos, BlockState state, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.collidesWithStateAtPos(pos, state));
    }
}

@Inject(method = "shouldSpawnSprintingParticles()Z", at=@At("HEAD"),cancellable=true)
private void shouldSpawnSprintingParticlesIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.shouldSpawnSprintingParticles());
    }
}

@Inject(method = "canBeHitByProjectile()Z", at=@At("HEAD"),cancellable=true)
private void canBeHitByProjectileIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canBeHitByProjectile());
    }
}

@Inject(method = "canHit()Z", at=@At("HEAD"),cancellable=true)
private void canHitIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canHit());
    }
}

@Inject(method = "isPushable()Z", at=@At("HEAD"),cancellable=true)
private void isPushableIdentity(CallbackInfoReturnable info){

    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isPushable());
    }
}
@Inject(method = "isPushedByFluids()Z", at=@At("HEAD"),cancellable=true)
private void isPushedByFluidsIdentity(CallbackInfoReturnable info){

    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isPushedByFluids());
    }
}

@Inject(method = "isInsideWall()Z", at=@At("HEAD"),cancellable=true)
private void isInsideWallIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isInsideWall());
    }
}




@Inject(method = "interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at=@At("HEAD"),cancellable=true)
private void interactIdentity(PlayerEntity player, Hand hand, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.interact(player, hand));
    }
}

@Inject(method = "collidesWith(Lnet/minecraft/entity/Entity;)Z", at=@At("HEAD"),cancellable=true)
private void collidesWithIdentity(Entity other, CallbackInfoReturnable info){
    if(((EntityAccessor)other).getIdentityOwner()!=null){
        info.setReturnValue(false);
        return;
    }
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.collidesWith(other));
    }
}

@Inject(method = "getMaxAir()I", at=@At("HEAD"),cancellable=true)
private void getMaxAirIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getMaxAir());
    }
}

@Inject(method = "getFreezingScale()F", at=@At("HEAD"),cancellable=true)
private void getFreezingScaleIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getFreezingScale());
    }
}

@Inject(method = "isAttackable()Z", at=@At("HEAD"),cancellable=true)
private void isAttackableIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isAttackable());
    }
}

@Inject(method = "isInvulnerable()Z", at=@At("HEAD"),cancellable=true)
private void isInvulnerableIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isInvulnerable());
    }
}

@Inject(method = "isCustomNameVisible()Z", at=@At("HEAD"),cancellable=true)
private void isCustomNameVisibleIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isCustomNameVisible());
    }
}

/*@Inject(method = "getEyeHeight(Lnet/minecraft/entity/Entity;)F", at=@At("HEAD"),cancellable=true)
private void getEyeHeightIdentity(EntityPose pose, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getEyeHeight(pose));
    }
}*/

public boolean saving=false;

@Inject(method = "writeData(Lnet/minecraft/storage/WriteView;)V", at=@At("HEAD"),cancellable=true)
public void writeDataLabelSaving(WriteView view,CallbackInfo info) {
    this.saving=true;
}
@Inject(method = "writeData(Lnet/minecraft/storage/WriteView;)V", at=@At("TAIL"),cancellable=true)
public void writeDataLabelDoneSaving(WriteView view,CallbackInfo info) {
    this.saving=false;
}



@Inject(method = "getStandingEyeHeight()F", at=@At("HEAD"),cancellable=true)
private void getStandingEyeHeightIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getStandingEyeHeight());
    }
}

@Inject(method = "getPistonBehavior()Lnet/minecraft/block/piston/PistonBehavior;", at=@At("HEAD"),cancellable=true)
private void getPistonBehaviorIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getPistonBehavior());
    }
}

@Inject(method = "canSprintAsVehicle()Z", at=@At("HEAD"),cancellable=true)
private void canSprintAsVehicleIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canSprintAsVehicle());
    }
}

@Inject(method = "getStepHeight()F", at=@At("HEAD"),cancellable=true)
private void getStepHeightIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getStepHeight());
    }
}

@Inject(method = "onLanding()V", at=@At("HEAD"))
private void onLandingIdentity(CallbackInfo info){
    if(this.currentIdentity!=null){
       this.currentIdentity.onLanding();
    }
}

@Inject(method = "setCustomNameVisible(Z)V", at=@At("HEAD"))
private void setCustomNameVisibleIdentity(boolean visible, CallbackInfo info){
    if(this.currentIdentity!=null){
       this.currentIdentity.setCustomNameVisible(visible);
    }
}
//Tons of Redirects - End
}
