package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void identity2$clampMorphFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return;
        }

        if (((EntityAccessor) client.player).getCurrentIdentity() == null) {
            return;
        }

        Integer configuredFov = client.options.getFov().getValue();
        if (configuredFov == null) {
            return;
        }

        float currentFov = cir.getReturnValue();
        float maxMorphFov = configuredFov.floatValue() + 15.0F;
        if (currentFov > maxMorphFov) {
            cir.setReturnValue(maxMorphFov);
        }
    }
}
