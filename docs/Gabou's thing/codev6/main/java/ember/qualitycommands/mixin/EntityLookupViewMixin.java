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
import ember.qualitycommands.QualityCommands;
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
import ember.qualitycommands.ModComponents;
import ember.qualitycommands.util.EntityAccessor;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityLookupView;
@Mixin(EntityLookupView.class)
public interface EntityLookupViewMixin{
    
    @Inject(method = "getClosestEntity(Ljava/util/List;Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/LivingEntity;", at=@At("RETURN"),cancellable=true)
    private static void useInject(List<?> entities, TargetPredicate targetPredicate, @Nullable LivingEntity entity, double x, double y, double z,CallbackInfoReturnable info) {
        try{
        if(QualityCommands.indexOverrideActive!=0){
        for(Object m:entities){
			if(m instanceof Entity n){
				if(((EntityAccessor)n).getCurrentIdentity()!=null){
					//QualityCommands.LOGGER.info("Entity with identity made it to the final step");
				}
				if(((EntityAccessor)n).getIdentityOwner()!=null){
					//QualityCommands.LOGGER.info("Identity entity made it to the final step");
				}
			}
		}
        }
        }catch(Exception e){
            int b=0;
        }
        //QualityCommands.LOGGER.info("EntityLookupViewMixin check!");
        if(info.getReturnValue()!=null){
        Entity m=((EntityAccessor)info.getReturnValue()).getCurrentIdentity();
        if(m!=null){
            //QualityCommands.LOGGER.info("EntityLookupViewMixin not triggered!");
        }
        }
        if(info.getReturnValue()!=null){
        Entity returnvalue=((EntityAccessor)info.getReturnValue()).getIdentityOwner();
        if(returnvalue!=null){
            //QualityCommands.LOGGER.info("EntityLookupViewMixin trigger!");
            info.setReturnValue(returnvalue);
        }
    }
    }
    @Inject(method = "getClosestEntity(Lnet/minecraft/registry/tag/TagKey;Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDDLnet/minecraft/util/math/Box;)Lnet/minecraft/entity/LivingEntity;", at=@At("RETURN"),cancellable=true)
    private static void useInject(TagKey<EntityType<?>> type, TargetPredicate predicate, @Nullable LivingEntity target, double x, double y, double z, Box box,CallbackInfoReturnable info) {
        if(info.getReturnValue()!=null){
        Entity m=((EntityAccessor)info.getReturnValue()).getCurrentIdentity();
        if(m!=null){
            //QualityCommands.LOGGER.info("EntityLookupViewMixin not triggered!");
        }
        }
        if(info.getReturnValue()!=null){
        Entity returnvalue=((EntityAccessor)info.getReturnValue()).getIdentityOwner();
        if(returnvalue!=null){
            //QualityCommands.LOGGER.info("EntityLookupViewMixin trigger 2!");
            info.setReturnValue(returnvalue);
        }
    }
    }
}
