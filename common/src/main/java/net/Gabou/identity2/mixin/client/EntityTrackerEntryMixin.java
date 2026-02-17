package net.Gabou.identity2.mixin.client;

import net.minecraft.registry.Registries;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.Gabou.identity2.Identity2Client;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.Gabou.identity2.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.item.Item;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.EnderDragonEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.minecraft.util.Identifier;
import net.Gabou.identity2.Identity2;
@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin{
    private List<DataTracker.SerializedEntry<?>> changedIdentityEntries;
    @Shadow
    private EntityTrackerEntry.TrackerPacketSender packetSender;
    @Inject(method = "<init>", at = @At("TAIL"))
    public void initmixin(
		ServerWorld world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, EntityTrackerEntry.TrackerPacketSender packetSender,CallbackInfo info
	){
        if(((EntityAccessor)entity).getCurrentIdentity()!=null){
            this.changedIdentityEntries=((EntityAccessor)entity).getCurrentIdentity().getDataTracker().getChangedEntries();
        }
    }
	@Shadow
    private Entity entity;
    
    @Inject(method = "syncEntityData", at = @At("HEAD"))
	private void onIdentityTrackerUpdate(CallbackInfo info) {
        if(((EntityAccessor)this.entity).getCurrentIdentity()!=null){
            DataTracker dataTracker = ((EntityAccessor)this.entity).getCurrentIdentity().getDataTracker();
            List<DataTracker.SerializedEntry<?>> list = dataTracker.getDirtyEntries();
            if (list != null) {
                this.changedIdentityEntries = dataTracker.getChangedEntries();
                this.packetSender.sendToSelfAndListeners(new EntityTrackerUpdateS2CPacket(-this.entity.getId(), list));
            }
        }
    }
}

