package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.Gabou.identity2.ModEffects;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.Gabou.identity2.ModComponents;
import java.util.EnumMap;
@Mixin(EntityEquipment.class)
public class EntityEquipmentMixin{
    @Shadow
    private EnumMap<EquipmentSlot, ItemStack> items;
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    @Nullable
    private ItemEntity cancelDropSoulboundItems(LivingEntity entity, ItemStack stack,boolean dropAtSelf,boolean retainOwnership) {
        if(!EnchantmentHelper.has(stack, ModComponents.SOULBOUND)){
            return entity.drop(stack,dropAtSelf,retainOwnership);
        }else{
            return null;
        }
    }
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityEquipment;clear()V"))
    private void clearNonSoulboundItems(EntityEquipment equipment) {
        this.items.replaceAll((slot, stack) -> EnchantmentHelper.has(stack, ModComponents.SOULBOUND)?stack:ItemStack.EMPTY);
        /*for(EquipmentSlot key:this.map.keySet()){
            if(!EnchantmentHelper.hasAnyEnchantmentsWith(this.map.get(key), ModComponents.SOULBOUND)){
                this.map.put(key,ItemStack.EMPTY);
            }
        }*/
    }
}

