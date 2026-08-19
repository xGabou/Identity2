package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class GameRendererMixin {
    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void identity2$clampMorphFov(float partialTick, CallbackInfoReturnable<Float> cir) {
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

        float currentFov = cir.getReturnValue();
        float maxMorphFov = configuredFov.floatValue() + 15.0F;
        if (currentFov > maxMorphFov) {
            cir.setReturnValue(maxMorphFov);
        }
    }
}
