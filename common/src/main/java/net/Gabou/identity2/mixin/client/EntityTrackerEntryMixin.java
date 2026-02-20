package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.Gabou.identity2.ModEffects;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.EnderDragonEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.Gabou.identity2.Identity2;
@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin{
    private List<SynchedEntityData.DataValue<?>> changedIdentityEntries;
    @Shadow
    private ServerEntity.Synchronizer synchronizer;
    @Inject(method = "<init>", at = @At("TAIL"))
    public void initmixin(
		ServerLevel world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, ServerEntity.Synchronizer packetSender,CallbackInfo info
	){
        if(((EntityAccessor)entity).getCurrentIdentity()!=null){
            this.changedIdentityEntries=((EntityAccessor)entity).getCurrentIdentity().getEntityData().getNonDefaultValues();
        }
    }
	@Shadow
    private Entity entity;
    
    @Inject(method = "sendDirtyEntityData", at = @At("HEAD"))
	private void onIdentityTrackerUpdate(CallbackInfo info) {
        if(((EntityAccessor)this.entity).getCurrentIdentity()!=null){
            SynchedEntityData dataTracker = ((EntityAccessor)this.entity).getCurrentIdentity().getEntityData();
            List<SynchedEntityData.DataValue<?>> list = dataTracker.packDirty();
            if (list != null) {
                this.changedIdentityEntries = dataTracker.getNonDefaultValues();
                this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityDataPacket(-this.entity.getId(), list));
            }
        }
    }
}

