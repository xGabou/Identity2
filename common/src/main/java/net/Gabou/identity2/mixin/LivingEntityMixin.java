package net.Gabou.identity2.mixin;

import com.google.common.collect.Lists;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.world.phys.AABB;
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

import java.util.Locale;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.Gabou.identity2.util.AttributeContainerAccessor;
import net.Gabou.identity2.util.DefaultAttributeContainerAccessor;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.SilverfishBurrowManager;
import net.Gabou.identity2.identity.IdentityTraitTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements LivingEntityAccessor {
    @Unique
    private static final String IDENTITY2_LAST_AMBIENT_SOUND_TICK_KEY = "identity2.last_ambient_sound_tick";

    @Shadow
    protected boolean jumping;

    @Shadow
    @Nullable
    public abstract SoundEvent getDeathSound();

    @Shadow
    protected abstract float getSoundVolume();

    @Override
    public boolean identity2$isJumping() {
        return this.jumping;
    }

    @Mutable
    @Shadow
    private AttributeMap attributes;

    public void fixAttributes(Entity entity, Entity identity) {
        if (entity instanceof Player) {
            return;
        }
        if (identity instanceof LivingEntity livingIdentity) {
            // Keep the morph entity on its own attribute map so the mob's intended
            // movement and combat values are preserved. Player-side morph attributes
            // are handled separately in IdentityProgression.
            this.attributes = livingIdentity.getAttributes();
        }

    }


    public AttributeMap createMangled(AttributeMap a, AttributeMap b) {
        AttributeSupplier.Builder builder = AttributeSupplier.builder();
        for (AttributeInstance attr : ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) a).getDefaultAttributes()).getInstances().values()) {
            builder.add(attr.getAttribute(), attr.getBaseValue());
        }
        for (AttributeInstance attr : ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) b).getDefaultAttributes()).getInstances().values()) {
            builder.add(attr.getAttribute(), attr.getBaseValue());
        }
        AttributeMap newContainer = new AttributeMap(builder.build());
        identity2$assignAllValues(newContainer, a);
        identity2$assignAllValues(newContainer, b);
        identity2$assignBaseValues(newContainer, a);
        identity2$assignBaseValues(newContainer, b);
        /*for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)newContainer).getDefaultAttributes()).getInstances().values()){
            Identity2.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(newContainer.getValue(attr.getAttribute())));
        }*/
        return newContainer;
    }

    private static void identity2$assignAllValues(AttributeMap target, AttributeMap source) {
        if (target == null || source == null) {
            return;
        }
        if (identity2$invokeAttributeMapCopy(target, "assignAllValues", source)) {
            return;
        }
        identity2$invokeAttributeMapCopy(target, "assignValues", source);
    }

    private static void identity2$assignBaseValues(AttributeMap target, AttributeMap source) {
        if (target == null || source == null) {
            return;
        }
        identity2$invokeAttributeMapCopy(target, "assignBaseValues", source);
    }

    private static boolean identity2$invokeAttributeMapCopy(AttributeMap target, String methodName, AttributeMap source) {
        for (Method method : AttributeMap.class.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (!param.isAssignableFrom(AttributeMap.class) && !AttributeMap.class.isAssignableFrom(param)) {
                continue;
            }
            try {
                method.invoke(target, source);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean identity2$isAquaticMorph(LivingEntity livingIdentity) {
        return livingIdentity != null
            && (
                livingIdentity.canBreatheUnderwater()
                    || Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(livingIdentity.getType()))
            );
    }

    private static boolean identity2$canUseSlot(LivingEntity livingIdentity, EquipmentSlot slot) {
        if (livingIdentity == null || slot == null) {
            return true;
        }
        try {
            return (boolean) LivingEntity.class
                .getMethod("canUseSlot", EquipmentSlot.class)
                .invoke(livingIdentity, slot);
        } catch (Throwable ignored) {
            return true;
        }
    }
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
        if ((Entity)(Object)this instanceof Player) {
            return;
        }
        try {
            if(!this.saving && this.currentIdentity instanceof LivingEntity livingIdentity){
                info.setReturnValue(livingIdentity.getAttributes());
            }
        } catch (Exception ignored){
        }
    }

    @Inject(method = "decreaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void getNextAirUnderwaterIdentity(int air, CallbackInfoReturnable info) {
        if (!(this.currentIdentity instanceof LivingEntity livingIdentity)) {
            return;
        }

        LivingEntity host = (LivingEntity) (Object) this;
        if (host.isInWaterOrBubble() && identity2$isAquaticMorph(livingIdentity)) {
            info.setReturnValue(host.getMaxAirSupply());
        }
    }

    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void getNextAirOnLandIdentity(int air, CallbackInfoReturnable info) {
        if (!(this.currentIdentity instanceof LivingEntity livingIdentity)) {
            return;
        }

        LivingEntity host = (LivingEntity) (Object) this;
        if (host.isInWaterOrBubble()) {
            return;
        }

        if (identity2$isAquaticMorph(livingIdentity)) {
            int nextAir = air - 1;
            if (livingIdentity.getType() == EntityType.DOLPHIN && air > 0 && nextAir <= 0) {
                host.hurt(host.damageSources().dryOut(), 2.0F);
                info.setReturnValue(0);
                return;
            }
            if (nextAir <= -20) {
                nextAir = 0;
                host.hurt(host.damageSources().dryOut(), 2.0F);
            }
            info.setReturnValue(nextAir);
            return;
        }

        info.setReturnValue(host.getMaxAirSupply());
    }

    @Inject(method = "isInvertedHealAndHarm()Z", at = @At("HEAD"), cancellable = true)
    private void hasInvertedHealingAndHarmIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.isInvertedHealAndHarm());
            }
        }
    }

    @Inject(method = "canBreatheUnderwater()Z", at = @At("HEAD"), cancellable = true)
    private void canBreatheInWaterIdentity(CallbackInfoReturnable<Boolean> info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(identity2$isAquaticMorph(livingIdentity));
            }
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void identity2$disableFallDamageForFlyingMorphs(float distance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Entity) (Object) this instanceof Player)) {
            return;
        }
        if (this.currentIdentity != null && ((EntityAccessor) this.currentIdentity).canFly()) {
            cir.setReturnValue(false);
            return;
        }
        if (this.currentIdentity != null && this.currentIdentity.getType() == EntityType.CAMEL) {
            cir.setReturnValue(false);
            return;
        }
        if (this.currentIdentity != null
                && (this.currentIdentity.getType() == EntityType.CAT
                || this.currentIdentity.getType() == EntityType.IRON_GOLEM)) {
            cir.setReturnValue(false);
        }
    }


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

