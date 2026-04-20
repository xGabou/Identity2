package ember.qualitycommands.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.EntityEquipment;
import net.minecraft.util.Hand;
import ember.qualitycommands.ModComponents;
import ember.qualitycommands.QualityCommands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.entity.Entity;
import net.minecraft.enchantment.EnchantmentHelper;
import ember.qualitycommands.ModComponents;
import net.minecraft.entity.ItemEntity;
import java.util.EnumMap;
import net.minecraft.entity.EquipmentSlot;
@Mixin(EntityEquipment.class)
public class EntityEquipmentMixin{
    @Shadow
    private EnumMap<EquipmentSlot, ItemStack> map;
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"))
    @Nullable
    private ItemEntity cancelDropSoulboundItems(LivingEntity entity, ItemStack stack,boolean dropAtSelf,boolean retainOwnership) {
        if(!EnchantmentHelper.hasAnyEnchantmentsWith(stack, ModComponents.SOULBOUND)){
            return entity.dropItem(stack,dropAtSelf,retainOwnership);
        }else{
            return null;
        }
    }
    @Redirect(method = "dropAll",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityEquipment;clear()V"))
    private void clearNonSoulboundItems(EntityEquipment equipment) {
        this.map.replaceAll((slot, stack) -> EnchantmentHelper.hasAnyEnchantmentsWith(stack, ModComponents.SOULBOUND)?stack:ItemStack.EMPTY);
        /*for(EquipmentSlot key:this.map.keySet()){
            if(!EnchantmentHelper.hasAnyEnchantmentsWith(this.map.get(key), ModComponents.SOULBOUND)){
                this.map.put(key,ItemStack.EMPTY);
            }
        }*/
    }
}

