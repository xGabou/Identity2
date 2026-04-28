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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
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
import net.Gabou.identity2.util.EntityAccessor;
import org.spongepowered.asm.mixin.Overwrite;
@Mixin(Player.class)
public class PlayerEntityMixin extends LivingEntityMixin{
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
		if(this.noPhysics){
            info.setReturnValue(true);
            return;
        }
        Entity identity = getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            info.setReturnValue(true);
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
        if (!identity2$hasAttackDamageAttribute(livingIdentity)) {
            return;
        }

        livingIdentity.setPos(player.position());
        livingIdentity.setDeltaMovement(player.getDeltaMovement());

        boolean attacked = player.level() instanceof ServerLevel serverLevel && livingIdentity.doHurtTarget(serverLevel, target);
        if (!attacked) {
            float damage = (float) livingIdentity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (damage <= 0.0F) {
                damage = 1.0F;
            }
            target.hurt(player.damageSources().mobAttack(livingIdentity), damage);
            attacked = true;
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
