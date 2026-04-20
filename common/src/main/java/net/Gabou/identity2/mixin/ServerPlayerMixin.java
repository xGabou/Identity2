package net.Gabou.identity2.mixin;

import net.Gabou.identity2.identity.IdentityProgression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void identity2$onChangeDimension(DimensionTransition transition, CallbackInfoReturnable cir) {
        IdentityProgression.restoreMorphFromSavedDataAndSync((ServerPlayer) (Object) this);
    }
}
