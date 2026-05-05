package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobEntityMixin extends LivingEntityMixin {
    @Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getItemBySlotIdentity(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> info) {
        identity2$filterEquippedStack(slot, info);
    }
}
