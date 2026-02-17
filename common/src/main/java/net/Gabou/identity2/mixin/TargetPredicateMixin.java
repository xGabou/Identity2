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
import net.minecraft.item.BoatItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.Gabou.identity2.ModComponents;
import net.minecraft.entity.ai.TargetPredicate;
import net.Gabou.identity2.util.EntityAccessor;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(TargetPredicate.class)
public class TargetPredicateMixin{
    /*@Inject(method = "test", at=@At("HEAD"),cancellable=true)
    private static void testIdentityFix(ServerWorld world, @Nullable LivingEntity tester, LivingEntity target,CallbackInfoReturnable info) {
        if(((EntityAccessor)target).getCurrentIdentity()!=null){
            if(((EntityAccessor)target).getCurrentIdentity() instanceof LivingEntity){
                target=(LivingEntity)((EntityAccessor)target).getCurrentIdentity();
            }else{
                info.setReturnValue(false);
            }
        }

    }*/
    @ModifyVariable(method = "test(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), index=2)
    private LivingEntity injected(LivingEntity target) {
        if(((EntityAccessor)target).getCurrentIdentity()!=null){
            //net.Gabou.identity2.Identity2.LOGGER.info("TargetPredicateMixin active!");
            Entity identity=((EntityAccessor)target).getCurrentIdentity();
            identity.setPos(
                ((Entity)target).getEntityPos().x,
                ((Entity)target).getEntityPos().y,
                ((Entity)target).getEntityPos().z
            );
            ((EntityAccessor)identity).setIdentityOf(target);
            //net.Gabou.identity2.Identity2.LOGGER.info("TargetPredicateMixin active!");
            /*net.Gabou.identity2.Identity2.LOGGER.info("identityentity at "+(
                String.valueOf(((Entity)identity).getEntityPos().x)+" "+
                String.valueOf(((Entity)identity).getEntityPos().y)+" "+
                String.valueOf(((Entity)identity).getEntityPos().z))
                );*/
            return (LivingEntity)identity;
        }else{
            return target;
        }
    }
}
