package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;

import java.util.Collection;
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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.Gabou.identity2.ModEffects;
import net.Gabou.identity2.Identity2;
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
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityLookupView;
import net.minecraft.util.collection.TypeFilterableList;
import java.util.Map;
import net.minecraft.util.Util;
import java.util.Collections;
@Mixin(TypeFilterableList.class)
public class TypeFilterableListMixin<T>{
    @Shadow
    private Map<Class<?>,List<T>> elementsByType;
    @Shadow
    private Class<?> elementType;
    @Shadow
    private List<T> allElements;
    
    //@Inject(method = "getAllOfType", at=@At("HEAD"),cancellable=true)
    @Overwrite
    public <S extends T> Collection<S> getAllOfType(Class<S> type) {
		if (!this.elementType.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Don't know how to search for " + type);
		} else {
			List<S> list = (List<S>)this.elementsByType
				.computeIfAbsent(type, typeClass -> (List)this.allElements.stream().map(
                    (T entity)->{
                        if(entity instanceof Entity){
                            if(((EntityAccessor) entity).getCurrentIdentity()!=null){
                                net.Gabou.identity2.Identity2.LOGGER.info("TypeFilterableListMixinActive");
                                return ((EntityAccessor) entity).getCurrentIdentity();
                            }else{
                            return entity;
                            }
                        }else{
                            return entity;
                        }
                    }
                ).filter(typeClass::isInstance).collect(Util.toArrayList()));
			return Collections.unmodifiableCollection(list);
		}
	}
    /*private static void useInject(List<?> entities, TargetPredicate targetPredicate, @Nullable LivingEntity entity, double x, double y, double z,CallbackInfoReturnable info) {
        //Identity2.LOGGER.info("EntityLookupViewMixin check!");
        if(info.getReturnValue()!=null){
        Entity returnvalue=((EntityAccessor)info.getReturnValue()).getIdentityOwner();
        if(returnvalue!=null){
            Identity2.LOGGER.info("TypeFilterableListMixin trigger!");
            info.setReturnValue(returnvalue);
        }
    }
    }*/
}
