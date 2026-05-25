package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.Collection;
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
import org.spongepowered.asm.mixin.Overwrite;
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
import java.util.Map;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import java.util.Collections;
import java.util.stream.Collectors;
@Mixin(ClassInstanceMultiMap.class)
public class TypeFilterableListMixin<T>{
    @Shadow
    private Map<Class<?>,List<T>> byClass;
    @Shadow
    private Class<?> baseClass;
    @Shadow
    private List<T> allInstances;
    
    //@Inject(method = "getAllOfType", at=@At("HEAD"),cancellable=true)
    /**
     * @author Gaboouu
     * @reason Identity-aware class lookup while targeting checks are active.
     */
    @Overwrite
    public <S extends T> Collection<S> find(Class<S> type) {
		if (!this.baseClass.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Don't know how to search for " + type);
		} else {
            if (net.Gabou.identity2.Identity2.indexOverrideActive == 0) {
                List<S> list = (List<S>) this.byClass.computeIfAbsent(
                    type,
                    typeClass -> (List) this.allInstances.stream().filter(typeClass::isInstance).collect(Collectors.toList())
                );
                return Collections.unmodifiableCollection(list);
            }

            // ActiveTargetGoal performs identity-aware targeting only while this flag is set.
            // Do not cache transformed results, otherwise identity substitutions leak to normal queries.
            List<S> liveList = (List<S>) this.allInstances.stream()
                .map((T entity) -> {
                    if (entity instanceof Entity && ((EntityAccessor) entity).getCurrentIdentity() != null) {
                        return (T) ((EntityAccessor) entity).getCurrentIdentity();
                    }
                    return entity;
                })
                .filter(type::isInstance)
                .collect(Collectors.toList());
            return Collections.unmodifiableCollection(liveList);
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

