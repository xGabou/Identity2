package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the in-wall texture from obscuring a tiny morph's first-person view. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true, require = 0)
    private static void identity2$hideTinyMorphOverlay(Player player, CallbackInfoReturnable<BlockState> cir) {
        if (identity2$isTinyMorph(player)) {
            cir.setReturnValue(null);
        }
    }

    private static boolean identity2$isTinyMorph(Player player) {
        if (player == null) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        return identity != null && identity.getBbHeight() < 1.0F;
    }
}