@Inject(method = "playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
private void identity2$playIdentityHurtSound(DamageSource source, CallbackInfo info) {
    if (identity2$playIdentityHurtSoundInternal(source)) {
        info.cancel();
    }
}

@Redirect(
        method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;playSecondaryHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"
        )
)
private void identity2$redirectSecondaryHurtSound(LivingEntity self, DamageSource source) {
    if (identity2$playIdentityHurtSoundInternal(source)) {
        return;
    }
    identity2$invokePrivateVoid(self, "playSecondaryHurtSound", new Class<?>[] { DamageSource.class }, new Object[] { source });
}

@Unique
private boolean identity2$playIdentityHurtSoundInternal(DamageSource source) {
    if (!IdentitySettings.useIdentitySounds || this.currentIdentity == null) {
        return false;
    }
    if (!(this.currentIdentity instanceof LivingEntity)) {
        return false;
    }
    SoundEvent hurtSound = ((LivingEntityAccessor) this.currentIdentity).getHurtSound(source);
    if (hurtSound == null) {
        return false;
    }
    LivingEntity self = (LivingEntity) (Object) this;
    float pitch = 1.0F;
    Object pitchValue = identity2$invokeNoArg(this.currentIdentity, "getVoicePitch");
    if (pitchValue instanceof Number number) {
        pitch = number.floatValue();
    }
    self.level().playSound(
            null,
            self.getX(),
            self.getY(),
            self.getZ(),
            hurtSound,
            self.getSoundSource(),
            this.getSoundVolume(),
            pitch
    );
    return true;
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
                mobIdentity.setNoAi(livingIdentity.getType() == EntityType.ENDER_DRAGON);
            }
            this.setPos(livingIdentity.position());
            this.setDeltaMovement(livingIdentity.getDeltaMovement());
            //info.cancel();
        }
    }
}

