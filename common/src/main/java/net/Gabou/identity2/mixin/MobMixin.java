package net.Gabou.identity2.mixin;

import net.Gabou.identity2.identity.MorphEntityTraits;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void identity2$getIdentityRiderController(CallbackInfoReturnable<LivingEntity> cir) {
        Mob vehicle = (Mob) (Object) this;
        Entity passenger = vehicle.getFirstPassenger();
        if (!(passenger instanceof Player player)) {
            return;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (MorphEntityTraits.canIdentityRide(identity, vehicle)) {
            cir.setReturnValue(player);
        }
    }
}
