package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.IdentityEquipmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobEquipmentMixin {
    @Inject(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void identity2$getItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack blocked = IdentityEquipmentHelper.getBlockedSlotStack((Entity) (Object) this, slot);
        if (blocked != null) {
            cir.setReturnValue(blocked);
        }
    }
}
