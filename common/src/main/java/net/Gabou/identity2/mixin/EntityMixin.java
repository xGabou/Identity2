package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
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
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.identity.IdentityTraitTags;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.Gabou.identity2.identity.IdentityProgression;
import java.lang.reflect.Method;
@Mixin(Entity.class)
public class EntityMixin implements net.Gabou.identity2.util.EntityAccessor{
    @Shadow
    private CustomData customData;
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
    public Vec3 getDeltaMovement(){return null;}
    @Shadow
    public int getAirSupply(){return 0;}
    @Shadow
    public void setAirSupply(int air){return;}
    @Shadow
    public Vec3 position(){return null;}
    @Shadow
    public final void setPos(Vec3 v){return;}
    @Shadow
    public void setDeltaMovement(Vec3 v){return;}
    @ModifyConstant(constant=@Constant(doubleValue=3.0E7),method="absSnapTo(DDD)V")
    private static double TDIOA(double x){
        return Identity2.maxWorldSize;
    }
    @ModifyConstant(constant=@Constant(doubleValue=-3.0E7),method="absSnapTo(DDD)V")
    private static double TDIOB(double x){
        return -Identity2.maxWorldSize;
    }
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void moveOnEntityLandOverride(Block block, BlockGetter view,Entity entity){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").get()!=0.0){
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("land_speed_multiplier_override").get(), 1.0));
            }else{
                block.updateEntityMovementAfterFallOn(view,entity);
            }
        }else{
                block.updateEntityMovementAfterFallOn(view,entity);
            }
    }
	@Inject(method = "tick", at=@At("HEAD"))
	private void identityFixCanFlyCheck(CallbackInfo info) {
        if(this.currentIdentity!=null && ((Entity)(Object)this).level().isClientSide()){
            this.currentIdentity.tick();
            
        }
        this.identity2$applyMorphPassiveTraits();
        if(this.identityOf!=null){
            this.canFly();
            this.identityOf.noPhysics=this.noPhysics;
        }
    }
    @Inject(method = "isInWall", at=@At("HEAD"),cancellable = true)
    protected void disableNoClipSuffocate(CallbackInfoReturnable info) {
		if(this.noPhysics){
            info.setReturnValue(false);
        }
	}
	@Inject(method = "tick", at=@At("RETURN"))
	private void identityFix(CallbackInfo info) {
		if(this.currentIdentity!=null){
            boolean hostIsPlayer = ((Entity)(Object)this) instanceof Player;
            if (hostIsPlayer) {
                this.identity2$applyMorphAquaticBreathing((Player) (Object) this);
            }
             
            this.currentIdentity.setPos(this.position());
            this.currentIdentity.setDeltaMovement(this.getDeltaMovement());
            this.currentIdentity.setAirSupply(this.getAirSupply());
            if(
                (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                ((Entity)(Object)this instanceof LivingEntity livingEntity)
            ){
            livingIdentity.setHealth(livingEntity.getHealth());
            }
            if(this.currentIdentity.level().isClientSide()==false && !hostIsPlayer){
                if(this.currentIdentity instanceof Mob mobIdentity){
                    mobIdentity.setNoAi(true);
                }
                this.currentIdentity.tick();
                //if(this.currentIdentity instanceof MobEntity mobIdentity){
                //    mobIdentity.setAiDisabled(false);
                //}
            }
             
            if(!hostIsPlayer){
                this.setPos(this.currentIdentity.position());
                this.setDeltaMovement(this.currentIdentity.getDeltaMovement());
                this.setAirSupply(this.currentIdentity.getAirSupply());
                if(
                    (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                    ((Entity)(Object)this instanceof LivingEntity livingEntity)
                ){
                    livingEntity.setHealth(livingIdentity.getHealth());
                }
            }
             
        }
	}
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private void moveOnEntityLandWallOverride(Entity entity,double x,double y,double z, @Local(ordinal=0) boolean bl, @Local(ordinal=1) boolean bl2, @Local(ordinal=2) Vec3 vec3d4){
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("horizontal_collision_speed_multiplier_override").isPresent()){
            double d=((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("horizontal_collision_speed_multiplier_override").get();
            if(d!=0.0){
                entity.setDeltaMovement(bl ? vec3d4.x*d : vec3d4.x, vec3d4.y, bl2 ? vec3d4.z*d : vec3d4.z);
            }else{
                entity.setDeltaMovement(x,y,z);
            }
        }else{
                entity.setDeltaMovement(x,y,z);
            }
    }







    public int abilityCooldown=0;

    public int getAbilityCooldown(){
        return this.abilityCooldown;
    }
    public void setAbilityCooldown(int cooldown){
        this.abilityCooldown=cooldown;
    }



















    @Inject(method="onRemoval",at=@At("HEAD"),cancellable=true)
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
                if(((Entity)(Object)this).level().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).level().getServer().getCommands().performPrefixedCommand(((Entity)(Object)this).level().getServer().createCommandSourceStack().withEntity((Entity)(Object)this).withPosition(this.position()).withSuppressedOutput(),/*command*/
                        command
                        );
                        
                    }
                }
            }
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed_"+reasonType).isPresent()){
            String command=((NbtComponentAccessor)(Object)this.customData).getNbt().getString("on_removed_"+reasonType).get();
                if(((Entity)(Object)this).level().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).level().getServer().getCommands().performPrefixedCommand(((Entity)(Object)this).level().getServer().createCommandSourceStack().withEntity((Entity)(Object)this).withPosition(this.position()).withSuppressedOutput(),/*command*/
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
                if(((Entity)(Object)this).level().getServer()!=null){
                    if(command!=""){
                    
                        ((Entity)(Object)this).level().getServer().getCommands().performPrefixedCommand(((Entity)(Object)this).level().getServer().createCommandSourceStack().withEntity((Entity)(Object)this).withPosition(this.position()).withSuppressedOutput(),/*command*/
                        command
                        );
                        
                    }
                }
            }
            
        
    }
    



    @Shadow
    public boolean noPhysics=false;
    
    public boolean entityCanFly=false;
    public boolean entityCanFlyEvaluated=false;
    public boolean entityCanFlyTickEvaluated=false;
    private boolean identity2$grantedMayfly = false;
    private long entityCanFlyLastEvalTick = Long.MIN_VALUE;
    private static final long ENTITY_FLY_REEVAL_TICKS = 20L;
    private static final String FALL_METHOD_NAME = identity2$resolveFallMethodName();
    

    public boolean canFly(){
        long gameTime = 0L;
        Entity self = (Entity)(Object)this;
        if (self.level() != null) {
            gameTime = self.level().getGameTime();
        }
        boolean shouldReevaluate = !this.entityCanFlyEvaluated
            || this.entityCanFlyLastEvalTick == Long.MIN_VALUE
            || (gameTime - this.entityCanFlyLastEvalTick) >= ENTITY_FLY_REEVAL_TICKS;
        if(shouldReevaluate){

        Boolean taggedFlight = IdentityTraitTags.resolveFlight(self.getType());
        if (Boolean.TRUE.equals(taggedFlight)) {
            this.entityCanFly = true;
        } else if (Boolean.FALSE.equals(taggedFlight)) {
            this.entityCanFly = false;
        } else {
            try{
            this.entityCanFly=net.Gabou.identity2.util.MFCheck.isMethodEmpty(((Object)this).getClass(),FALL_METHOD_NAME);
            }catch(
                Exception e
            ){int x=0;}
            if(this.isAffectedByBlocks()==false){
                this.entityCanFly=true;
            }
            if(this.noPhysics){
                this.entityCanFly=true;
            }
        }
        this.entityCanFlyEvaluated=true;
        this.entityCanFlyLastEvalTick = gameTime;
        if(this.identityOf!=null){
        if((Entity)(Object)this.identityOf instanceof Player player){
                Entity playerIdentity = ((EntityAccessor) player).getCurrentIdentity();
                this.applyIdentityFlightGrant(player, playerIdentity != null && ((EntityAccessor) playerIdentity).canFly());
            }
        }

        }
        return this.entityCanFly;
    }

    private static String identity2$resolveFallMethodName() {
        try {
            for (Method method : Entity.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getReturnType() == Void.TYPE
                    && params.length == 4
                    && params[0] == Double.TYPE
                    && params[1] == Boolean.TYPE
                    && params[2] == BlockState.class
                    && params[3] == BlockPos.class) {
                    return method.getName();
                }
            }
        } catch (Throwable ignored) {
        }
        return "fall";
    }

    private void applyIdentityFlightGrant(Player player, boolean identityCanFly) {
        // Do not touch spectator/creative abilities.
        if (player.isSpectator() || player.getAbilities().instabuild) {
            this.identity2$grantedMayfly = false;
            return;
        }

        if (identityCanFly) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            }
            this.identity2$grantedMayfly = true;
            return;
        }

        if (this.identity2$grantedMayfly) {
            player.getAbilities().mayfly = false;
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
            }
            this.identity2$grantedMayfly = false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        }
    }

    private void identity2$applyMorphPassiveTraits() {
        Entity self = (Entity)(Object)this;
        if (self.level().isClientSide()) {
            return;
        }
        if (!(self instanceof Player player)) {
            return;
        }
        if (this.currentIdentity == null) {
            return;
        }
        this.applyIdentityFlightGrant(player, ((EntityAccessor) this.currentIdentity).canFly());

        EntityType<?> identityType = this.currentIdentity.getType();
        if (IdentityTraitTags.burnsInDaylight(identityType) && this.identity2$shouldBurnInDaylight(player)) {
            player.igniteForSeconds(8.0F);
        }

        if (IdentityTraitTags.hasSlowFalling(identityType) && !player.onGround() && player.getDeltaMovement().y < 0.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false, true));
        }
    }

    private boolean identity2$shouldBurnInDaylight(Player player) {
        if (player.isSpectator() || player.getAbilities().instabuild || player.getAbilities().invulnerable) {
            return false;
        }
        if (!player.level().isBrightOutside()) {
            return false;
        }
        if (player.isInWaterOrRain()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return player.level().canSeeSky(pos);
    }

    private void identity2$applyMorphAquaticBreathing(Player player) {
        if (this.currentIdentity == null) {
            return;
        }
        EntityType<?> identityType = this.currentIdentity.getType();
        boolean requiresWater = Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(identityType));
        if (!requiresWater && this.currentIdentity instanceof LivingEntity livingIdentity) {
            requiresWater = livingIdentity.canBreatheUnderwater();
        }
        if (!requiresWater) {
            return;
        }

        if (player.isInWaterOrRain()) {
            player.setAirSupply(player.getMaxAirSupply());
            return;
        }

        int nextAir = ((LivingEntityAccessor) player).getNextAirUnderwater(player.getAirSupply());
        player.setAirSupply(nextAir);
        if (nextAir <= -20) {
            player.setAirSupply(0);
            if (player.level() instanceof ServerLevel serverLevel) {
                player.hurtServer(serverLevel, player.damageSources().dryOut(), 2.0F);
            }
        }
    }






    @Inject(method="isClientAuthoritative",at=@At("HEAD"),cancellable=true)
    private void isControlledByPlayerOverride(CallbackInfoReturnable info){
        if(this.identityOf!=null){
            info.setReturnValue(((Entity)this.identityOf).isClientAuthoritative());
        }
    }
    
    @Inject(method="getBbWidth",at=@At("HEAD"),cancellable=true)
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
        info.setReturnValue(dimensions.scale(widthScale, heightScale));
    }
    @Inject(method="getBbHeight",at=@At("HEAD"),cancellable=true)
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
    private AABB bb;
    @Inject(method="setBoundingBox",at=@At("TAIL"))
    private void getBoundingBoxModification(CallbackInfo info){
        AABB box=((AABB)this.bb);
        double old_width=box.maxX-box.minX;
        double old_height=box.maxY-box.minY;
        double center_x=(box.maxX+box.minX)/2;
        double center_z=(box.maxZ+box.minZ)/2;
        double center_y=box.minY;
        double new_width=old_width;
        double new_height=old_height;
        boolean hasOverride=false;

        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get()>0.0){
                new_width=(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("width_override").get();
                hasOverride=true;
            }
        }
        if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").isPresent()){
            if(((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get()>0.0){
                new_height=(double)
                ((NbtComponentAccessor)(Object)this.customData).getNbt().getDouble("height_override").get();
                hasOverride=true;
            }
        }
        if(!hasOverride){
            return;
        }
        box=box.setMaxX(center_x+new_width/2);
        box=box.setMinX(center_x-new_width/2);
        box=box.setMaxZ(center_z+new_width/2);
        box=box.setMinZ(center_z-new_width/2);
        box=box.setMaxY(center_y+new_height);
        this.bb=box;
        //info.setReturnValue(box);
    }
    @Override
    public net.minecraft.world.item.component.CustomData getCustomData(){
        if(this.customData==CustomData.EMPTY){
        this.customData= CustomData.of(((NbtComponentAccessor)(Object)this.customData).getNbt().copy());
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
    public void setCurrentIdentity(String id, CompoundTag data){
        if (data != null && !data.isEmpty()) {
            this.setCurrentIdentity(id + data.toString());
            return;
        }
        this.setCurrentIdentity(id);
    }
    public void fixAttributes(Entity entity, Entity identity){}
    public void setCurrentIdentity(String id){
        this.noPhysics=false;
        this.entityCanFlyEvaluated = false;
        this.entityCanFlyTickEvaluated = false;
        this.entityCanFlyLastEvalTick = Long.MIN_VALUE;
        CompoundTag nbtCompound=null;
        if(id.contains("{")){
            try{
            nbtCompound=net.minecraft.commands.arguments.CompoundTagArgument.compoundTag().parse(new com.mojang.brigadier.StringReader(id.substring(id.indexOf('{'))));
            id=id.substring(0,id.indexOf('{'));
            }catch(Exception e){
            id=id.substring(0,id.indexOf('{'));
            }
        }
        if(nbtCompound==null){
            nbtCompound=new CompoundTag().copy();
        }
        if (nbtCompound.isEmpty()) {
            String variantRaw = ((NbtComponentAccessor) (Object) this.customData).getNbt().getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
            if (!variantRaw.isBlank()) {
                try {
                    nbtCompound = net.minecraft.commands.arguments.CompoundTagArgument.compoundTag()
                        .parse(new com.mojang.brigadier.StringReader(variantRaw));
                } catch (Exception ignored) {
                    nbtCompound = new CompoundTag().copy();
                }
            }
        }
        if(id.length()==0){
            this.currentIdentity=null;
            this.entityCanFly = false;
            ((Entity)(Object)this).refreshDimensions();
            this.setStandingEyeHeight(((Entity)(Object)this).getEyeHeight());
            if((Entity)(Object)this instanceof Player player){
                this.applyIdentityFlightGrant(player, false);
                if (player instanceof ServerPlayer serverPlayer) {
                    IdentityProgression.refreshScaledHealth(serverPlayer);
                }
            }
            return;
        }
        Identifier identityId;
        try {
            identityId = Identifier.parse(id);
        } catch (Exception e) {
            this.deactivateIdentityAfterFailure(null, "invalid id");
            return;
        }
        if (IdentityProgression.isIdentityTemporarilyDisabled(identityId)) {
            this.deactivateIdentityAfterFailure(identityId, IdentityProgression.getDisabledIdentityReason(identityId));
            return;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            this.deactivateIdentityAfterFailure(identityId, "entity type missing");
            return;
        }

        nbtCompound.putString("id", identityId.toString());
        Vec3 pos=new Vec3(0,0,0);
        try {
            Level serverWorld = (Level)((Entity)(Object)this).level();
            Entity entity = EntityType.loadEntityRecursive(nbtCompound, serverWorld, EntitySpawnReason.COMMAND, entityx -> {
                entityx.snapTo(pos.x, pos.y, pos.z, entityx.getYRot(), entityx.getXRot());
                return entityx;
            });
            if (entity == null) {
                throw new IllegalStateException("loadEntityWithPassengers returned null");
            }
            entity.setId(-this.getId());
            ((EntityAccessor)entity).fixAttributes((Entity)(Object)this, entity);
            this.currentIdentity=entity;
            ((EntityAccessor)this.currentIdentity).setIdentityOf((Entity)(Object)this);
        } catch (Throwable throwable) {
            String reason = throwable.getClass().getSimpleName();
            IdentityProgression.disableIdentity(identityId, reason);
            Identity2.LOGGER.error("Failed to load identity {}. It has been disabled for this runtime.", identityId, throwable);
            this.deactivateIdentityAfterFailure(identityId, reason);
            return;
        }
        
        if(this.currentIdentity!=null){
            ((EntityAccessor)this.currentIdentity).setIdentityOf((Entity)(Object)this);
            //if(((Entity)(Object)this).getEntityWorld().isClient()){
            ((EntityAccessor)(this.currentIdentity)).setId(((EntityAccessor)(this.currentIdentity)).getId()*-1);
            //}
            ((Entity)(Object)this).refreshDimensions();
            this.setStandingEyeHeight(this.currentIdentity.getEyeHeight());
            if((Entity)(Object)this instanceof Player player){
                Entity playerIdentity = ((EntityAccessor) player).getCurrentIdentity();
                this.applyIdentityFlightGrant(player, playerIdentity != null && ((EntityAccessor) playerIdentity).canFly());
                if (player instanceof ServerPlayer serverPlayer) {
                    IdentityProgression.refreshScaledHealth(serverPlayer);
                }
            }
        }
        
    }
    private void deactivateIdentityAfterFailure(@Nullable Identifier identityId, String reason) {
        this.currentIdentity = null;
        this.entityCanFly = false;
        this.entityCanFlyEvaluated = false;
        this.entityCanFlyTickEvaluated = false;
        this.entityCanFlyLastEvalTick = Long.MIN_VALUE;
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.customData).getNbt();
        nbt.putString("model_override", "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        nbt.putDouble(IdentityProgression.TRANSITION_START_TICK_KEY, 0.0D);
        nbt.putDouble(IdentityProgression.TRANSITION_DURATION_TICKS_KEY, 0.0D);

        if ((Entity) (Object) this instanceof Player player) {
            this.applyIdentityFlightGrant(player, false);
            ((Entity) (Object) this).refreshDimensions();
            this.setStandingEyeHeight(((Entity) (Object) this).getEyeHeight());
            if (player instanceof ServerPlayer serverPlayer && identityId != null) {
                serverPlayer.displayClientMessage(
                    Component.literal(
                        "Identity disabled after load failure: " + identityId + (reason == null || reason.isBlank() ? "" : " (" + reason + ")")
                    ),
                    false
                );
            }
        }
    }
    @Shadow
    protected boolean wasTouchingWater;
    @Shadow
    @Nullable private Entity vehicle;
    public void setVehicle(Entity vehicle){
        this.vehicle=vehicle;
    }
	public void setTouchingWater(boolean isTouchingWater){
        this.wasTouchingWater=isTouchingWater;
    }
    @Shadow
	public double xo;
    @Shadow
	public double yo;
    @Shadow
	public double zo;
    @Shadow
	public double xOld;
    @Shadow
	public double yOld;
    @Shadow
	public double zOld;
    @Overwrite
    public void setOldPos(Vec3 pos){
		this.xo = this.xOld = pos.x;
		this.yo = this.yOld = pos.y;
		this.zo = this.zOld = pos.z;
    };
    @Override
    public void setLastPosition(Vec3 pos) {
        this.setOldPos(pos);
    }
    @Shadow
    public void processFlappingMovement(){};
    public void runAddAirTravelEffects(){
        this.processFlappingMovement();
    }
    @Shadow
    EntityDimensions dimensions;
    @Shadow
    float eyeHeight;
	public EntityDimensions getEntityDimensions(){
        return this.dimensions;
    };
	public void setEntityDimensions(EntityDimensions dimensions){
        this.dimensions=dimensions;
        ((Entity)(Object)this).refreshDimensions();
    };

    @Shadow
	public float getEyeHeight(){
        return this.eyeHeight;
    };
    @Override
    public float getStandingEyeHeight() {
        return this.getEyeHeight();
    }
	public void setStandingEyeHeight(float standingEyeHeight){
        this.eyeHeight=standingEyeHeight;
    };


































    //Tons of Redirects - Begin!
@Shadow
public Entity.MovementEmission getMovementEmission(){return null;}
@Override
public Entity.MovementEmission getMoveEffect() {
    return this.getMovementEmission();
}
@Inject(method = "getMovementEmission()Lnet/minecraft/world/entity/Entity$MovementEmission;", at=@At("HEAD"),cancellable=true)
private void getMoveEffectIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).getMoveEffect());
    }
}


