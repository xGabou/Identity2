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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(TargetingConditions.class)
public class TargetPredicateMixin{
    @Inject(
        method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void identity2$skipHostileVsHostileMorph(ServerLevel world, @Nullable LivingEntity tester, LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        if (!IdentitySettings.hostilesIgnoreHostileIdentityPlayer) {
            return;
        }
        if (!(tester instanceof Monster)) {
            return;
        }
        if (!(target instanceof Player)) {
            return;
        }

        Entity currentIdentity = ((EntityAccessor) target).getCurrentIdentity();
        if (!(currentIdentity instanceof LivingEntity identityLiving)) {
            return;
        }

        EntityType<?> testerType = tester.getType();
        EntityType<?> identityType = identityLiving.getType();
        if (testerType == null || identityType == null) {
            return;
        }

        if (identity2$hasRecentAggression(tester, (Player) target)) {
            return;
        }

        if (testerType == identityType) {
            info.setReturnValue(false);
            return;
        }

        if (testerType.getCategory() == MobCategory.MONSTER && identityType.getCategory() == MobCategory.MONSTER) {
            info.setReturnValue(false);
        }
    }

    private static boolean identity2$hasRecentAggression(LivingEntity tester, Player target) {
        int hostilityWindow = Math.max(20, IdentitySettings.hostilityTime);

        if (tester instanceof Mob mob && mob.getTarget() == target) {
            return true;
        }

        if (tester.getLastHurtByMob() == target) {
            int stamp = tester.getLastHurtByMobTimestamp();
            if (stamp <= 0 || tester.tickCount - stamp <= hostilityWindow) {
                return true;
            }
        }

        if (tester.getLastHurtByPlayer() == target) {
            return true;
        }

        if (target.getLastHurtMob() == tester) {
            int stamp = target.getLastHurtMobTimestamp();
            if (stamp <= 0 || target.tickCount - stamp <= hostilityWindow) {
                return true;
            }
        }

        return false;
    }

    /*@Inject(method = "test", at=@At("HEAD"),cancellable=true)
    private static void testIdentityFix(ServerWorld world, @Nullable LivingEntity tester, LivingEntity target,CallbackInfoReturnable info) {
        if(((EntityAccessor)target).getCurrentIdentity()!=null){
            if(((EntityAccessor)target).getCurrentIdentity() instanceof LivingEntity){
                target=(LivingEntity)((EntityAccessor)target).getCurrentIdentity();
            }else{
                info.setReturnValue(false);
            }
        }

    }*/
    @ModifyVariable(method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), index=2)
    private LivingEntity injected(LivingEntity target) {
        if(((EntityAccessor)target).getCurrentIdentity()!=null){
            //net.Gabou.identity2.Identity2.LOGGER.info("TargetPredicateMixin active!");
            Entity identity=((EntityAccessor)target).getCurrentIdentity();
            identity.setPosRaw(
                ((Entity)target).position().x,
                ((Entity)target).position().y,
                ((Entity)target).position().z
            );
            ((EntityAccessor)identity).setIdentityOf(target);
            //net.Gabou.identity2.Identity2.LOGGER.info("TargetPredicateMixin active!");
            /*net.Gabou.identity2.Identity2.LOGGER.info("identityentity at "+(
                String.valueOf(((Entity)identity).getEntityPos().x)+" "+
                String.valueOf(((Entity)identity).getEntityPos().y)+" "+
                String.valueOf(((Entity)identity).getEntityPos().z))
                );*/
            return (LivingEntity)identity;
        }else{
            return target;
        }
    }
}
