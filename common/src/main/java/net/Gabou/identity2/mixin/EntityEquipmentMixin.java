package net.Gabou.identity2.mixin;

import net.Gabou.identity2.identity.KeepInventoryHelper;
import net.Gabou.identity2.ModComponents;
import net.minecraft.server.level.ServerLevel;
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
    @Inject(method = "dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), cancellable = true)
    private void identity2$dropEquipmentNoSoulbound(ServerLevel level, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (KeepInventoryHelper.isKeepInventoryEnabled(self)) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (EnchantmentHelper.has(stack, ModComponents.SOULBOUND)) {
                continue;
            }

            self.spawnAtLocation(level, stack);
            self.setItemSlot(slot, ItemStack.EMPTY);
        }

        ci.cancel();
    }
}
