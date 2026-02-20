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
import net.minecraft.world.level.Level;
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
@Mixin(Level.class)
public class WorldMixin{
    
    private static final int HORIZONTAL_LIMIT = Identity2.maxWorldSize;
    @ModifyConstant(constant=@Constant(intValue=30000000),method="isInWorldBoundsHorizontal")
    private static int isValidHorizontallyA(int x){
        return Identity2.maxWorldSize;
    }
    @ModifyConstant(constant=@Constant(intValue=-30000000),method="isInWorldBoundsHorizontal")
    private static int isValidHorizontallyB(int x){
        return -Identity2.maxWorldSize;
    }
    /*private static boolean isValidHorizontally(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }
    public int getTopY(Heightmap.Type heightmap, int x, int z) {
          if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {*/
    @ModifyConstant(constant=@Constant(intValue=30000000),method="getHeight")
    private static int getTopYA(int x){
        return Identity2.maxWorldSize;
    }
    @ModifyConstant(constant=@Constant(intValue=-30000000),method="getHeight")
    private static int getTopYB(int x){
        return -Identity2.maxWorldSize;
    }
}

