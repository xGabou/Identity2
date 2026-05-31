package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipSoundMixin {

    @Inject(
            method = "onEquipItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void identity2$suppressMorphEquipSound(
            EquipmentSlot slot,
            ItemStack oldStack,
            ItemStack newStack,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof Player player && ((EntityAccessor) player).getCurrentIdentity() != null) {
            ci.cancel();
            return;
        }

        if (((EntityAccessor) self).getIdentityOwner() != null) {
            ci.cancel();
        }
    }
}