@Inject(method = "aiStep()V", at = @At("TAIL"))
private void identity2$playAmbientSound(CallbackInfo info) {
    if (!IdentitySettings.useIdentitySounds || !IdentitySettings.playAmbientSounds) {
        return;
    }
    if (!(this.currentIdentity instanceof LivingEntity livingIdentity)) {
        return;
    }
    if (!((Entity) (Object) this instanceof Player hostPlayer) || !(hostPlayer.level() instanceof ServerLevel serverLevel)) {
        return;
    }

    if (!(livingIdentity instanceof Mob mobIdentity)) {
        return;
    }

    SoundEvent ambientSound = ((MobAccessor) mobIdentity).identity2$invokeGetAmbientSound();
    if (ambientSound == null) {
        return;
    }

    int interval = 120;
    Object intervalValue = identity2$invokeNoArg(livingIdentity, "getAmbientSoundInterval");
    if (intervalValue instanceof Number number) {
        interval = Math.max(1, number.intValue());
    }
    if (serverLevel.getRandom().nextInt(interval) != 0) {
        return;
    }
    CompoundTag customData = ((EntityAccessor) hostPlayer).getCustomData();
    long lastAmbientTick = customData.getLong(IDENTITY2_LAST_AMBIENT_SOUND_TICK_KEY);
    if (lastAmbientTick > 0L && hostPlayer.tickCount - lastAmbientTick < 20) {
        return;
    }
    customData.putLong(IDENTITY2_LAST_AMBIENT_SOUND_TICK_KEY, hostPlayer.tickCount);

    float volume = 1.0F;
    Object volumeValue = identity2$invokeNoArg(livingIdentity, "getSoundVolume");
    if (volumeValue instanceof Number number) {
        volume = number.floatValue();
    }

    float pitch = 1.0F;
    Object pitchValue = identity2$invokeNoArg(livingIdentity, "getSoundPitch");
    if (pitchValue instanceof Number number) {
        pitch = number.floatValue();
    }

    serverLevel.playSound(
            IdentitySettings.hearSelfAmbient ? null : hostPlayer,
            hostPlayer.blockPosition(),
            ambientSound,
            mobIdentity.getSoundSource(),
            volume,
            pitch
    );
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

@Inject(method = "getLastHurtByMobTimestamp()I", at=@At("HEAD"),cancellable=true)
private void getPlayerHitTimerIdentity(CallbackInfoReturnable<Integer> info){
    if(this.identityOf!=null){
        if(this.identityOf instanceof LivingEntity livingIdentity){
            info.setReturnValue(livingIdentity.getLastHurtByMobTimestamp());
        }
    }
}





    @Unique
    private static boolean identity2$shouldIgnoreMorphSuffocation(Player player, Entity activeIdentity) {
        float idHeight = activeIdentity.getBbHeight();
        if (idHeight >= 1.2f) {
            return false;
        }

        if (player.isCrouching() || player.isSwimming()) {
            return false;
        }

        AABB box = player.getBoundingBox();

        AABB feet = new AABB(
                box.minX, box.minY, box.minZ,
                box.maxX, box.minY + 0.35, box.maxZ
        );

        double headStart = box.maxY - 0.35;
        AABB head = new AABB(
                box.minX, headStart, box.minZ,
                box.maxX, box.maxY, box.maxZ
        );

        boolean feetCollide = !player.level().noCollision(player, feet);
        boolean headCollide = !player.level().noCollision(player, head);

        return headCollide && !feetCollide;
    }

@Unique
private static boolean identity2$isWallCollisionDamage(DamageSource source) {
    String msgId = identity2$getDamageMessageId(source);
    if (msgId == null || msgId.isBlank()) {
        return false;
    }
    String normalized = msgId.trim().toLowerCase(Locale.ROOT).replace("-", "_");
    return normalized.equals("inwall")
        || normalized.equals("in_wall")
        || normalized.equals("flyintowall")
        || normalized.equals("fly_into_wall")
        || normalized.equals("cramming");
}

@Unique
private static boolean identity2$isFallDamage(DamageSource source) {
    if (source == null) {
        return false;
    }
    if (source.is(DamageTypes.FALL)) {
        return true;
    }
    if (source.is(DamageTypeTags.IS_FALL)) {
        return true;
    }
    String msgId = identity2$getDamageMessageId(source);
    if (msgId == null || msgId.isBlank()) {
        return false;
    }
    String normalized = msgId.trim().toLowerCase(Locale.ROOT).replace("-", "_");
    return normalized.equals("fall");
}

@Unique
private static String identity2$getDamageMessageId(DamageSource source) {
    if (source == null) {
        return "";
    }
    Object direct = identity2$invokeNoArg(source, "getMsgId");
    if (direct instanceof String text && !text.isBlank()) {
        return text;
    }
    Object type = identity2$invokeNoArg(source, "type");
    Object fromType = identity2$invokeNoArg(type, "msgId");
    if (fromType instanceof String text && !text.isBlank()) {
        return text;
    }
    Object holder = identity2$invokeNoArg(source, "typeHolder");
    Object value = identity2$invokeNoArg(holder, "value");
    Object fromHolder = identity2$invokeNoArg(value, "msgId");
    if (fromHolder instanceof String text && !text.isBlank()) {
        return text;
    }
    return "";
}

@Unique
private static Object identity2$invokeNoArg(Object target, String methodName) {
    if (target == null || methodName == null || methodName.isBlank()) {
        return null;
    }
    try {
        Method method = target.getClass().getMethod(methodName);
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }
        return method.invoke(target);
    } catch (Throwable ignored) {
        return null;
    }
}

