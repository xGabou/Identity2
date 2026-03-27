package net.Gabou.identity2.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.Gabou.identity2.identity.IdentityTraitTags;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyExpressionValue(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"
        ),
        require = 0
    )
    private boolean identity2$hideAquaticMorphAirBarUnderwater(
            boolean original
    ) {
        Player player = identity2$invokeGetCameraPlayer();
        if (original && identity2$isAquaticMorph(player)) {
            return false;
        }
        return original;
    }

    @Invoker("getCameraPlayer")
    protected abstract Player identity2$invokeGetCameraPlayer();

    @Unique
    private static boolean identity2$isAquaticMorph(Player player) {
        if (player == null) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        return identity != null && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(identity.getType()));
    }
}
