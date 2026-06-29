package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAmbientSoundMixin {
    @Inject(method = "playAmbientSound()V", at = @At("HEAD"), cancellable = true)
    private void identity2$cancelIdentityAmbientSound(CallbackInfo info) {
        EntityAccessor accessor = (EntityAccessor) this;
        if (accessor.getIdentityOwner() != null || accessor.getCurrentIdentity() instanceof LivingEntity) {
            info.cancel();
        }
    }
}
