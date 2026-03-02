package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;

import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
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
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.checkonly.EntityMethodChecks;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.identity.IdentityTraitTags;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.Gabou.identity2.identity.IdentityProgression;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
@Mixin(Entity.class)
public class EntityMixin implements EntityAccessor{
    @Nullable
    @Unique
    private CustomData identity2$customDataView;

    @Nullable
    @Unique
    private static Constructor<CustomData> identity2$customDataCtor;

    @Nullable
    private CompoundTag persistentData;
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
//    @ModifyConstant(constant=@Constant(doubleValue=3.0E7),method="absSnapTo(DDD)V")
//    private static double TDIOA(double x){
//        return Identity2.maxWorldSize;
//    }
//    @ModifyConstant(constant=@Constant(doubleValue=-3.0E7),method="absSnapTo(DDD)V")
//    private static double TDIOB(double x){
//        return -Identity2.maxWorldSize;
//    }
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void moveOnEntityLandOverride(Block block, BlockGetter view,Entity entity){

        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double multiplier = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "land_speed_multiplier_override", Double.NaN);
        if (this.currentIdentity != null && !Double.isNaN(multiplier) && multiplier != 0.0D) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, multiplier, 1.0));
            return;
        }
        block.updateEntityMovementAfterFallOn(view,entity);
    }
	@Inject(method = "tick", at=@At("HEAD"))
	private void identityFixCanFlyCheck(CallbackInfo info) {
        //this.identity2$applyShulkerOpenVisualState();
        //this.identity2$applyMorphPassiveTraits();
        if(this.identityOf!=null){
            if(this.entityCanFlyTickEvaluated==false){
                this.entityCanFlyTickEvaluated=true;
                this.entityCanFlyEvaluated=false;
            }
            if(this.entityCanFlyEvaluated==false){
                //QualityCommands.LOGGER.info("Reevaluating canFly for entity "+((Entity)(Object)this).getName());
                    this.canFly();
                }
            this.identityOf.noPhysics=this.noPhysics;
        }
    }
    @Inject(method = "isInWall", at=@At("HEAD"),cancellable = true)
    protected void disableNoClipSuffocate(CallbackInfoReturnable info) {
		if(this.noPhysics){
            info.setReturnValue(false);
            return;
        }
        if ((Entity)(Object)this instanceof Player player) {
            Entity identity = ((EntityAccessor) player).getCurrentIdentity();
            if (
                identity != null
                    && player.isInWater()
                    && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(identity.getType()))
            ) {
                // Aquatic morphs at water/solid boundaries (e.g. under ice) can trigger false in-wall checks.
                info.setReturnValue(false);
                return;
            }
            if (
                (!((Entity)(Object)this).level().isClientSide() && IdentityProgression.isMorphDamageGraceActive(player))
                    || (identity != null && identity.getType() == EntityType.ENDER_DRAGON)
            ) {
                info.setReturnValue(false);
            }
        }
	}
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void identity2$ignoreCobwebSlowdownForSpiderMorphs(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if (!state.is(Blocks.COBWEB)) {
            return;
        }

        EntityType<?> hostType = ((Entity) (Object) this).getType();
        if (hostType == EntityType.SPIDER || hostType == EntityType.CAVE_SPIDER) {
            ci.cancel();
            return;
        }

        if (this.currentIdentity == null) {
            return;
        }
        EntityType<?> identityType = this.currentIdentity.getType();
        if (identityType == EntityType.SPIDER || identityType == EntityType.CAVE_SPIDER) {
            ci.cancel();
        }
    }
	@Inject(method = "tick", at=@At("RETURN"))
	private void identityFix(CallbackInfo info) {
		if(this.currentIdentity!=null){
            boolean hostIsPlayer = ((Entity)(Object)this) instanceof Player;
             
              
            //this.currentIdentity.setInvulnerable(hostIsPlayer);
            this.currentIdentity.setPos(this.position());
            this.currentIdentity.setDeltaMovement(this.getDeltaMovement());
            this.currentIdentity.setAirSupply(this.getAirSupply());
            if(
                (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                ((Entity)(Object)this instanceof LivingEntity livingEntity)
            ){
            livingIdentity.setHealth(livingEntity.getHealth());
            }
            if(!this.currentIdentity.level().isClientSide()){
                if(this.currentIdentity instanceof Mob mobIdentity){
                    mobIdentity.setNoAi(true);
                }
                this.currentIdentity.tick();
                //if(this.currentIdentity instanceof MobEntity mobIdentity){
                //    mobIdentity.setAiDisabled(false);
                //}
            }
             
            
            this.setPos(this.currentIdentity.position());
            this.setDeltaMovement(this.currentIdentity.getDeltaMovement());
            this.setAirSupply(this.currentIdentity.getAirSupply());
            if(
                (this.currentIdentity instanceof LivingEntity livingIdentity)&&
                ((Entity)(Object)this instanceof LivingEntity livingEntity)
            ){
                // Do not mirror transient identity damage back into players (prevents login hurt ticks/sounds).
                livingEntity.setHealth(livingIdentity.getHealth());
                
            }
        
             
        }
	}
    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private void moveOnEntityLandWallOverride(Entity entity,double x,double y,double z, @Local(ordinal=0) boolean bl, @Local(ordinal=1) boolean bl2, @Local(ordinal=2) Vec3 vec3d4){
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double multiplier = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "horizontal_collision_speed_multiplier_override", Double.NaN);
        if (this.currentIdentity != null && !Double.isNaN(multiplier) && multiplier != 0.0D) {
            entity.setDeltaMovement(bl ? vec3d4.x * multiplier : vec3d4.x, vec3d4.y, bl2 ? vec3d4.z * multiplier : vec3d4.z);
            return;
        }
        entity.setDeltaMovement(x,y,z);
    }







    public int abilityCooldown=0;
    public int secondaryAbilityCooldown=0;

    public int getAbilityCooldown(){
        return this.abilityCooldown;
    }
    public void setAbilityCooldown(int cooldown){
        this.abilityCooldown=cooldown;
    }

    public int getSecondaryAbilityCooldown() {
        return this.secondaryAbilityCooldown;
    }

    public void setSecondaryAbilityCooldown(int cooldown) {
        this.secondaryAbilityCooldown = cooldown;
    }



















    @Inject(method="baseTick",at=@At("HEAD"),cancellable=true)
    private void identity2$baseTick(CallbackInfo info){
        if(this.abilityCooldown>0){
            this.abilityCooldown-=1;
        }
        if(this.secondaryAbilityCooldown>0){
            this.secondaryAbilityCooldown-=1;
        }
        if ((Entity)(Object)this instanceof ServerPlayer serverPlayer) {
            IdentityProgression.tickDailyRandomMorph(serverPlayer);
        }
    }
    



    @Shadow
    public boolean noPhysics=false;
    @Shadow
    public boolean horizontalCollision;
    
    public boolean entityCanFly=false;
    public boolean entityCanFlyEvaluated=false;
    public boolean entityCanFlyTickEvaluated=false;
    private boolean identity2$grantedMayfly = false;
    private long entityCanFlyLastEvalTick = Long.MIN_VALUE;
    private static final long ENTITY_FLY_REEVAL_TICKS = 20L;
    private static final String FALL_METHOD_NAME = identity2$resolveFallMethodName();
    


    public boolean canFly(){
        if(!this.entityCanFlyEvaluated){
            Boolean taggedFlight = IdentityTraitTags.resolveFlight(((Entity)(Object)this).getType());
            if (taggedFlight != null) {
                this.entityCanFly = taggedFlight;
            } else {
                try {
                    this.entityCanFly = net.Gabou.identity2.util.MFCheck.isMethodEmpty(((Object)this).getClass(), FALL_METHOD_NAME);
                } catch (Exception ignored) {
                }
                if (!this.shouldTickBlockCollision()) {
                    this.entityCanFly = true;
                }
                if (this.noPhysics) {
                    this.entityCanFly = true;
                }
            }
            this.entityCanFlyEvaluated = true;
            if (this.identityOf instanceof Player player) {
                ((EntityMixin)(Object)player).applyIdentityFlightGrant(player, this.entityCanFly);
            }
        }
        return this.entityCanFly;
    }
        private static String identity2$resolveFallMethodName() {
        try {
            return EntityMethodChecks.class
                .getDeclaredMethod("checkFallDamage", double.class, boolean.class, BlockState.class, BlockPos.class)
                .getName();
        } catch (NoSuchMethodException ignored) {
            return "checkFallDamage";
        }
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






    @Inject(method="isControlledByClient",at=@At("HEAD"),cancellable=true)
    private void isControlledByPlayerOverride(CallbackInfoReturnable info){
        if(this.identityOf!=null){
            info.setReturnValue(((Entity)this.identityOf).isControlledByClient());
        }
    }
    
    @Inject(method="getBbWidth",at=@At("HEAD"),cancellable=true)
    private void getWidthOverride(CallbackInfoReturnable info){
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double override = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (override > 0.0D) {
            info.setReturnValue((float) override);
        }
    }
    @Inject(method="getDimensions",at=@At("RETURN"),cancellable=true)
    private void getDimensionsModification(CallbackInfoReturnable info){
        EntityDimensions dimensions = (EntityDimensions) info.getReturnValue();
        float oldWidth = dimensions.width();
        float oldHeight = dimensions.height();
        float widthOverride = oldWidth;
        float heightOverride = oldHeight;

        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double widthValue = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (widthValue > 0.0D) {
            widthOverride = (float) widthValue;
        }
        double heightValue = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (heightValue > 0.0D) {
            heightOverride = (float) heightValue;
        }

        float widthScale = oldWidth > 0.0F ? widthOverride / oldWidth : 1.0F;
        float heightScale = oldHeight > 0.0F ? heightOverride / oldHeight : 1.0F;
        info.setReturnValue(dimensions.scale(widthScale, heightScale));
    }
    @Inject(method="getBbHeight",at=@At("HEAD"),cancellable=true)
    private void getHeightOverride(CallbackInfoReturnable info){
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double override = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (override > 0.0D) {
            info.setReturnValue((float) override);
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

        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        double widthOverride = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (widthOverride > 0.0D) {
            new_width = widthOverride;
            hasOverride = true;
        }
        double heightOverride = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (heightOverride > 0.0D) {
            new_height = heightOverride;
            hasOverride = true;
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
    public CustomData getCustomData(){
        if (this.persistentData == null) {
            this.persistentData = new CompoundTag();
        }
        CompoundTag persistentTag = this.persistentData;
        if (
            this.identity2$customDataView == null
                || ((NbtComponentAccessor) (Object) this.identity2$customDataView).getNbt() != persistentTag
        ) {
            this.identity2$customDataView = identity2$wrapTagReference(persistentTag);
        }
        return this.identity2$customDataView;
    };

    @Unique
    private static CustomData identity2$wrapTagReference(CompoundTag tag) {
        Constructor<CustomData> ctor = identity2$customDataCtor;
        if (ctor == null) {
            try {
                ctor = CustomData.class.getDeclaredConstructor(CompoundTag.class);
                ctor.setAccessible(true);
                identity2$customDataCtor = ctor;
            } catch (Throwable ignored) {
                return CustomData.of(tag);
            }
        }
        try {
            return ctor.newInstance(tag);
        } catch (Throwable ignored) {
            return CustomData.of(tag);
        }
    }

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
        this.identity2$clearTransientMovementOverrides();
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
            CompoundTag dataNbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
            String variantRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(dataNbt, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
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
            }
            return;
        }
        ResourceLocation identityId;
        try {
            identityId = ResourceLocation.parse(id);
        } catch (Exception e) {
            this.deactivateIdentityAfterFailure(null, "invalid id");
            return;
        }
        if (IdentityProgression.PLAYER_IDENTITY_ID.equals(identityId)) {
            this.currentIdentity = null;
            this.entityCanFly = false;
            ((Entity)(Object)this).refreshDimensions();
            this.setStandingEyeHeight(((Entity)(Object)this).getEyeHeight());
            if((Entity)(Object)this instanceof Player player){
                this.applyIdentityFlightGrant(player, false);
            }
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
                entityx.moveTo(pos.x, pos.y, pos.z, entityx.getYRot(), entityx.getXRot());
                return entityx;
            });
            if (entity == null) {
                throw new IllegalStateException("loadEntityWithPassengers returned null");
            }
            entity.setId(-this.getId());
            this.identity2$applyIdentityVariantState(entity, nbtCompound);
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
            }
        }
        
    }

    private void identity2$applyIdentityVariantState(Entity identityEntity, CompoundTag variantNbt) {
        if (identityEntity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        IdentityVariantNbtHelper.applyVariantData(identityEntity, variantNbt);

        boolean hasBabyFlag = variantNbt.contains("IsBaby", net.minecraft.nbt.Tag.TAG_BYTE) || variantNbt.contains("Baby", net.minecraft.nbt.Tag.TAG_BYTE);
        if (hasBabyFlag) {
            boolean baby = net.Gabou.identity2.util.NbtCompat.getBooleanOr(variantNbt, "IsBaby", net.Gabou.identity2.util.NbtCompat.getBooleanOr(variantNbt, "Baby", false));
            identity2$invokeOneArg(identityEntity, "setBaby", baby);
        }
        if (variantNbt.contains("Age", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            int age = variantNbt.getInt("Age");
            identity2$invokeIntArg(identityEntity, "setAge", age);
            if (age < 0) {
                identity2$invokeOneArg(identityEntity, "setBaby", true);
            }
        }
        if (variantNbt.contains("AgeLocked", net.minecraft.nbt.Tag.TAG_BYTE)) {
            identity2$invokeOneArg(identityEntity, "setAgeLocked", net.Gabou.identity2.util.NbtCompat.getBooleanOr(variantNbt, "AgeLocked", false));
        }

        if (variantNbt.contains("Variant", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            identity2$invokeIntArg(identityEntity, "setVariant", variantNbt.getInt("Variant"));
        }
        if (variantNbt.contains("variant", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            identity2$invokeIntArg(identityEntity, "setVariant", variantNbt.getInt("variant"));
        }
        if (variantNbt.contains("Type", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            int type = variantNbt.getInt("Type");
            if (identity2$invokeIntArg(identityEntity, "setType", type) == null) {
                identity2$invokeIntArg(identityEntity, "setVariant", type);
            }
        }
        if (variantNbt.contains("type", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            int type = variantNbt.getInt("type");
            if (identity2$invokeIntArg(identityEntity, "setType", type) == null) {
                identity2$invokeIntArg(identityEntity, "setVariant", type);
            }
        }

        identity2$applyRegistryBackedVariant(identityEntity, variantNbt, "CatVariant", "CAT_VARIANT");
        identity2$applyRegistryBackedVariant(identityEntity, variantNbt, "WolfVariant", "WOLF_VARIANT");
        identity2$applyRegistryBackedVariant(identityEntity, variantNbt, "FrogVariant", "FROG_VARIANT");

        if (variantNbt.contains("CollarColor", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            Object dyeColor = identity2$resolveDyeColorById(variantNbt.getInt("CollarColor"));
            if (dyeColor != null) {
                identity2$invokeOneArg(identityEntity, "setCollarColor", dyeColor);
            }
        }

        identity2$applyVillagerVariantState(identityEntity, variantNbt);
    }

    private void identity2$applyVillagerVariantState(Entity identityEntity, CompoundTag variantNbt) {
        if (identityEntity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        Object villagerData = identity2$invokeNoArg(identityEntity, "getVillagerData");
        if (villagerData == null) {
            return;
        }

        CompoundTag villagerDataTag = net.Gabou.identity2.util.NbtCompat.getCompoundOrNull(variantNbt, "VillagerData");
        String professionRaw = identity2$readVariantString(variantNbt, "VillagerProfession", "Profession", "profession");
        if ((professionRaw == null || professionRaw.isBlank()) && villagerDataTag != null) {
            professionRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(villagerDataTag, "profession", "");
        }
        String typeRaw = identity2$readVariantString(variantNbt, "VillagerType", "Type", "type");
        if ((typeRaw == null || typeRaw.isBlank()) && villagerDataTag != null) {
            typeRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(villagerDataTag, "type", "");
        }

        ResourceLocation professionId = identity2$parseResourceLocation(professionRaw);
        ResourceLocation typeId = identity2$parseResourceLocation(typeRaw);

        if (professionId != null) {
            Object profession = identity2$resolveRegistryValue("VILLAGER_PROFESSION", professionId);
            if (profession != null) {
                Object professionArg = profession;
                Object professionRegistry = identity2$getBuiltInRegistryObject("VILLAGER_PROFESSION");
                Object wrapped = identity2$wrapAsHolder(professionRegistry, profession);
                if (wrapped != null) {
                    professionArg = wrapped;
                }
                Object updatedVillagerData = identity2$invokeOneArg(villagerData, "setProfession", professionArg);
                if (updatedVillagerData != null) {
                    villagerData = updatedVillagerData;
                }
            }
        }

        if (typeId != null) {
            Object villagerType = identity2$resolveRegistryValue("VILLAGER_TYPE", typeId);
            if (villagerType != null) {
                Object typeArg = villagerType;
                Object typeRegistry = identity2$getBuiltInRegistryObject("VILLAGER_TYPE");
                Object wrapped = identity2$wrapAsHolder(typeRegistry, villagerType);
                if (wrapped != null) {
                    typeArg = wrapped;
                }
                Object updatedVillagerData = identity2$invokeOneArg(villagerData, "setType", typeArg);
                if (updatedVillagerData != null) {
                    villagerData = updatedVillagerData;
                }
            }
        }

        int level = 0;
        if (variantNbt.contains("VillagerLevel", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            level = variantNbt.getInt("VillagerLevel");
        } else if (villagerDataTag != null && villagerDataTag.contains("level", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            level = villagerDataTag.getInt("level");
        }
        if (level > 0) {
            Object updatedVillagerData = identity2$invokeIntArg(villagerData, "setLevel", Math.max(1, level));
            if (updatedVillagerData != null) {
                villagerData = updatedVillagerData;
            }
        }

        if (identity2$invokeOneArg(identityEntity, "setVillagerData", villagerData) != null) {
            identity2$clearVillagerOffers(identityEntity);
        }
    }

    private static void identity2$clearVillagerOffers(Object villager) {
        if (villager == null) {
            return;
        }
        try {
            Class<?> offersClass = Class.forName("net.minecraft.world.item.trading.MerchantOffers");
            Object offers = offersClass.getConstructor().newInstance();
            if (identity2$invokeOneArg(villager, "setOffers", offers) != null) {
                return;
            }
        } catch (Throwable ignored) {
        }
        identity2$invokeNoArg(villager, "resetOffers");
    }

    @Nullable
    private static ResourceLocation identity2$parseResourceLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.contains(":")) {
                return ResourceLocation.parse(raw);
            }
            return ResourceLocation.fromNamespaceAndPath("minecraft", raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object identity2$resolveRegistryValue(String registryField, @Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Object registry = identity2$getBuiltInRegistryObject(registryField);
        if (registry instanceof net.minecraft.core.Registry<?> rawRegistry) {
            @SuppressWarnings("unchecked")
            net.minecraft.core.Registry<Object> cast = (net.minecraft.core.Registry<Object>) rawRegistry;
            return cast.getValue(id);
        }
        return null;
    }

    private static void identity2$applyRegistryBackedVariant(Entity identityEntity, CompoundTag variantNbt, String nbtKey, String registryField) {
        String raw = identity2$readVariantString(variantNbt, nbtKey, "variant", "Variant");
        ResourceLocation variantId = identity2$parseResourceLocation(raw);
        if (variantId == null) {
            return;
        }
        Object variant = identity2$resolveRegistryValue(registryField, variantId);
        if (variant == null) {
            return;
        }

        Object arg = variant;
        Object registry = identity2$getBuiltInRegistryObject(registryField);
        Object wrapped = identity2$wrapAsHolder(registry, variant);
        if (wrapped != null) {
            arg = wrapped;
        }

        if (identity2$invokeOneArg(identityEntity, "setVariant", arg) == null) {
            identity2$invokeOneArg(identityEntity, "setType", arg);
        }
    }

    @Nullable
    private static Object identity2$getBuiltInRegistryObject(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            return BuiltInRegistries.class.getField(fieldName).get(null);
        } catch (Throwable ignored) {
        }

        // 1.21.8: some registries (cat/wolf/frog variants) are exposed as keys in Registries
        // rather than direct BuiltInRegistries fields.
        try {
            Object registryKeyObj = Registries.class.getField(fieldName).get(null);
            if (!(registryKeyObj instanceof net.minecraft.resources.ResourceKey<?> registryKey)) {
                return null;
            }
            ResourceLocation location = registryKey.location();
            if (location == null || BuiltInRegistries.REGISTRY == null) {
                return null;
            }
            return BuiltInRegistries.REGISTRY.getValue(location);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String identity2$readVariantString(CompoundTag variantNbt, String... keys) {
        if (variantNbt == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!variantNbt.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                continue;
            }
            String value = net.Gabou.identity2.util.NbtCompat.getStringOr(variantNbt, key, "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    @Nullable
    private static Object identity2$resolveDyeColorById(int colorId) {
        try {
            Class<?> dyeColorClass = Class.forName("net.minecraft.world.item.DyeColor");
            for (Method method : dyeColorClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!method.getName().equals("byId") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (type == int.class || type == Integer.class) {
                    return method.invoke(null, Math.max(0, colorId));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object identity2$wrapAsHolder(Object registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        for (Method method : registry.getClass().getMethods()) {
            if (!method.getName().equals("wrapAsHolder") || method.getParameterCount() != 1) {
                continue;
            }
            if (!identity2$isAssignable(method.getParameterTypes()[0], value.getClass())) {
                continue;
            }
            try {
                return method.invoke(registry, value);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (arg != null && !identity2$isAssignable(paramType, arg.getClass())) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, arg);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeIntArg(Object target, String methodName, int value) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!(paramType == int.class || paramType == Integer.class)) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, value);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static List<Method> identity2$getAllMethods(Class<?> type) {
        List<Method> methods = Lists.newArrayList();
        Set<String> signatures = new java.util.LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                for (Class<?> parameterType : method.getParameterTypes()) {
                    signature += ":" + parameterType.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static boolean identity2$isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType == int.class && argType == Integer.class) {
            return true;
        }
        if (paramType == boolean.class && argType == Boolean.class) {
            return true;
        }
        if (paramType == byte.class && argType == Byte.class) {
            return true;
        }
        if (paramType == short.class && argType == Short.class) {
            return true;
        }
        if (paramType == long.class && argType == Long.class) {
            return true;
        }
        if (paramType == float.class && argType == Float.class) {
            return true;
        }
        if (paramType == double.class && argType == Double.class) {
            return true;
        }
        return false;
    }

    private void deactivateIdentityAfterFailure(@Nullable ResourceLocation identityId, String reason) {
        this.currentIdentity = null;
        this.entityCanFly = false;
        this.entityCanFlyEvaluated = false;
        this.entityCanFlyTickEvaluated = false;
        this.entityCanFlyLastEvalTick = Long.MIN_VALUE;
        this.identity2$clearTransientMovementOverrides();
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        nbt.putString("model_override", "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        nbt.putDouble(IdentityProgression.MORPH_DAMAGE_GRACE_END_TICK_KEY, 0.0D);
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
    @Unique
    private void identity2$clearTransientMovementOverrides() {
        CompoundTag nbt = ((NbtComponentAccessor) (Object) this.getCustomData()).getNbt();
        nbt.putDouble("land_speed_multiplier_override", 0.0D);
        nbt.putDouble("horizontal_collision_speed_multiplier_override", 0.0D);
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
    if(this.currentIdentity!=null && !(((Entity)(Object)this) instanceof Player) && !this.currentIdentity.isRemoved()){
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

@Inject(method = "lerpTo(DDDFFI)V", at=@At("HEAD"))
private void identity2$forwardLerpTo(
    double x,
    double y,
    double z,
    float yRot,
    float xRot,
    int interpolationSteps,
    CallbackInfo info
){
    if (this.currentIdentity != null) {
        this.currentIdentity.lerpTo(x, y, z, yRot, xRot, interpolationSteps);
    }
}

@Inject(method = "cancelLerp()V", at=@At("HEAD"))
private void identity2$forwardCancelLerp(CallbackInfo info){
    if (this.currentIdentity != null) {
        this.currentIdentity.cancelLerp();
    }
}

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void identity2$canBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {

        if (this.currentIdentity != null) {
            cir.setReturnValue(this.currentIdentity.canBeCollidedWith());
            return;
        }

        if ((Object)this instanceof Entity self) {
            try {
                if (((EntityAccessor) self).getIdentityOwner() != null) {
                    cir.setReturnValue(false);
                }
            } catch (Exception ignored) {}
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

@Inject(method = "saveWithoutId", at=@At("HEAD"),cancellable=true, require = 0)
public void writeDataLabelSaving(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
    this.saving=true;
}
@Inject(method = "saveWithoutId", at=@At("TAIL"),cancellable=true, require = 0)
public void writeDataLabelDoneSaving(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
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



