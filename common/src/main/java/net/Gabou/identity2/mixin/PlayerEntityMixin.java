package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.mixin.LivingEntitySoundInvoker;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.identity.SilverfishBurrowManager;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin{
    @ModifyConstant(constant=@Constant(doubleValue=2.9999999E7),method="tick")
    private static double TDIOA(double x){
        return Identity2.maxWorldSize-1;
    }
    @ModifyConstant(constant=@Constant(doubleValue=-2.9999999E7),method="tick")
    private static double TDIOB(double x){
        return -Identity2.maxWorldSize+1;
    }
    @Inject(method = "freeAt", at=@At("HEAD"), cancellable = true)
    protected void disableNoClipSuffocate(BlockPos pos,CallbackInfoReturnable info) {
		if(this.noPhysics || SilverfishBurrowManager.isHidden((Entity) (Object) this)){
            info.setReturnValue(true);
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            info.setReturnValue(true);
        }
	}

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void identity2$exitBurrowOnAttack(Entity target, CallbackInfo ci) {
        if ((Entity) (Object) this instanceof ServerPlayer serverPlayer) {
            if (SilverfishBurrowManager.isHidden(serverPlayer)) {
                SilverfishBurrowManager.stop(serverPlayer, true);
            }
        }
    }

    @Inject(method = "playStepSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$playIdentityStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!IdentitySettings.useIdentitySounds) {
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity == null || !((Entity) (Object) this).onGround()) {
            return;
        }
        ((EntitySoundInvoker) identity).identity2$invokePlayStepSound(pos, state);
        ci.cancel();
    }

    // Player overrides LivingEntity.getHurtSound, so the common replacement does
    // not cover client damage events for morphed players.
    @Inject(
            method = "getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void identity2$getIdentityHurtSound(
            net.minecraft.world.damagesource.DamageSource source,
            CallbackInfoReturnable<net.minecraft.sounds.SoundEvent> cir
    ) {
        if (!IdentitySettings.useIdentitySounds) {
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity instanceof LivingEntity livingIdentity) {
            cir.setReturnValue(((LivingEntitySoundInvoker) livingIdentity).identity2$invokeGetHurtSound(source));
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$attackAsIdentityWhenUnarmed(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        // Requested behavior:
        // - Empty main hand -> identity attack logic.
        // - Any held item -> vanilla player attack logic.
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }
        if (!IdentitySettings.useIdentityAttackDamage) {
            return;
        }

        Entity identity = getCurrentIdentity();
        if (!(identity instanceof LivingEntity livingIdentity)) {
            return;
        }

        livingIdentity.setPos(player.position());
        livingIdentity.setDeltaMovement(player.getDeltaMovement());

        boolean attacked;
        if (identity.getType() == EntityType.WARDEN) {
            float damage = Math.max(16.0F, (float) livingIdentity.getAttributeValue(Attributes.ATTACK_DAMAGE));
            attacked = target.hurt(player.damageSources().mobAttack(livingIdentity), damage);
            if (attacked) {
                Vec3 look = player.getViewVector(1.0F).normalize();
                target.push(look.x * 1.6D, 0.55D, look.z * 1.6D);
            }
        } else {
            attacked = livingIdentity.doHurtTarget(target);
        }
        if (!attacked) {
            float damage = (float) livingIdentity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (damage <= 0.0F) {
                damage = 1.0F;
            }
            attacked = target.hurt(player.damageSources().mobAttack(livingIdentity), damage);
        }

        if (attacked) {
            PredefIdentityAbilities.triggerMorphAttackAnimation(player, identity.getType() == EntityType.WARDEN ? 20 : 10);
            player.resetAttackStrengthTicker();
        }
        ci.cancel();
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void identity2$applyIdentityMeleeEffect(Entity target, CallbackInfo info) {
        if (((Entity)(Object)this).level().isClientSide()) {
            return;
        }
        if (this.currentIdentity == null || !(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.isDeadOrDying()) {
            return;
        }
        if (livingTarget.getLastHurtByMob() != (Object)this) {
            return;
        }

        EntityType<?> identityType = this.currentIdentity.getType();
        if (identityType == EntityType.CAVE_SPIDER) {
            int poisonDuration = switch (((Entity)(Object)this).level().getDifficulty()) {
                case HARD -> 300;
                case NORMAL -> 140;
                default -> 0;
            };
            if (poisonDuration > 0) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration), (Entity)(Object)this);
            }
            return;
        }

        if (identityType == EntityType.WITHER_SKELETON && ((Entity)(Object)this).level().getDifficulty() != Difficulty.PEACEFUL) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 200), (Entity)(Object)this);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void identity2$animateVanillaDamageUnarmedMorphAttack(Entity target, CallbackInfo info) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()
                || IdentitySettings.useIdentityAttackDamage
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity == null) {
            return;
        }
        if (target instanceof LivingEntity livingTarget && livingTarget.getLastHurtByMob() != player) {
            return;
        }
        PredefIdentityAbilities.triggerMorphAttackAnimation(
                player,
                identity.getType() == EntityType.WARDEN ? 20 : 10
        );
    }

    @Inject(method = "updatePlayerPose()V", at = @At("TAIL"))
    private void identity2$avoidUnneededSmallMorphCrawl(CallbackInfo info) {
        Player player = (Player) (Object) this;
        Entity identity = getCurrentIdentity();
        if (identity == null
                || identity.getBbHeight() >= 1.0F
                || player.getPose() != Pose.SWIMMING
                || player.isSwimming()
                || player.isFallFlying()
                || player.isAutoSpinAttack()
                || player.isSleeping()) {
            return;
        }
        if (player.level().noCollision(player, player.getBoundingBox())) {
            player.setPose(Pose.STANDING);
        }
    }
}
