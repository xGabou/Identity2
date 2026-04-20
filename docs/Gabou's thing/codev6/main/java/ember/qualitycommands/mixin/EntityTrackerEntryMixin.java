package ember.qualitycommands.mixin;
import com.google.common.collect.Lists;
import net.minecraft.util.math.MathHelper;
import java.util.List;
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
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import net.minecraft.registry.tag.FluidTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.BundleItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import ember.qualitycommands.ModComponents;
import net.minecraft.server.network.EntityTrackerEntry;

import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import java.util.function.Consumer;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.component.type.NbtComponent;
import ember.qualitycommands.packets.CustomEntityBoolDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityDataS2CPacket;
import ember.qualitycommands.packets.CustomEntityDataS2CPacketPayload;
import ember.qualitycommands.packets.CustomEntityNBTDataS2CPacketPayload;

import java.util.ArrayList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.world.ServerWorld;
import ember.qualitycommands.packets.CustomEntityStringDataS2CPacketPayload;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin{
    @Shadow
    Entity entity;
    @Inject(method = "startTracking", at=@At("TAIL"))
    private void sendCustomDataPackets(ServerPlayerEntity player,CallbackInfo info) {
        ArrayList<CustomEntityDataS2CPacket.Entry> doubleValues=new ArrayList(0);
        ArrayList<CustomEntityDataS2CPacket.EntryString> stringValues=new ArrayList(0);
        ArrayList<CustomEntityDataS2CPacket.EntryBool> boolValues=new ArrayList(0);
        ArrayList<CustomEntityDataS2CPacket.EntryNBT> compoundValues=new ArrayList(0);
        NbtCompound data=((NbtComponentAccessor)(Object)((EntityAccessor)this.entity).getCustomData()).getNbt();
        for(String key:data.getKeys()){
            Optional<String> strKey=data.getString(key);
            if(strKey.isPresent()){
                stringValues.ensureCapacity(stringValues.size()+1);
                stringValues.add(new CustomEntityDataS2CPacket.EntryString(key,strKey.get()));
            }
            Optional<Double> doubleKey=data.getDouble(key);
            if(doubleKey.isPresent()){
                doubleValues.ensureCapacity(doubleValues.size()+1);
                doubleValues.add(new CustomEntityDataS2CPacket.Entry(key,doubleKey.get()));
            }
            Optional<Boolean> boolKey=data.getBoolean(key);
            if(boolKey.isPresent()){
                boolValues.ensureCapacity(doubleValues.size()+1);
                boolValues.add(new CustomEntityDataS2CPacket.EntryBool(key,boolKey.get()));
            }
            Optional<NbtCompound> compoundKey=data.getCompound(key);
            if(compoundKey.isPresent()){
                compoundValues.ensureCapacity(compoundValues.size()+1);
                compoundValues.add(new CustomEntityDataS2CPacket.EntryNBT(key,compoundKey.get()));
            }
            
        }
        if(doubleValues.size()!=0){
            ServerPlayNetworking.send(player, new CustomEntityDataS2CPacketPayload(this.entity.getId(),doubleValues));
            //sender.accept(new CustomEntityDataS2CPacketPayload(this.entity.getId(),doubleValues));
        }
        if(stringValues.size()!=0){
            ServerPlayNetworking.send(player, new CustomEntityStringDataS2CPacketPayload(this.entity.getId(),stringValues));
            //sender.accept(new CustomEntityStringDataS2CPacketPayload(this.entity.getId(),stringValues));
        }
        if(boolValues.size()!=0){
            ServerPlayNetworking.send(player, new CustomEntityBoolDataS2CPacketPayload(this.entity.getId(),boolValues));
            //sender.accept(new CustomEntityStringDataS2CPacketPayload(this.entity.getId(),stringValues));
        }
        if(compoundValues.size()!=0){
            ServerPlayNetworking.send(player, new CustomEntityNBTDataS2CPacketPayload(this.entity.getId(),compoundValues));
            //sender.accept(new CustomEntityStringDataS2CPacketPayload(this.entity.getId(),stringValues));
        }
    }
}
