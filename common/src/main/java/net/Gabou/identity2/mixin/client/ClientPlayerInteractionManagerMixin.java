package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void forceFlyIdentity(CallbackInfoReturnable<Boolean> info) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity != null && ((EntityAccessor) identity).canFly()) {
            // Keep flight unlocked for fly-capable identities.
            info.setReturnValue(false);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Player player, Entity target, CallbackInfo info) {
        if (((EntityAccessor) player).getCurrentIdentity() == null) {
            return;
        }

        Registry<IdentityAbilityDefinition> identityAbilityRegistry = ModRegistries.getIdentityAbilityRegistry();
        if (identityAbilityRegistry == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = identityAbilityRegistry.getValue(
            net.minecraft.world.entity.EntityType.getKey(((EntityAccessor) player).getCurrentIdentity().getType())
        );
        if (identityAbility != null) {
            Identity2Client.sendIdentityAbilityPacket(-3);
            if (identityAbility.override_attack()) {
                info.cancel();
            }
        }
    }
}