@Unique
private static void identity2$invokePrivateVoid(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
    if (target == null || methodName == null || methodName.isBlank()) {
        return;
    }
    Class<?> current = target.getClass();
    while (current != null) {
        try {
            Method method = current.getDeclaredMethod(methodName, parameterTypes);
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            method.invoke(target, args);
            return;
        } catch (NoSuchMethodException e) {
            current = current.getSuperclass();
        } catch (Throwable ignored) {
            return;
        }
    }
}

//@Inject(method = "canUseSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z", at=@At("HEAD"),cancellable=true)
//private void canUseSlotIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
//    if(this.currentIdentity!=null){
//        if(this.currentIdentity instanceof LivingEntity livingIdentity){
//            if (slot.getType() == EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipItems) {
//                info.setReturnValue(false);
//                return;
//            }
//            if (slot.getType() != EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipArmor) {
//                info.setReturnValue(false);
//                return;
//            }
//            info.setReturnValue(identity2$canUseSlot(livingIdentity, slot));
//        }
//    }
//}

    @Inject(method = "getLastHurtByMobTimestamp", at = @At("HEAD"), cancellable = true)
    private void identity2$getLastHurtTimestamp(CallbackInfoReturnable<Integer> cir) {
        if (this.identityOf instanceof LivingEntity livingIdentity) {
            cir.setReturnValue(livingIdentity.getLastHurtByMobTimestamp());
        }
    }




    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void pushAwayFromIdentity(Entity entity, CallbackInfo info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity == entity) {
                info.cancel();
            }
        }
    }


    @Inject(method = "getMainHandItem()Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getMainHandItemIdentity(CallbackInfoReturnable<ItemStack> info) {
    }

    @Inject(method = "getOffhandItem()Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getOffhandItemIdentity(CallbackInfoReturnable<ItemStack> info) {
    }

    @Inject(method = "hasItemInSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z", at = @At("HEAD"), cancellable = true)
    private void identity2$hasItemInSlotIdentity(EquipmentSlot slot, CallbackInfoReturnable<Boolean> info) {
        if (slot.getType() == EquipmentSlot.Type.HAND) {
            return;
        }
        if (slot.getType() == EquipmentSlot.Type.HAND && IdentitySettings.identitiesEquipItems) {
            return;
        }
        if (slot.getType() != EquipmentSlot.Type.HAND && IdentitySettings.identitiesEquipArmor) {
            return;
        }
        if (this.currentIdentity instanceof LivingEntity livingIdentity && !identity2$canUseSlot(livingIdentity, slot)) {
            info.setReturnValue(false);
        }
    }

//    @Inject(method = "canUseSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z", at = @At("HEAD"), cancellable = true)
//    private void canUseSlotIdentity(EquipmentSlot slot, CallbackInfoReturnable info) {
//        if (this.currentIdentity != null) {
//            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
//                info.setReturnValue(identity2$canUseSlot(livingIdentity, slot));
//            }
//        }
//    }

    @Inject(method = "doHurtTarget(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void doHurtTargetIdentity(Entity entity, CallbackInfoReturnable<Boolean> info) {
        if (entity instanceof EntityAccessor targetAccessor
                && targetAccessor.getIdentityOwner() instanceof LivingEntity owner
                && owner.isAlive()
                && owner != (Object) this) {
            LivingEntity attacker = (LivingEntity) (Object) this;
            float damage = Math.max(1.0F, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            boolean hurt = owner.hurt(owner.damageSources().mobAttack(attacker), damage);
            if (hurt) {
                owner.knockback(0.4D, attacker.getX() - owner.getX(), attacker.getZ() - owner.getZ());
            }
            info.setReturnValue(hurt);
            return;
        }
        if ((Entity)(Object)this instanceof Player) {
            // Keep vanilla player attack pipeline so item/enchant bonuses are applied.
            return;
        }
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.doHurtTarget(entity));
            }
        }
    }

    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$suppressOwnedIdentityDeathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (((EntityAccessor) this).getIdentityOwner() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void identity2$forwardOwnedIdentityDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity ownerEntity = ((EntityAccessor) self).getIdentityOwner();
        if (!(ownerEntity instanceof LivingEntity owner) || owner == self || !owner.isAlive()) {
            return;
        }
        if (owner instanceof Player playerOwner && SilverfishBurrowManager.isHidden(playerOwner) && source != null && source.is(DamageTypes.IN_WALL)) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(owner.hurt(source, amount));
    }
//Tons of Redirects - End
}
