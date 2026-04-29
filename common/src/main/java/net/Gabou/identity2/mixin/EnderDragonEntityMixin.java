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
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
@Mixin(targets = "net.minecraft.world.entity.boss.enderdragon.EnderDragon")
public class EnderDragonEntityMixin implements net.Gabou.identity2.util.EnderDragonEntityAccessor{
    @Shadow
    public void checkCrystals(){};
    @Shadow
    private int growlTime;
    public int setTicksUntilNextGrowl(int ticks){return this.growlTime=ticks;};
    public int getTicksUntilNextGrowl(){return this.growlTime;};
    public void runTickWithEndCrystals(){this.checkCrystals();};

    private boolean identity2$shouldIgnorePlayerOwnedDragonAttack(DamageSource source) {
        if (!(((EntityAccessor) this).getIdentityOwner() instanceof Player)) {
            return false;
        }
        if (source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return source.is(DamageTypes.MOB_ATTACK) && source.getDirectEntity() == (Object) this && source.getEntity() == (Object) this;
    }
}

