package net.Gabou.identity2.mixin;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.auth.ClientLauncherGuards;
import net.Gabou.identity2.auth.TLauncherDetectedHandler;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.progression.MorphChargeManager;
import net.Gabou.identity2.progression.ProgressionConfig;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.MinecraftServerAccessor;
import net.Gabou.identity2.util.NetworkCompat;
import net.Gabou.identity2.util.NetworkPayload;
import net.Gabou.identity2.util.PlayerManagerAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin implements PlayerManagerAccessor {
    private static final int DELAYED_MORPH_REAPPLY_TICKS = 20;
    private static final Map<UUID, Integer> DELAYED_MORPH_REAPPLY = new HashMap<>();

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private MinecraftServer server;

    @Shadow
    public ServerPlayer getPlayer(UUID uuid) {
        return null;
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"), cancellable = true)
    private void identity2$authOnLogin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo info) {
        if (!net.Gabou.identity2.auth.ServerAuth.onLogin(connection, player)) {
            info.cancel();
            return;
        }

        String launcherReason = ClientLauncherGuards.getDetectedReason();
        if (launcherReason != null && player.level() instanceof ServerLevel serverLevel) {
            TLauncherDetectedHandler.handle(serverLevel, player, launcherReason);
            info.cancel();
            return;
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void removeInject(ServerPlayer player, CallbackInfo info) {
        net.Gabou.identity2.auth.ServerAuth.onLogout(player);
        DELAYED_MORPH_REAPPLY.remove(player.getUUID());
        MinecraftServerAccessor accessor = (MinecraftServerAccessor) player.level().getServer();
        if (accessor.getCommandFunctionManager().getTag(new ResourceLocation(Identity2.MOD_ID, "on_before_player_leave")) != null) {
            for (CommandFunction function : accessor.getCommandFunctionManager()
                .getTag(new ResourceLocation(Identity2.MOD_ID, "on_before_player_leave"))) {
                accessor.getCommandFunctionManager().execute(
                    function,
                    player.level().getServer().createCommandSourceStack().withEntity(player).withPosition(player.position()).withSuppressedOutput()
                );
            }
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void playerConnectInject(Connection connection, ServerPlayer player, CallbackInfo info) {
        if (!net.Gabou.identity2.auth.PendingAuthManager.isPending(player.getUUID())) {
            return;
        }

        net.Gabou.identity2.auth.ServerAuth.sendChallenge(player);

        ArrayList<CustomEntityDataS2CPacket.EntryBool> boolData = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.EntryString> stringData = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.Entry> doubleData = new ArrayList<>(0);

        IdentityProgression.ensureClientUnlockCache(player);
        IdentityProgression.restoreMorphFromSavedData(player);

        CompoundTag nbt = ((EntityAccessor) player).getCustomData();
        for (String key : net.Gabou.identity2.util.NbtCompat.keySet(nbt)) {
            Tag raw = nbt.get(key);
            if (raw == null) {
                continue;
            }
            byte id = raw.getId();
            if (id >= Tag.TAG_BYTE && id <= Tag.TAG_DOUBLE) {
                doubleData.add(new CustomEntityDataS2CPacket.Entry(key, net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, key, 0.0)));
            }
            if (id == Tag.TAG_STRING) {
                stringData.add(new CustomEntityDataS2CPacket.EntryString(key, net.Gabou.identity2.util.NbtCompat.getStringOr(nbt, key, "")));
            }
            if (id == Tag.TAG_BYTE) {
                boolData.add(new CustomEntityDataS2CPacket.EntryBool(key, net.Gabou.identity2.util.NbtCompat.getBooleanOr(nbt, key, false)));
            }
        }

        CustomEntityDataS2CPacketPayload doublePayload = new CustomEntityDataS2CPacketPayload(player.getId(), doubleData);
        sendToWorldPlayers(player, doublePayload);
        NetworkCompat.sendToPlayer(player, doublePayload);

        CustomEntityStringDataS2CPacketPayload stringPayload = new CustomEntityStringDataS2CPacketPayload(player.getId(), stringData);
        sendToWorldPlayers(player, stringPayload);
        NetworkCompat.sendToPlayer(player, stringPayload);

        CustomEntityBoolDataS2CPacketPayload boolPayload = new CustomEntityBoolDataS2CPacketPayload(player.getId(), boolData);
        sendToWorldPlayers(player, boolPayload);
        NetworkCompat.sendToPlayer(player, boolPayload);
        IdentityProgression.syncUnlockedIdentities(player);

        // Re-apply morph shape one second later to avoid login-time race conditions
        // where dimensions are still being initialized by vanilla/mods.
        DELAYED_MORPH_REAPPLY.put(player.getUUID(), DELAYED_MORPH_REAPPLY_TICKS);

        MinecraftServerAccessor accessor = (MinecraftServerAccessor) player.level().getServer();
        if (accessor.getCommandFunctionManager().getTag(new ResourceLocation(Identity2.MOD_ID, "on_before_player_join")) != null) {
            for (CommandFunction function : accessor.getCommandFunctionManager()
                .getTag(new ResourceLocation(Identity2.MOD_ID, "on_before_player_join"))) {
                accessor.getCommandFunctionManager().execute(
                    function,
                    player.level().getServer().createCommandSourceStack().withEntity(player).withPosition(player.position()).withSuppressedOutput()
                );
            }
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "To keep your morph after death, use: /identity config set deathMorphRule none"
            ),
            false
        );
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void identity2$delayedMorphReapply(CallbackInfo info) {
        net.Gabou.identity2.auth.ServerAuth.onTick(this.server);
        if (DELAYED_MORPH_REAPPLY.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = DELAYED_MORPH_REAPPLY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }

            ServerPlayer player = this.getPlayer(entry.getKey());
            if (player != null) {
                IdentityProgression.restoreMorphFromSavedDataAndSync(player);
                IdentityProgression.refreshScaledHealth(player);
                IdentityProgression.syncUnlockedIdentities(player);
            }
            iterator.remove();
        }
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void identity2$onRespawn(ServerPlayer player, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer respawned = cir.getReturnValue();
        if (respawned == null) {
            return;
        }
        identity2$copyCustomData(player, respawned);
        WardenBurrowManager.stop(respawned, false);
        boolean alive = !player.isDeadOrDying();

        if (alive) {
            IdentityProgression.restoreMorphFromSavedDataAndSync(respawned);
            identity2$syncUnlockedIdentities(respawned);
            DELAYED_MORPH_REAPPLY.put(respawned.getUUID(), DELAYED_MORPH_REAPPLY_TICKS);
            return;
        }
        IdentitySettings.DeathMorphRule rule =
            IdentitySettings.getEffectiveDeathMorphRule(respawned.level().getServer());

        switch (rule) {
            case WIPE_ALL -> {
                IdentityProgression.clearUnlockedIdentities(respawned);
                IdentityProgression.clearMorph(respawned);
            }
            case REVOKE_ACTIVE -> IdentityProgression.clearMorph(respawned);
            case NONE -> {
                MorphChargeManager.applyDeathPenalty(respawned);
                IdentityProgression.restoreMorphFromSavedDataAndSync(respawned);
            }
        }

        identity2$syncUnlockedIdentities(respawned);
        DELAYED_MORPH_REAPPLY.put(respawned.getUUID(), DELAYED_MORPH_REAPPLY_TICKS);
    }

    private static void identity2$copyCustomData(ServerPlayer source, ServerPlayer target) {
        if (source == null || target == null || source == target) {
            return;
        }
        CompoundTag sourceNbt = ((EntityAccessor) source).getCustomData();
        if (sourceNbt == null || sourceNbt.isEmpty()) {
            return;
        }
        CompoundTag targetNbt = ((EntityAccessor) target).getCustomData();
        targetNbt.merge(sourceNbt.copy());
    }

    private static void identity2$syncUnlockedIdentities(ServerPlayer player) {
        IdentityProgression.syncUnlockedIdentities(player);
    }

    @Unique
    @Override
    public void identity2$queueDelayedMorphReapply(ServerPlayer player) {
        DELAYED_MORPH_REAPPLY.put(player.getUUID(), DELAYED_MORPH_REAPPLY_TICKS);
    }

    private static <T extends NetworkPayload> void sendToWorldPlayers(ServerPlayer source, T payload) {
        if (source.level() instanceof ServerLevel serverWorld) {
            for (ServerPlayer player : serverWorld.players()) {
                if (player != source) {
                    NetworkCompat.sendToPlayer(player, payload);
                }
            }
        }
    }
}
