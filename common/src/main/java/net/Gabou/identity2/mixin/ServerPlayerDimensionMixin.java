package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerDimensionMixin {
    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"))
    private void identity2$discardIdentityBeforeDimensionTravel(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Entity currentIdentity = ((EntityAccessor) player).getCurrentIdentity();
        if (currentIdentity != null) {
            currentIdentity.discard();
            ((EntityAccessor) player).setCurrentIdentity((Entity) null);
        }
        if (player.level() instanceof ServerLevel level) {
            identity2$pruneAttachedIdentities(level, player);
        }
    }

    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void identity2$pruneIdentityAfterDimensionTravel(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level() instanceof ServerLevel level) {
            identity2$pruneAttachedIdentities(level, player);
        }
    }

    private static void identity2$pruneAttachedIdentities(ServerLevel level, ServerPlayer player) {
        int hostId = Math.abs(player.getId());
        for (Entity entity : level.getAllEntities()) {
            if (entity == player) {
                continue;
            }
            Entity owner = ((EntityAccessor) entity).getIdentityOwner();
            if (owner == player || Math.abs(entity.getId()) == hostId) {
                entity.discard();
            }
        }
    }
}
