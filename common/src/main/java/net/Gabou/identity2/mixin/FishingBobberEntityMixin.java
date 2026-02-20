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
import net.minecraft.world.entity.projectile.FishingHook;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
@Mixin(FishingHook.class)
public class FishingBobberEntityMixin{
	@ModifyConstant(method = "shouldStopFishing", constant = @Constant(doubleValue = 1024.0))
	private double injected(double value) {
		return value*100;
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