@Inject(method = "handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V", at=@At("HEAD"),cancellable=true)
private void onDamagedActual(DamageSource source,CallbackInfo info){
    if(this.currentIdentity!=null){
        this.currentIdentity.handleDamageEvent(source);
    }
}
@Inject(method = "isRemoved()Z", at=@At("HEAD"),cancellable=true)
private void isRemovedActual(CallbackInfoReturnable info){
    if(this.identityOf!=null){
        info.setReturnValue(false);
    }
}











@Shadow
public boolean isFlapping(){return false;}
@Override
public boolean isFlappingWings() {
    return this.isFlapping();
}
@Inject(method = "isFlapping()Z", at=@At("HEAD"),cancellable=true)
private void isFlappingWingsIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).isFlappingWings());
    }
}

@Shadow
public boolean isAffectedByBlocks(){return false;}
@Override
public boolean shouldTickBlockCollision() {
    return this.isAffectedByBlocks();
}
@Inject(method = "isAffectedByBlocks()Z", at=@At("HEAD"),cancellable=true)
private void shouldTickBlockCollisionIdentity(CallbackInfoReturnable info){
    if(((Entity)(Object)this) instanceof Player){
        return;
    }
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).shouldTickBlockCollision());
    }
}


@Shadow
public double getDefaultGravity(){return 0;}
@Override
public double getIdentityGravity() {
    return this.getDefaultGravity();
}
@Inject(method = "getDefaultGravity()D", at=@At("HEAD"),cancellable=true)
private void getGravityIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(((EntityAccessor)this.currentIdentity).getIdentityGravity());
    }
}


