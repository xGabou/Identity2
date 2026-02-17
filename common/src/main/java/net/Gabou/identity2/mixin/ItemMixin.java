package net.Gabou.identity2.mixin;

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
import net.Gabou.identity2.ModEffects;
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
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.Gabou.identity2.ModComponents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EquipmentSlot;
import net.Gabou.identity2.commands.WithCommand;
import net.minecraft.command.EntityDataObject;
import net.Gabou.identity2.Identity2;
@Mixin(Item.class)
public class ItemMixin{
	@Inject(method = "use", at=@At("HEAD"))
	private static void useInject(World world, PlayerEntity user, Hand hand,CallbackInfoReturnable info) {
		if(world.getServer()!=null){
			String command=user.getStackInHand(hand).getOrDefault(ModComponents.USE_COMMAND_COMPONENT, "");
			if(command!=""){
				world.getServer().getCommandManager().parseAndExecute(world.getServer().getCommandSource().withEntity(user).withPosition(user.getEntityPos()).withSilent(),command);
			}
			
		}
	}
	@Inject(method = "onItemEntityDestroyed", at=@At("HEAD"))
	private static void onItemEntityDestroyedInject(ItemEntity entity,CallbackInfo info) {
		if(entity.getEntityWorld().getServer()!=null){
			String command=entity.getStack().getOrDefault(ModComponents.ON_ITEM_DESTROYED_COMMAND_COMPONENT, "");
			if(command!=""){
				Identity2.LOGGER.info(WithCommand.replaceAllInString(command,"@(d)",WithCommand.toString(new EntityDataObject(entity).getNbt().get("Item"))));
			
				entity.getEntityWorld().getServer().getCommandManager().parseAndExecute(entity.getEntityWorld().getServer().getCommandSource().withEntity(entity).withPosition(entity.getEntityPos()).withSilent(),/*command*/
				WithCommand.replaceAllInString(command,"@(d)",WithCommand.toString(new EntityDataObject(entity).getNbt().get("Item")))
				);
				
			}
		}
	}
	@Inject(method = "inventoryTick", at=@At("HEAD"))
	private static void inventoryTickInject(ItemStack stack, ServerWorld world,Entity entity,@Nullable EquipmentSlot slot,CallbackInfo info) {
		if(world.getServer()!=null){
			String command=stack.getOrDefault(ModComponents.INVENTORY_TICK_COMMAND_COMPONENT, "");
			if(command!=""){
				world.getServer().getCommandManager().parseAndExecute(world.getServer().getCommandSource().withEntity(entity).withPosition(entity.getEntityPos()).withSilent(),
				WithCommand.replaceAllInString(command,"@(s)",slot.getName())
			);
			}
		}
	}
	/*private boolean removeIfInvalid(PlayerEntity player) {
	ItemStack itemStack = player.getMainHandStack();
	ItemStack itemStack2 = player.getOffHandStack();
	boolean bl = itemStack.isOf(Items.FISHING_ROD);
	boolean bl2 = itemStack2.isOf(Items.FISHING_ROD);
		if (!player.isRemoved() && player.isAlive() && (bl || bl2) && !(this.squaredDistanceTo(player) > 1024.0)) {
			return false;
		} else {
			this.discard();
			return true;
		}
	}*/
}


