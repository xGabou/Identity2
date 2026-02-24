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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.Gabou.identity2.ModEffects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.Gabou.identity2.Identity2;
@Mixin(NearestAttackableTargetGoal.class)
public class ActiveTargetGoalMixin{
	@Shadow
	LivingEntity target;
	@Inject(method = "findTarget", at=@At("HEAD"))
	private void identityBreak(CallbackInfo info) {
		Identity2.indexOverrideActive+=1;
		//Identity2.LOGGER.info("activeTargetGoal +1");
	}
	@Inject(method = "findTarget", at=@At("RETURN"))
	private void identityFix(CallbackInfo info) {
		try {
			if (this.target != null) {
				Entity owner = ((EntityAccessor) this.target).getIdentityOwner();
				if (owner instanceof LivingEntity livingOwner) {
					this.target = livingOwner;
				}
			}
		} catch (Exception ignored) {
		}
		Identity2.indexOverrideActive-=1;
	}
	
}