@Inject(method = "getSoundSource()Lnet/minecraft/sounds/SoundSource;", at=@At("HEAD"),cancellable=true)
private void getSoundCategoryIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getSoundSource());
    }
}

@Inject(method = "getInterpolation()Lnet/minecraft/world/entity/InterpolationHandler;", at=@At("HEAD"),cancellable=true)
private void getInterpolatorIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getInterpolation());
    }
}

@Inject(method = "canBeCollidedWith(Lnet/minecraft/world/entity/Entity;)Z", at=@At("HEAD"),cancellable=true)
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
        info.setReturnValue(this.currentIdentity.canBeCollidedWith(entity));
    }
}

@Inject(method = "isColliding(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", at=@At("HEAD"),cancellable=true)
private void collidesWithStateAtPosIdentity(BlockPos pos, BlockState state, CallbackInfoReturnable info){
    if(((Entity)(Object)this) instanceof Player){
        return;
    }
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isColliding(pos, state));
    }
}

@Inject(method = "canSpawnSprintParticle()Z", at=@At("HEAD"),cancellable=true)
private void shouldSpawnSprintingParticlesIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canSpawnSprintParticle());
    }
}

@Inject(method = "canBeHitByProjectile()Z", at=@At("HEAD"),cancellable=true)
private void canBeHitByProjectileIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canBeHitByProjectile());
    }
}

