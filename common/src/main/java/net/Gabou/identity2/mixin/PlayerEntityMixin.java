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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

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
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.server.level.ServerPlayer;

@Mixin(Player.class)
public class PlayerEntityMixin extends LivingEntityMixin {
    @ModifyConstant(constant = @Constant(doubleValue = 2.9999999E7), method = "tick")
    private static double TDIOA(double x) {
        return Identity2.maxWorldSize - 1;
    }

    @ModifyConstant(constant = @Constant(doubleValue = -2.9999999E7), method = "tick")
    private static double TDIOB(double x) {
        return -Identity2.maxWorldSize + 1;
    }
    @Inject(method = "freeAt", at = @At("HEAD"), cancellable = true)
    protected void disableNoClipSuffocate(BlockPos pos, CallbackInfoReturnable info) {
        if (this.noPhysics || WardenBurrowManager.isHidden((Entity) (Object) this)) {
            info.setReturnValue(true);
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void identity2$exitWardenBurrowOnAttack(Entity target, CallbackInfo ci) {
        if ((Entity) (Object) this instanceof ServerPlayer serverPlayer && WardenBurrowManager.isHidden(serverPlayer)) {
            WardenBurrowManager.stop(serverPlayer, true);
        }
    }

    @Inject(method = "interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void identity2$rideZombieHorseAsZombie(Entity target, InteractionHand hand, Vec3 interactionLocation, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide() || player.isSpectator() || player.isPassenger() || hand != InteractionHand.MAIN_HAND) {
            return;
        }

        Entity identity = getCurrentIdentity();
        if (identity == null || identity.getType() != net.minecraft.world.entity.EntityTypes.ZOMBIE || target.getType() != net.minecraft.world.entity.EntityTypes.ZOMBIE_HORSE) {
            return;
        }

        if (player.startRiding(target, true, false)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void attackAlsoAsIdentity(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (!(player.level() instanceof ServerLevel level)) return;

        Entity identity = getCurrentIdentity();
        if (identity == null) return;
        if (identity instanceof LivingEntity livingIdentity) {
            if (!identity2$hasAttackDamageAttribute(livingIdentity)) {
                return;
            }

            // Optional guard so it only triggers when the player actually hit something
            if (target instanceof LivingEntity livingTarget) {
                if (livingTarget.hurtTime <= 0) return;

                // Optional, makes sure the second hit is not blocked by i frames
                int oldInvul = livingTarget.invulnerableTime;
                livingTarget.invulnerableTime = 0;

                livingIdentity.doHurtTarget(level, target);

                livingTarget.invulnerableTime = oldInvul;
                return;
            }

            livingIdentity.doHurtTarget(level, target);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean identity2$hasAttackDamageAttribute(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        try {
            entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}

