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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    @Inject(method = "freeAt", at=@At("HEAD"))
    protected void disableNoClipSuffocate(BlockPos pos,CallbackInfoReturnable info) {
		if(this.noPhysics){
            info.setReturnValue(true);
        }
	}
//    @Inject(method = "attack", at = @At("TAIL"))
//    private void identity2$applyMorphMeleeEffects(Entity target, CallbackInfo info) {
//        Player player = (Player)(Object)this;
//        if (player.level().isClientSide()) {
//            return;
//        }
//        if (!(target instanceof LivingEntity livingTarget)) {
//            return;
//        }
//        if (livingTarget.getLastHurtByMob() != player) {
//            return;
//        }
//        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
//        if (identity == null) {
//            return;
//        }
//        if (identity.getType() == EntityType.CAVE_SPIDER) {
//            int duration = 0;
//            if (player.level().getDifficulty() == Difficulty.NORMAL) {
//                duration = 140;
//            } else if (player.level().getDifficulty() == Difficulty.HARD) {
//                duration = 300;
//            }
//            if (duration > 0) {
//                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0), player);
//            }
//            return;
//        }
//        if (identity.getType() == EntityType.WITHER_SKELETON && player.level().getDifficulty() != Difficulty.PEACEFUL) {
//            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0), player);
//        }
//    }
}

