package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void identity2$clampMorphFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return;
        }

        if (((EntityAccessor) client.player).getCurrentIdentity() == null) {
            return;
        }

        Integer configuredFov = client.options.fov().get();
        if (configuredFov == null) {
            return;
        }

        double currentFov = cir.getReturnValue();
        double maxMorphFov = configuredFov.doubleValue() + 15.0D;
        if (currentFov > maxMorphFov) {
            cir.setReturnValue(maxMorphFov);
        }
    }
}
