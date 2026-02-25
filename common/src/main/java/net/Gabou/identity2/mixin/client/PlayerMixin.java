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
        Player player = (Player) (Object)this;

        // Si le joueur est vraiment en spectator, ne touche pas
        // Important sinon tu brises les vraies regles spectator
        if (player.getAbilities().instabuild == false && player.isCreative() == false) {
            // rien ici, juste pour montrer que tu ne dois pas te baser sur isSpectator recursif
        }

        // Ne pas appeler player.isSpectator() ici sinon recursion
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            GameType mode = gameMode.getPlayerMode(); // existe en 1.21.11 :contentReference[oaicite:2]{index=2}
            if (mode == GameType.SPECTATOR) {
                return;
            }
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            // Si ton but est de debarrer le vol, pretendre spectator ici est souvent ce que tu veux
            // Si au contraire tu voulais empecher spectator, mets false
            cir.setReturnValue(true);
        }
    }
}