@Inject(method = "isPickable()Z", at=@At("HEAD"),cancellable=true)
private void canHitIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isPickable());
    }
}

@Inject(method = "isPushable()Z", at=@At("HEAD"),cancellable=true)
private void isPushableIdentity(CallbackInfoReturnable info){

    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isPushable());
    }
}
@Inject(method = "isPushedByFluid()Z", at=@At("HEAD"),cancellable=true)
private void isPushedByFluidsIdentity(CallbackInfoReturnable info){

    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isPushedByFluid());
    }
}

@Inject(method = "isInWall()Z", at=@At("HEAD"),cancellable=true)
private void isInsideWallIdentity(CallbackInfoReturnable info){
    if(((Entity)(Object)this) instanceof Player){
        return;
    }
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.isInWall());
    }
}




@Inject(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at=@At("HEAD"),cancellable=true)
private void interactIdentity(Player player, InteractionHand hand, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        InteractionResult actionResult = this.currentIdentity.interact(player, hand);
        if (actionResult != InteractionResult.PASS) {
            info.setReturnValue(actionResult);
        }
    }
}

@Inject(method = "canCollideWith(Lnet/minecraft/world/entity/Entity;)Z", at=@At("HEAD"),cancellable=true)
private void collidesWithIdentity(Entity other, CallbackInfoReturnable info){
    if(((EntityAccessor)other).getIdentityOwner()!=null){
        info.setReturnValue(false);
        return;
    }
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canCollideWith(other));
    }
}

