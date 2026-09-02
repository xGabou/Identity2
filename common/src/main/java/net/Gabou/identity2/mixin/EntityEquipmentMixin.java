package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.identity.KeepInventoryHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.Gabou.identity2.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public class EntityEquipmentMixin{
    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$noProxyDeathLoot(DamageSource source, CallbackInfo ci) {
        if (((EntityAccessor) this).getIdentityOwner() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void identity2$dropPlayerEquipment(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer)) {
            return;
        }
        if (KeepInventoryHelper.isKeepInventoryEnabled(self)) {
            ci.cancel();
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty() || ModComponents.hasSoulbound(stack)) {
                continue;
            }
            self.spawnAtLocation(stack);
            self.setItemSlot(slot, ItemStack.EMPTY);
        }
        ci.cancel();
    }

}

