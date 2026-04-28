package net.Gabou.identity2.mixin;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;

import net.Gabou.identity2.identity.IdentityTraitTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
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
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.Gabou.identity2.util.AttributeContainerAccessor;
import net.Gabou.identity2.util.DefaultAttributeContainerAccessor;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;

@Mixin(LivingEntity.class)
public class LivingEntityMixin extends EntityMixin implements LivingEntityAccessor {

    @Mutable
    @Shadow
    private AttributeMap attributes;

    public void fixAttributes(Entity entity, Entity identity) {
        if (entity instanceof Player) {
            return;
        }
        if ((identity instanceof LivingEntity livingIdentity) && (entity instanceof LivingEntity livingEntity)) {
            this.attributes = createMangled(livingEntity.getAttributes(), livingIdentity.getAttributes());
            /*Identity2.LOGGER.info("Attributes mangled!");
            for(EntityAttributeInstance attr:((DefaultAttributeContainerAccessor)((AttributeContainerAccessor)this.attributes).getDefaultAttributes()).getInstances().values()){
                Identity2.LOGGER.info("Mangled "+attr.getAttribute().getIdAsString()+" : "+String.valueOf(this.attributes.getValue(attr.getAttribute())));
            }*/
        }

    }


    public AttributeMap createMangled(AttributeMap a, AttributeMap b) {
        AttributeSupplier.Builder builder = AttributeSupplier.builder();
        for (AttributeInstance attr : ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) a).getDefaultAttributes()).getInstances().values()) {
            builder.add(attr.getAttribute(), attr.getBaseValue());
            //Identity2.LOGGER.info("Mangling A: "+attr.getAttribute().toString());
        }
        for (AttributeInstance attr : ((DefaultAttributeContainerAccessor) ((AttributeContainerAccessor) b).getDefaultAttributes()).getInstances().values()) {
            builder.add(attr.getAttribute(), attr.getBaseValue());
            //Identity2.LOGGER.info("Mangling B: "+attr.getAttribute().toString());
        }
        AttributeMap newContainer = new AttributeMap(builder.build());
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
    public boolean canUseSlot(EquipmentSlot slot) {
        return false;
    }

    private static boolean identity2$isAquaticMorph(LivingEntity livingIdentity) {
        return livingIdentity != null
            && (
                livingIdentity.canBreatheUnderwater()
                    || Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(livingIdentity.getType()))
            );
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
        if (host.isInWater() && identity2$isAquaticMorph(livingIdentity)) {
            info.setReturnValue(host.getMaxAirSupply());
        }
    }

    @Inject(method = "increaseAirSupply(I)I", at = @At("HEAD"), cancellable = true)
    private void getNextAirOnLandIdentity(int air, CallbackInfoReturnable info) {
        if (!(this.currentIdentity instanceof LivingEntity livingIdentity)) {
            return;
        }

        LivingEntity host = (LivingEntity) (Object) this;
        if (host.isInWater()) {
            return;
        }

        if (identity2$isAquaticMorph(livingIdentity)) {
            int nextAir = air - 1;
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
    private void canBreatheInWaterIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(identity2$isAquaticMorph(livingIdentity));
            }
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void identity2$disableFallDamageForFlyingMorphs(double d, float f, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!((Entity) (Object) this instanceof Player)) {
            return;
        }
        if (this.currentIdentity != null && (((EntityAccessor) this.currentIdentity).canFly()
                || this.currentIdentity.getType() == EntityType.CHICKEN
                || this.currentIdentity.getType() == EntityType.CAT
                || IdentityTraitTags.hasSlowFalling(this.currentIdentity.getType()))) {
            cir.setReturnValue(false);
        }
    }


    @Shadow
    @Nullable
    public SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Shadow
    @Nullable
    public SoundEvent getDeathSound() {
        return null;
    }


    @Inject(method = "getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;", at = @At("HEAD"), cancellable = true)
    private void getHurtSoundIdentity(DamageSource source, CallbackInfoReturnable info) {
        if (this.identityOf != null) {
            info.setReturnValue(null);
            return;
        }
        if (IdentitySettings.useIdentitySounds) {
            if (this.currentIdentity != null) {
                if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                    info.setReturnValue(((LivingEntityAccessor) this.currentIdentity).getHurtSound(source));
                }
            }
        }
    }

