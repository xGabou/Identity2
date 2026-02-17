package net.Gabou.identity2.mixin.client;

import net.minecraft.registry.Registries;
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
import net.Gabou.identity2.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.item.Item;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.EnderDragonEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import net.minecraft.util.Identifier;
import net.Gabou.identity2.Identity2;
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin{
	@Shadow
    private ClientWorld world;
    @Inject(method = "onEntityTrackerUpdate", at = @At("HEAD"))
	private void onIdentityTrackerUpdate(EntityTrackerUpdateS2CPacket packet,CallbackInfo info) {
        if(packet.id()<0){
            NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler)(Object)this, MinecraftClient.getInstance().getPacketApplyBatcher());
            Entity entity = this.world.getEntityById(-packet.id());
            if (entity != null) {
                Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                if(identity!=null){
                    identity.getDataTracker().writeUpdatedEntries(packet.trackedValues());
                }
            }
        }
    }
}

