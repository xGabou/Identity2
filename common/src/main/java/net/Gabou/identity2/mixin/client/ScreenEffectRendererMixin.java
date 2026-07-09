package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.identity.SilverfishBurrowManager;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    /**
     * While a silverfish morph is burrowed inside a block, the camera sits inside
     * that block and vanilla fills the whole screen with the block's texture
     * ("suffocation" overlay). Suppress it so the player can still see out.
     */
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void identity2$hideBurrowOverlay(Player player, CallbackInfoReturnable<BlockState> cir) {
        if (SilverfishBurrowManager.isHidden(player)) {
            cir.setReturnValue(null);
        }
    }
}
