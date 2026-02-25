//package net.Gabou.identity2.mixin;
//import com.google.common.collect.Lists;
//import java.util.List;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.gen.Accessor;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Mutable;
//import org.spongepowered.asm.mixin.injection.Constant;
//import org.spongepowered.asm.mixin.injection.ModifyConstant;
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import net.Gabou.identity2.ModEffects;
//import net.Gabou.identity2.Identity2;
//import java.util.Set;
//import org.jetbrains.annotations.Nullable;
//import com.mojang.brigadier.builder.LiteralArgumentBuilder;
//import com.mojang.brigadier.builder.ArgumentBuilder;
//import com.mojang.brigadier.tree.CommandNode;
//import com.mojang.brigadier.tree.LiteralCommandNode;
//import com.mojang.brigadier.exceptions.CommandSyntaxException;
//import com.mojang.brigadier.context.CommandContext;
//import net.Gabou.identity2.ModComponents;
//import net.Gabou.identity2.util.EntityAccessor;
//import net.minecraft.server.level.ServerEntityGetter;
//import net.minecraft.tags.TagKey;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.ai.targeting.TargetingConditions;
//import net.minecraft.world.phys.AABB;
//@Mixin(ServerEntityGetter.class)
//public interface EntityLookupViewMixin{
//
//    @Inject(method = "getNearestEntity(Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)Lnet/minecraft/world/entity/LivingEntity;", at=@At("RETURN"),cancellable=true)
//    private static void useInject(List<?> entities, TargetingConditions targetPredicate, @Nullable LivingEntity entity, double x, double y, double z,CallbackInfoReturnable info) {
//        try{
//        if(Identity2.indexOverrideActive!=0){
//        for(Object m:entities){
//			if(m instanceof Entity n){
//				if(((EntityAccessor)n).getCurrentIdentity()!=null){
//					//Identity2.LOGGER.info("Entity with identity made it to the final step");
//				}
//				if(((EntityAccessor)n).getIdentityOwner()!=null){
//					//Identity2.LOGGER.info("Identity entity made it to the final step");
//				}
//			}
//		}
//        }
//        }catch(Exception e){
//            int b=0;
//        }
//        //Identity2.LOGGER.info("EntityLookupViewMixin check!");
//        if(info.getReturnValue()!=null){
//        Entity m=((EntityAccessor)info.getReturnValue()).getCurrentIdentity();
//        if(m!=null){
//            //Identity2.LOGGER.info("EntityLookupViewMixin not triggered!");
//        }
//        }
//        if(Identity2.indexOverrideActive!=0 && info.getReturnValue()!=null){
//        Entity returnvalue=((EntityAccessor)info.getReturnValue()).getIdentityOwner();
//        if(returnvalue!=null){
//            //Identity2.LOGGER.info("EntityLookupViewMixin trigger!");
//            info.setReturnValue(returnvalue);
//        }
//        }
//    }
//    @Inject(method = "getNearestEntity(Lnet/minecraft/tags/TagKey;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDDLnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/entity/LivingEntity;", at=@At("RETURN"),cancellable=true)
//    private static void useInject(TagKey<EntityType<?>> type, TargetingConditions predicate, @Nullable LivingEntity target, double x, double y, double z, AABB box,CallbackInfoReturnable info) {
//        if(info.getReturnValue()!=null){
//        Entity m=((EntityAccessor)info.getReturnValue()).getCurrentIdentity();
//        if(m!=null){
//            //Identity2.LOGGER.info("EntityLookupViewMixin not triggered!");
//        }
//        }
//        if(Identity2.indexOverrideActive!=0 && info.getReturnValue()!=null){
//        Entity returnvalue=((EntityAccessor)info.getReturnValue()).getIdentityOwner();
//        if(returnvalue!=null){
//            //Identity2.LOGGER.info("EntityLookupViewMixin trigger 2!");
//            info.setReturnValue(returnvalue);
//        }
//        }
//    }
//}
