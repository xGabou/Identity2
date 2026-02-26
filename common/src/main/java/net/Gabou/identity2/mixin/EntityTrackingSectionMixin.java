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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntityTypeTest;
@Mixin(EntitySection.class)
public class EntityTrackingSectionMixin{
    @Redirect(
        method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)Lnet/minecraft/util/AbortableIterationConsumer$Continuation;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTypeTest;tryCast(Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 0
    )
    private <T,U extends T> U replaceDowncast(EntityTypeTest<T,U> filter, T toDowncast) {
        if(net.Gabou.identity2.Identity2.indexOverrideActive!=0){
            try{
                if (!(toDowncast instanceof Entity hostEntity) || hostEntity instanceof net.minecraft.world.entity.player.Player) {
                    return filter.tryCast(toDowncast);
                }
                Entity identity = ((EntityAccessor) toDowncast).getCurrentIdentity();
                if (identity != null && hostEntity.getClass().isAssignableFrom(identity.getClass())) {
                    toDowncast=(T) identity;
                    return filter.tryCast(toDowncast);
                }
            }catch(Exception e){
                int x=0;
            }
        }//else, have a nice day without awkwardness
        return filter.tryCast(toDowncast);
    }
}
