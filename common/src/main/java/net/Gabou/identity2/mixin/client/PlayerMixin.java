package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void identity2$forceFlyIdentity(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        Player player = (Player) (Object) this;
        if (player != client.player) {
            return;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode != null) {
            GameType mode = gameMode.getPlayerMode();
            if (mode == GameType.SPECTATOR) {
                return;
            }
        }

        if (!player.getAbilities().mayfly || player.getAbilities().instabuild) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            cir.setReturnValue(true);
        }
    }
}
