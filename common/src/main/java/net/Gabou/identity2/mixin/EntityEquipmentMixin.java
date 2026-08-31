package net.Gabou.identity2.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.Gabou.identity2.ModComponents;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumMap;
import net.Gabou.identity2.identity.KeepInventoryHelper;
@Mixin(EntityEquipment.class)
public class EntityEquipmentMixin{
    @Unique
    private boolean identity2$isKeepInventoryEnabled = false;
    @Unique
    private boolean identity2$isPlayerEquipmentDrop = false;
    @Shadow
    private EnumMap<EquipmentSlot, ItemStack> items;
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    @Nullable
    private ItemEntity cancelDropSoulboundItems(LivingEntity entity, ItemStack stack,boolean dropAtSelf,boolean retainOwnership) {
        if (!this.identity2$isPlayerEquipmentDrop) {
            return entity.drop(stack, dropAtSelf, retainOwnership);
        }
        if (KeepInventoryHelper.isKeepInventoryEnabled(entity)) {
            return null;
        }
        if(!EnchantmentHelper.has(stack, ModComponents.SOULBOUND)){
            return entity.drop(stack,dropAtSelf,retainOwnership);
        }else{
            return null;
        }
    }

    @Inject(method = "dropAll",
            at = @At("HEAD"))
    private void dropAll(LivingEntity livingEntity, CallbackInfo ci) {
        this.identity2$isPlayerEquipmentDrop = livingEntity instanceof ServerPlayer;
        this.identity2$isKeepInventoryEnabled = this.identity2$isPlayerEquipmentDrop
                && KeepInventoryHelper.isKeepInventoryEnabled(livingEntity);
    }
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityEquipment;clear()V"))
    private void clearNonSoulboundItems(EntityEquipment equipment) {
        if (!this.identity2$isPlayerEquipmentDrop) {
            equipment.clear();
            return;
        }
        if (this.identity2$isKeepInventoryEnabled) {
            return;
        }
        this.items.replaceAll((slot, stack) -> EnchantmentHelper.has(stack, ModComponents.SOULBOUND)?stack:ItemStack.EMPTY);
        /*for(EquipmentSlot key:this.map.keySet()){
            if(!EnchantmentHelper.hasAnyEnchantmentsWith(this.map.get(key), ModComponents.SOULBOUND)){
                this.map.put(key,ItemStack.EMPTY);
            }
        }*/
    }
}

