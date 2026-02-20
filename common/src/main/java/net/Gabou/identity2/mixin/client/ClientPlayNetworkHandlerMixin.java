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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import java.util.function.BiFunction;
import net.Gabou.identity2.Identity2;
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin{
	@Shadow
    private ClientLevel level;
    @Inject(method = "handleSetEntityData", at = @At("HEAD"))
	private void onIdentityTrackerUpdate(ClientboundSetEntityDataPacket packet,CallbackInfo info) {
        if(packet.id()<0){
            PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener)(Object)this, Minecraft.getInstance().packetProcessor());
            Entity entity = this.level.getEntity(-packet.id());
            if (entity != null) {
                Entity identity=((EntityAccessor)entity).getCurrentIdentity();
                if(identity!=null){
                    identity.getEntityData().assignValues(packet.packedItems());
                }
            }
        }
    }
}

