package net.Gabou.identity2.mixin;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public class TargetPredicateMixin {
    @Inject(
            method = "test(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void identity2$skipHostileVsHostileMorph(@Nullable LivingEntity tester, LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        if (!IdentitySettings.hostilesIgnoreHostileIdentityPlayer) {
            return;
        }
        if (!(tester instanceof Monster)) {
            return;
        }
        if (!(target instanceof Player player)) {
            return;
        }

        Entity currentIdentity = ((EntityAccessor) target).getCurrentIdentity();
        if (!(currentIdentity instanceof LivingEntity identityLiving)) {
            return;
        }
        if (!identity2$isHostileMob(identityLiving.getType())) {
            return;
        }

        if (IdentitySettings.hostilesForgetNewHostileIdentityPlayer
                && target instanceof ServerPlayer serverPlayer
                && IdentityProgression.isHostileIdentityGraceActive(serverPlayer)) {
            return;
        }

        info.setReturnValue(false);
    }

    @Unique
    private static boolean identity2$isHostileMob(EntityType<?> type) {
        return type != null && type.getCategory() == MobCategory.MONSTER;
    }
}
