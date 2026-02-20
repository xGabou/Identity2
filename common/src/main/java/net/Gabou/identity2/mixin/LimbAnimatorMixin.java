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
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.minecraft.world.entity.WalkAnimationState;
@Mixin(WalkAnimationState.class)
public class LimbAnimatorMixin implements LimbAnimatorAccessor{
    @Shadow
    private float speedOld;
    @Shadow
    private float speed;
    public void setPrevSpeed(float lastSpeed){
        this.speedOld=lastSpeed;
    };
    @Shadow
	public void setSpeed(float speed){
        this.speed=speed;
    };
	public float getPrevSpeed(){
        return speedOld;
    };
    @Shadow
	public float speed(){
        return speed;
    };
    @Override
    public float getSpeed() {
        return this.speed();
    }
}

