package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNametagMixin {
    @Inject(method = "shouldShowName()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void identity2$hideIdentityNametag(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player)) {
            return;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean isLocalPlayer = minecraft != null && minecraft.player == player;
        if (isLocalPlayer) {
            if (!IdentitySettings.renderOwnNametag) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (!IdentitySettings.showPlayerNametag) {
            cir.setReturnValue(false);
        }
    }
}
