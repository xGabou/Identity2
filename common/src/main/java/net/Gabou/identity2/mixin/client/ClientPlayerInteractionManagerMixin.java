package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
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

        if (!IdentitySettings.enableFlight) {
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
        Entity currentIdentity = ((EntityAccessor) player).getCurrentIdentity();
        if (currentIdentity == null) {
            return;
        }
        if (currentIdentity.getType() == net.minecraft.world.entity.EntityTypes.SHULKER && PredefIdentityAbilities.isShulkerOpen(player)) {
            Identity2Client.sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_OVERRIDE_ATTACK);
            info.cancel();
            return;
        }

        IdentityAbilityDefinition identityAbility = ModRegistries.resolveIdentityAbility(
            currentIdentity.getType()
        );
        if (identityAbility != null && identityAbility.override_attack()) {
            Identity2Client.sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_OVERRIDE_ATTACK);
            info.cancel();
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(target instanceof Player targetPlayer)) {
            return;
        }
        if (!identity2$isVillagerLikeIdentity(targetPlayer)) {
            return;
        }

        Identity2Client.sendVillagerTradeRequest(targetPlayer.getUUID());
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    private static boolean identity2$isVillagerLikeIdentity(Player targetPlayer) {
        CompoundTag nbt = ((NbtComponentAccessor) (Object) ((EntityAccessor) targetPlayer).getCustomData()).getNbt();
        String selectedType = nbt.getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (selectedType.isBlank()) {
            selectedType = nbt.getStringOr("model_override", "");
        }
        return "minecraft:villager".equals(selectedType) || "minecraft:wandering_trader".equals(selectedType);
    }
}

