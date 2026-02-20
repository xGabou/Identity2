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
import net.minecraft.world.level.border.WorldBorder;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.Gabou.identity2.ModComponents;
import net.Gabou.identity2.Identity2;
import org.spongepowered.asm.mixin.Overwrite;
@Mixin(WorldBorder.class)
public class WorldBorderMixin{
    @Shadow
    public static final double MAX_CENTER_COORDINATE = Identity2.maxWorldSize;
    @Shadow
    int absoluteMaxSize = Identity2.maxWorldSize-16;
    @Mixin(WorldBorder.Settings.class)
    public static class Properties{
        @ModifyConstant(constant=@Constant(doubleValue=-2.9999984E7),method="fromDynamic")
        private static double TDIOA(double x){
            return -Identity2.maxWorldSize+16;
        }
        @ModifyConstant(constant=@Constant(doubleValue=2.9999984E7),method="fromDynamic")
        private static double TDIOB(double x){
            return Identity2.maxWorldSize-16;
        }
    }
}

