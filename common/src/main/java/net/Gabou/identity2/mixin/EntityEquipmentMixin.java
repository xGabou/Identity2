package net.Gabou.identity2.mixin;

import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.identity.KeepInventoryHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class EntityEquipmentMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void identity2$dropEquipmentNoSoulbound(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (KeepInventoryHelper.isKeepInventoryEnabled(self)) {
            ci.cancel();
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty() || EnchantmentHelper.has(stack, ModComponents.SOULBOUND)) {
                continue;
            }

            self.spawnAtLocation(stack);
            self.setItemSlot(slot, ItemStack.EMPTY);
        }

        ci.cancel();
    }
}
