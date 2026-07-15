package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.Gabou.identity2.identity.SilverfishBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    /**
     * While a silverfish morph is burrowed inside a block, the camera sits inside
     * that block and vanilla fills the whole screen with the block's texture
     * ("suffocation" overlay). Suppress it so the player can still see out.
     *
     * Fabric only in practice: NeoForge's patched renderScreenEffect resolves the
     * overlay through its own getOverlayBlock(Player) and never calls this method;
     * Identity2NeoForgeClient cancels RenderBlockScreenEffectEvent instead.
     */
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void identity2$hideBurrowOverlay(Player player, CallbackInfoReturnable<BlockState> cir) {
        if (SilverfishBurrowManager.shouldSuppressBlockOverlay(player)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Snow layers below 8 layers are not view-blocking, so vanilla never draws an
     * in-wall overlay for them. Tiny morphs (silverfish/endermite eye height ~0.13)
     * walk with the camera inside the snow layer's space and would see straight
     * through the terrain. Draw the snow overlay ourselves in that case; vanilla
     * then finds no view-blocking state, so nothing is drawn twice.
     */
    @Inject(method = "renderScreenEffect", at = @At("HEAD"))
    private static void identity2$renderSnowLayerOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.level == null) {
            return;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null
                || (identity.getType() != EntityType.SILVERFISH && identity.getType() != EntityType.ENDERMITE)) {
            return;
        }

        Vec3 eyePosition = player.getEyePosition();
        BlockPos eyeBlockPos = BlockPos.containing(eyePosition);
        BlockState state = minecraft.level.getBlockState(eyeBlockPos);
        if (!(state.getBlock() instanceof SnowLayerBlock)) {
            return;
        }
        double snowTop = eyeBlockPos.getY() + state.getValue(SnowLayerBlock.LAYERS) * 0.125D;
        if (eyePosition.y >= snowTop) {
            return;
        }

        ScreenEffectRendererInvoker.identity2$renderTex(
                minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state),
                poseStack
        );
    }
}
