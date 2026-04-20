package ember.qualitycommands.mixin.client;

import net.minecraft.registry.Registries;
import ember.qualitycommands.QualityCommandsClient;
import ember.qualitycommands.packets.IdentityAbilityPacketPayload;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;
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

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import ember.qualitycommands.ModEffects;
import ember.qualitycommands.ModRegistries;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.IdentityAbilityDefinition;
import ember.qualitycommands.util.EnderDragonEntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import net.minecraft.util.Identifier;
import ember.qualitycommands.QualityCommands;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin{
    @Inject(method = "isFlyingLocked", at = @At("HEAD"),cancellable=true)
	private void forceFlyIdentity(CallbackInfoReturnable info) {
        PlayerEntity player=MinecraftClient.getInstance().player;
        if(((EntityAccessor)player).getCurrentIdentity()!=null){
            if(((EntityAccessor)((EntityAccessor)player).getCurrentIdentity()).canFly()){
                info.setReturnValue(true);
            }
        }
    }
    @Inject(method = "attackEntity", at = @At("HEAD"),cancellable = true)
	private void onAttackEntity(PlayerEntity player,Entity target,CallbackInfo info) {
        if(((EntityAccessor)player).getCurrentIdentity()!=null){
            IdentityAbilityDefinition identityAbility = ModRegistries.identityAbilityRegistry.get(net.minecraft.entity.EntityType.getId(((EntityAccessor)player).getCurrentIdentity().getType()));
            if(identityAbility!=null){
                QualityCommands.LOGGER.info("trying ability attack");
                ClientPlayNetworking.send(new IdentityAbilityPacketPayload(-3));
                if(identityAbility.override_attack()){
                    info.cancel();
                }
            }
        }
    }
}
