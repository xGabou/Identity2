package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDimensionMixin {
    @Unique
    private boolean identity2$changingDimension;

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void identity2$discardAttachedIdentityOnDimensionChange(
        TeleportTransition teleportTransition,
        CallbackInfoReturnable<ServerPlayer> cir
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (teleportTransition.newLevel().dimension() == player.level().dimension()) {
            return;
        }

        this.identity2$changingDimension = true;
        identity2$pruneAttachedIdentityEntities(player, player.level(), player.position());
        Entity currentIdentity = ((EntityAccessor) player).getCurrentIdentity();
        if (currentIdentity != null) {
            currentIdentity.discard();
            ((EntityAccessor) player).setCurrentIdentity((Entity) null);
        }
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
    private void identity2$pruneAttachedIdentityAfterDimensionChange(
        TeleportTransition teleportTransition,
        CallbackInfoReturnable<ServerPlayer> cir
    ) {
        if (!this.identity2$changingDimension) {
            return;
        }
        this.identity2$changingDimension = false;

        ServerPlayer player = cir.getReturnValue();
        if (player == null) {
            return;
        }

        identity2$pruneAttachedIdentityEntities(player, player.level(), player.position());
    }

    @Unique
    private static void identity2$pruneAttachedIdentityEntities(ServerPlayer player, ServerLevel level, net.minecraft.world.phys.Vec3 position) {
        if (player == null || level == null || position == null) {
            return;
        }

        List<Entity> toRemove = new ArrayList<>();
        int playerId = player.getId();
        for (Entity entity : level.getAllEntities()) {
            if (entity == player) {
                continue;
            }
            EntityAccessor accessor = (EntityAccessor) entity;
            if (accessor.getIdentityOwner() == player) {
                toRemove.add(entity);
                continue;
            }
            if ((entity.getId() == playerId || entity.getId() == -playerId) && entity.distanceToSqr(position) < 256.0D) {
                toRemove.add(entity);
            }
        }

        for (Entity entity : toRemove) {
            entity.discard();
        }
    }
}
