package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "isFlyingLocked", at = @At("HEAD"), cancellable = true)
    private void forceFlyIdentity(CallbackInfoReturnable<Boolean> info) {
        PlayerEntity player = MinecraftClient.getInstance().player;
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

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        if (((EntityAccessor) player).getCurrentIdentity() == null) {
            return;
        }

        Registry<IdentityAbilityDefinition> identityAbilityRegistry = ModRegistries.getIdentityAbilityRegistry();
        if (identityAbilityRegistry == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = identityAbilityRegistry.get(
            net.minecraft.entity.EntityType.getId(((EntityAccessor) player).getCurrentIdentity().getType())
        );
        if (identityAbility != null) {
            Identity2Client.sendIdentityAbilityPacket(-3);
            if (identityAbility.override_attack()) {
                info.cancel();
            }
        }
    }
}