@Inject(method = "getMaxAirSupply()I", at=@At("HEAD"),cancellable=true)
private void getMaxAirIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getMaxAirSupply());
    }
}

@Inject(method = "getPercentFrozen()F", at=@At("HEAD"),cancellable=true)
private void getFreezingScaleIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getPercentFrozen());
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

@Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at=@At("HEAD"),cancellable=true)
public void writeDataLabelSaving(ValueOutput view,CallbackInfo info) {
    this.saving=true;
}
@Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at=@At("TAIL"),cancellable=true)
public void writeDataLabelDoneSaving(ValueOutput view,CallbackInfo info) {
    this.saving=false;
}



@Inject(method = "getEyeHeight()F", at=@At("HEAD"),cancellable=true)
private void getStandingEyeHeightIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getEyeHeight());
    }
}

@Inject(method = "getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;", at=@At("HEAD"),cancellable=true)
private void getPistonBehaviorIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getPistonPushReaction());
    }
}

@Inject(method = "canSprint()Z", at=@At("HEAD"),cancellable=true)
private void canSprintAsVehicleIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.canSprint());
    }
}

@Inject(method = "maxUpStep()F", at=@At("HEAD"),cancellable=true)
private void getStepHeightIdentity(CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.maxUpStep());
    }
}

@Inject(method = "resetFallDistance()V", at=@At("HEAD"))
private void onLandingIdentity(CallbackInfo info){
    if(this.currentIdentity!=null){
       this.currentIdentity.resetFallDistance();
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
