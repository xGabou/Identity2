package net.Gabou.identity2.mixin;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin extends LivingEntityMixin {
    @Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getItemBySlotIdentity(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> info) {
        identity2$filterEquippedStack(slot, info);
    }

    @ModifyConstant(constant = @Constant(doubleValue = 2.9999999E7), method = "tick")
    private static double TDIOA(double x) {
        return Identity2.maxWorldSize - 1;
    }

    @ModifyConstant(constant = @Constant(doubleValue = -2.9999999E7), method = "tick")
    private static double TDIOB(double x) {
        return -Identity2.maxWorldSize + 1;
    }

    @Inject(method = "freeAt", at = @At("HEAD"), cancellable = true)
    protected void disableNoClipSuffocate(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
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

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$attackAsIdentityWhenUnarmed(Entity target, CallbackInfo ci) {
        if (!((Object) this instanceof Player player) || player.level().isClientSide() || !player.getMainHandItem().isEmpty()) {
            return;
        }

        Entity identity = getCurrentIdentity();
        if (!(identity instanceof LivingEntity livingIdentity) || !identity2$hasAttackDamageAttribute(livingIdentity)) {
            return;
        }

        livingIdentity.setPos(player.position());
        livingIdentity.setDeltaMovement(player.getDeltaMovement());

        boolean attacked = livingIdentity.doHurtTarget(target);
        if (!attacked) {
            float damage = (float) livingIdentity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (damage <= 0.0F) {
                damage = 1.0F;
            }
            attacked = target.hurt(player.damageSources().mobAttack(livingIdentity), damage);
        }

        if (attacked) {
            player.resetAttackStrengthTicker();
        }
        ci.cancel();
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
