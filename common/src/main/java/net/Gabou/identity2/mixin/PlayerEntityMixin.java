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
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.util.IdentityEquipmentHelper;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
		if(this.noPhysics || WardenBurrowManager.isHidden((Entity) (Object) this)){
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

    @Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack blocked = IdentityEquipmentHelper.getBlockedSlotStack((Entity) (Object) this, slot);
        if (blocked != null) {
            cir.setReturnValue(blocked);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$attackAsIdentityWhenUnarmed(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }

        Entity identity = getCurrentIdentity();
        if (!(identity instanceof LivingEntity livingIdentity)) {
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
            PredefIdentityAbilities.triggerMorphAttackAnimation(player, 10);
            player.resetAttackStrengthTicker();
        }
        ci.cancel();
    }

//    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
//    private void identity2$applyIdentityMeleeEffect(Entity target, CallbackInfo info) {
//        if (((Entity)(Object)this).level().isClientSide()) {
//            return;
//        }
//        if (this.currentIdentity == null || !(target instanceof LivingEntity livingTarget)) {
//            return;
//        }
//        if (livingTarget.isDeadOrDying()) {
//            return;
//        }
//        if (livingTarget.getLastHurtByMob() != (LivingEntity)(Object)this) {
//            return;
//        }
//
//        EntityType<?> identityType = this.currentIdentity.getType();
//        if (identityType == EntityType.CAVE_SPIDER) {
//            int poisonDuration = switch (((Entity)(Object)this).level().getDifficulty()) {
//                case HARD -> 300;
//                case NORMAL -> 140;
//                default -> 0;
//            };
//            if (poisonDuration > 0) {
//                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration), (Entity)(Object)this);
//            }
//            return;
//        }
//
//        if (identityType == EntityType.WITHER_SKELETON && ((Entity)(Object)this).level().getDifficulty() != Difficulty.PEACEFUL) {
//            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 200), (Entity)(Object)this);
//        }
//    }
}