@Inject(method = "aiStep()V", at = @At("TAIL"))
private void identity2$playAmbientSound(CallbackInfo info) {
    if (!IdentitySettings.playAmbientSounds) {
        return;
    }
    if (!(this.currentIdentity instanceof LivingEntity livingIdentity)) {
        return;
    }
    if (!((Entity) (Object) this instanceof Player hostPlayer) || !(hostPlayer.level() instanceof ServerLevel serverLevel)) {
        return;
    }

    Object ambientSoundValue = identity2$invokeNoArg(livingIdentity, "getAmbientSound");
    if (!(ambientSoundValue instanceof SoundEvent ambientSound)) {
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
            livingIdentity.getSoundSource(),
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


    @Inject(method = "aiStep()V", at = @At("HEAD"), cancellable = true)
    private void tickMovementIdentity(CallbackInfo info) {
        //if ((Entity)(Object)this instanceof Player) {
        // Keep vanilla player movement/collision to avoid wall-sticking while morphed.
        //    return;
        //}
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {

                livingIdentity.setPos(this.position());
                livingIdentity.setDeltaMovement(this.getDeltaMovement());
                if (livingIdentity instanceof Mob mobIdentity) {
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
    @Inject(method = "onClimbable()Z", at = @At("HEAD"), cancellable = true)
    private void identity2$spiderWallClimb(CallbackInfoReturnable<Boolean> info) {
        if (this.currentIdentity == null) {
            return;
        }
        EntityType<?> identityType = this.currentIdentity.getType();
        if (identityType != EntityType.SPIDER && identityType != EntityType.CAVE_SPIDER) {
            return;
        }
        if ((Entity) (Object) this instanceof Player player && player.isSpectator()) {
            info.setReturnValue(false);
            return;
        }
        info.setReturnValue(this.horizontalCollision);
    }

    @Inject(method = "canFreeze()Z", at = @At("HEAD"), cancellable = true)
    private void canFreezeIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.canFreeze());

        }
    }

    @Inject(method = "canStandOnFluid(Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true)
    private void canWalkOnFluidIdentity(net.minecraft.world.level.material.FluidState key, CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.canStandOnFluid(key));
            }
        }
    }

    @Inject(method = "isSensitiveToWater()Z", at = @At("HEAD"), cancellable = true)
    private void hurtByWaterIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.isSensitiveToWater());
            }
        }
    }

    @Inject(method = "isAffectedByPotions()Z", at = @At("HEAD"), cancellable = true)
    private void isAffectedBySplashPotionsIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.isAffectedByPotions());
            }
        }
    }

    @Inject(method = "getLastHurtByPlayerMemoryTime()I", at = @At("HEAD"), cancellable = true)
    private void getPlayerHitTimerIdentity(CallbackInfoReturnable info) {
        if (this.identityOf != null) {
            if (this.identityOf instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.getLastHurtByPlayerMemoryTime());
            }
        }
    }


    @Inject(method = "isInvulnerableTo(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    private void isInvulnerableToIdentity(ServerLevel world, DamageSource source, CallbackInfoReturnable<Boolean> info) {
        if ((Object) this instanceof Player player) {
            if (player.getAbilities().instabuild || player.isSpectator()) {
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    return;
                }
                info.setReturnValue(true);
                return;
            }
            if (
                    this.currentIdentity != null
                            && source.is(DamageTypes.IN_WALL)
                            && player.isInWater()
                            && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(this.currentIdentity.getType()))
            ) {
                info.setReturnValue(true);
                return;
            }
            Entity activeIdentity = ((EntityAccessor) player).getCurrentIdentity();
            if (
                    activeIdentity != null
                            && activeIdentity.getType() == EntityType.ENDER_DRAGON
                            && (source.is(DamageTypes.DRAGON_BREATH) || identity2$isOwnDragonBreathCloud(player, source))
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && identity2$isMorphFireImmune(activeIdentity)
                            && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA))
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && source.is(DamageTypes.IN_WALL)
                            && identity2$shouldIgnoreMorphSuffocation(player, activeIdentity)
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && identity2$isFallDamage(source)
                            && (
                            activeIdentity.getType() == EntityType.CHICKEN
                                    || activeIdentity.getType() == EntityType.CAT
                                    || IdentityTraitTags.hasSlowFalling(activeIdentity.getType())
                    )
            ) {
                info.setReturnValue(true);
                return;
            }
            boolean dragonIdentity = activeIdentity != null && activeIdentity.getType() == EntityType.ENDER_DRAGON;
            if ((dragonIdentity || IdentityProgression.isMorphDamageGraceActive(player)) && identity2$isWallCollisionDamage(source)) {
                info.setReturnValue(true);
                return;
            }
            return;
        }
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.isInvulnerableTo(world, source));
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
    private static boolean identity2$isMorphFireImmune(Entity activeIdentity) {
        return activeIdentity != null && activeIdentity.fireImmune();
    }

    @Unique
    private static boolean identity2$isOwnDragonBreathCloud(Player player, DamageSource source) {
        if (player == null || source == null) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AreaEffectCloud cloud)) {
            return false;
        }
        return cloud.getOwner() == player || source.getEntity() == player;
    }

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

    private static Object identity2$invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(target);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void pushAwayFromIdentity(Entity entity, CallbackInfo info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity == entity) {
                info.cancel();
            }
        }
    }


    @Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void getEquippedStackIdentity(EquipmentSlot slot, CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                if (livingIdentity.canUseSlot(slot) == false) {
                    info.setReturnValue(Items.AIR.getDefaultInstance());
                }
            }
        }
    }


@Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at=@At("HEAD"),cancellable=true)
private void getEquippedStackIdentity(EquipmentSlot slot, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        if(this.currentIdentity instanceof LivingEntity livingIdentity){
            if (slot.getType() == EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipItems) {
                info.setReturnValue(Items.AIR.getDefaultInstance());
                return;
            }
            if (slot.getType() != EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipArmor) {
                info.setReturnValue(Items.AIR.getDefaultInstance());
                return;
            }
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
            if (slot.getType() == EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipItems) {
                info.setReturnValue(false);
                return;
            }
            if (slot.getType() != EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipArmor) {
                info.setReturnValue(false);
                return;
            }
            info.setReturnValue(livingIdentity.canUseSlot(slot));
        }
    }
}

    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void doHurtTargetIdentity(ServerLevel world, Entity entity, CallbackInfoReturnable info) {
        if ((Entity)(Object)this instanceof Player) {
            // Keep vanilla player attack pipeline so item/enchant bonuses are applied.
            return;
        }
        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.doHurtTarget(world, entity));
            }
        }
    }
//Tons of Redirects - End
}
