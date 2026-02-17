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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.Gabou.identity2.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;

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
import net.minecraft.item.EmptyMapItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.Gabou.identity2.ModComponents;
import net.minecraft.world.entity.EntityIndex;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.EntityTrackingSection;
@Mixin(EntityTrackingSection.class)
public class EntityTrackingSectionMixin{
    @Redirect(method = "forEach(Lnet/minecraft/util/TypeFilter;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/function/LazyIterationConsumer;)Lnet/minecraft/util/function/LazyIterationConsumer$NextIteration;",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/util/TypeFilter;downcast(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <T,U extends T> U replaceDowncast(TypeFilter<T,U> filter, T toDowncast) {
        if(net.Gabou.identity2.Identity2.indexOverrideActive!=0){
        try{
        if(((EntityAccessor)toDowncast).getIdentityOwner()!=null){
            net.Gabou.identity2.Identity2.LOGGER.info("Success1 alt! "+((Entity)toDowncast).getName().getString()+" from "+((EntityAccessor)toDowncast).getIdentityOwner().getName().getString());
            if(filter.downcast(toDowncast)==null){
                net.Gabou.identity2.Identity2.LOGGER.info("Downcast Failure1 alt!");
            }
        }
        if(((EntityAccessor)toDowncast).getCurrentIdentity()!=null){
            net.Gabou.identity2.Identity2.LOGGER.info("Success2 alt! "+((EntityAccessor)toDowncast).getCurrentIdentity().getName().getString()+" from "+((Entity)toDowncast).getName().getString());
            toDowncast=(T)((EntityAccessor)toDowncast).getCurrentIdentity();
            try{
            if(filter.downcast(toDowncast)==null){
                net.Gabou.identity2.Identity2.LOGGER.info("Downcast Failure2 alt!");
            }else{
                net.Gabou.identity2.Identity2.LOGGER.info("Downcast Success2 alt!");
            }
            }catch(Exception n){
                net.Gabou.identity2.Identity2.LOGGER.info("Downcast Failure3 alt!");
            }
            return filter.downcast(toDowncast);
            /*try{
            if(filter.downcast((T)((EntityAccessor)toDowncast).getCurrentIdentity())!=null){
                try{
                net.Gabou.identity2.Identity2.LOGGER.info("EntityIndexMixin passed! ("+((Entity)toDowncast).getUuidAsString()+"("+toDowncast.getClass().getName()+") to "+((EntityAccessor)toDowncast).getCurrentIdentity().getUuidAsString()+"("+((EntityAccessor)toDowncast).getCurrentIdentity().getClass().getName()+"))");
                }catch(Exception e){
                    net.Gabou.identity2.Identity2.LOGGER.info("EntityIndexMixin passed ("+((Entity)toDowncast).getUuidAsString()+" to "+((EntityAccessor)toDowncast).getCurrentIdentity().getUuidAsString()+")");
                }
            }
            }catch(Exception e){
                net.Gabou.identity2.Identity2.LOGGER.info("Well this is awkward. V2");
            }*/
        }
    }catch(Exception e){
        net.Gabou.identity2.Identity2.LOGGER.info("Something went wrong alt.");
    }
        }//else, have a nice day without awkwardness
        return filter.downcast(toDowncast);
    }
}
