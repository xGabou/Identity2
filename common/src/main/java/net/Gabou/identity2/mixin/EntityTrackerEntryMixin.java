package net.Gabou.identity2.mixin;

import dev.architectury.networking.NetworkManager;
import net.Gabou.identity2.util.NetworkCompat;
import java.util.ArrayList;
import java.util.List;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin {
    @Shadow
    private Entity entity;

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void sendCustomDataPackets(ServerPlayer player, CallbackInfo info) {
        ArrayList<CustomEntityDataS2CPacket.Entry> doubleValues = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.EntryString> stringValues = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.EntryBool> boolValues = new ArrayList<>(0);
        CompoundTag data = ((EntityAccessor) this.entity).getCustomData();

        for (String key : net.Gabou.identity2.util.NbtCompat.keySet(data)) {
            Tag value = data.get(key);
            if (value == null) {
                continue;
            }
            if (value.getId() == Tag.TAG_STRING) {
                stringValues.add(new CustomEntityDataS2CPacket.EntryString(key, data.getString(key)));
            }
            if (value.getId() == Tag.TAG_BYTE) {
                boolValues.add(new CustomEntityDataS2CPacket.EntryBool(key, data.getBoolean(key)));
            }
            if (value.getId() >= Tag.TAG_BYTE && value.getId() <= Tag.TAG_DOUBLE) {
                doubleValues.add(new CustomEntityDataS2CPacket.Entry(key, data.getDouble(key)));
            }
        }

        if (!doubleValues.isEmpty()) {
            NetworkCompat.sendToPlayer(player, new CustomEntityDataS2CPacketPayload(this.entity.getId(), doubleValues));
        }
        if (!stringValues.isEmpty()) {
            NetworkCompat.sendToPlayer(player, new CustomEntityStringDataS2CPacketPayload(this.entity.getId(), stringValues));
        }
        if (!boolValues.isEmpty()) {
            NetworkCompat.sendToPlayer(player, new CustomEntityBoolDataS2CPacketPayload(this.entity.getId(), boolValues));
        }

        Entity currentIdentity = ((EntityAccessor) this.entity).getCurrentIdentity();
        if (currentIdentity == null) {
            return;
        }

        List<SynchedEntityData.DataValue<?>> trackedValues = currentIdentity.getEntityData().getNonDefaultValues();
        if (trackedValues != null && !trackedValues.isEmpty()) {
            player.connection.send(new ClientboundSetEntityDataPacket(-this.entity.getId(), trackedValues));
        }
    }
}



