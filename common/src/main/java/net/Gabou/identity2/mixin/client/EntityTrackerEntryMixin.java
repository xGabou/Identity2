package net.Gabou.identity2.mixin.client;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mixin(ServerEntity.class)
public abstract class EntityTrackerEntryMixin {

    private List<SynchedEntityData.DataValue<?>> changedIdentityEntries;

    @Shadow private Entity entity;

    @Shadow
    protected abstract void broadcastAndSend(Packet<?> packet);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void identity2$init(
            ServerLevel world,
            Entity entity,
            int tickInterval,
            boolean alwaysUpdateVelocity,
            Consumer<Packet<?>> watchingSender,
            BiConsumer<Packet<?>, List<UUID>> filteredWatchingSender,
            CallbackInfo ci
    ) {
        if (((EntityAccessor) entity).getCurrentIdentity() != null) {
            this.changedIdentityEntries = ((EntityAccessor) entity).getCurrentIdentity()
                    .getEntityData()
                    .getNonDefaultValues();
        }
    }

    @Inject(method = "sendDirtyEntityData", at = @At("HEAD"))
    private void identity2$onIdentityTrackerUpdate(CallbackInfo ci) {
        Entity currentIdentity = ((EntityAccessor) this.entity).getCurrentIdentity();
        if (currentIdentity == null) {
            return;
        }

        SynchedEntityData dataTracker = currentIdentity.getEntityData();
        List<SynchedEntityData.DataValue<?>> dirty = dataTracker.packDirty();
        if (dirty == null) {
            return;
        }

        this.changedIdentityEntries = dataTracker.getNonDefaultValues();
        this.broadcastAndSend(new ClientboundSetEntityDataPacket(-this.entity.getId(), dirty));
    }
}