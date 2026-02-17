package net.Gabou.identity2.mixin;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.Optional;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
    @Shadow
    private Entity entity;

    @Inject(method = "startTracking", at = @At("TAIL"))
    private void sendCustomDataPackets(ServerPlayerEntity player, CallbackInfo info) {
        ArrayList<CustomEntityDataS2CPacket.Entry> doubleValues = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.EntryString> stringValues = new ArrayList<>(0);
        NbtCompound data = ((NbtComponentAccessor) (Object) ((EntityAccessor) this.entity).getCustomData()).getNbt();

        for (String key : data.getKeys()) {
            Optional<String> strKey = data.getString(key);
            if (strKey.isPresent()) {
                stringValues.add(new CustomEntityDataS2CPacket.EntryString(key, strKey.get()));
            }
            Optional<Double> doubleKey = data.getDouble(key);
            if (doubleKey.isPresent()) {
                doubleValues.add(new CustomEntityDataS2CPacket.Entry(key, doubleKey.get()));
            }
        }

        if (!doubleValues.isEmpty()) {
            NetworkManager.sendToPlayer(player, new CustomEntityDataS2CPacketPayload(this.entity.getId(), doubleValues));
        }
        if (!stringValues.isEmpty()) {
            NetworkManager.sendToPlayer(player, new CustomEntityStringDataS2CPacketPayload(this.entity.getId(), stringValues));
        }
    }
}
