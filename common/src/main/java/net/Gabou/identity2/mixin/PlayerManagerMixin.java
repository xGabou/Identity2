package net.Gabou.identity2.mixin;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.MinecraftServerAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private static void removeInject(ServerPlayerEntity player, CallbackInfo info) {
        MinecraftServerAccessor accessor = (MinecraftServerAccessor) player.getEntityWorld().getServer();
        if (accessor.getCommandFunctionManager().getTag(Identifier.of(Identity2.MOD_ID, "on_before_player_leave")) != null) {
            for (CommandFunction<ServerCommandSource> function : accessor.getCommandFunctionManager()
                .getTag(Identifier.of(Identity2.MOD_ID, "on_before_player_leave"))) {
                accessor.getCommandFunctionManager().execute(
                    function,
                    player.getEntityWorld().getServer().getCommandSource().withEntity(player).withPosition(player.getEntityPos()).withSilent()
                );
            }
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private static void playerConnectInject(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo info) {
        ArrayList<CustomEntityDataS2CPacket.EntryBool> boolData = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.EntryString> stringData = new ArrayList<>(0);
        ArrayList<CustomEntityDataS2CPacket.Entry> floatData = new ArrayList<>(0);

        IdentityProgression.ensureClientUnlockCache(player);

        NbtComponent customData = ((EntityAccessor) player).getCustomData();
        NbtCompound nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        boolean identityDataSeen = false;

        for (String key : nbt.getKeys()) {
            if (nbt.getFloat(key).isPresent()) {
                floatData.add(new CustomEntityDataS2CPacket.Entry(key, nbt.getFloat(key, 0)));
            }
            if (nbt.getString(key).isPresent()) {
                stringData.add(new CustomEntityDataS2CPacket.EntryString(key, nbt.getString(key, "")));
                if (
                    "model_override".equals(key) ||
                    IdentityProgression.SELECTED_IDENTITY_TYPE_KEY.equals(key) ||
                    IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY.equals(key)
                ) {
                    identityDataSeen = true;
                }
            }
            if (nbt.getBoolean(key).isPresent()) {
                boolData.add(new CustomEntityDataS2CPacket.EntryBool(key, nbt.getBoolean(key, false)));
            }
        }
        if (identityDataSeen) {
            String type = nbt.getString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
            if (type.isBlank()) {
                type = nbt.getString("model_override", "");
            }
            if (!type.isBlank()) {
                ((EntityAccessor) player).setCurrentIdentity(type, IdentityProgression.parseVariantNbt(nbt.getString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "")));
            }
        }

        CustomEntityDataS2CPacketPayload floatPayload = new CustomEntityDataS2CPacketPayload(player.getId(), floatData);
        sendToWorldPlayers(player, floatPayload);
        NetworkManager.sendToPlayer(player, floatPayload);

        CustomEntityStringDataS2CPacketPayload stringPayload = new CustomEntityStringDataS2CPacketPayload(player.getId(), stringData);
        sendToWorldPlayers(player, stringPayload);
        NetworkManager.sendToPlayer(player, stringPayload);

        CustomEntityBoolDataS2CPacketPayload boolPayload = new CustomEntityBoolDataS2CPacketPayload(player.getId(), boolData);
        sendToWorldPlayers(player, boolPayload);
        NetworkManager.sendToPlayer(player, boolPayload);

        MinecraftServerAccessor accessor = (MinecraftServerAccessor) player.getEntityWorld().getServer();
        if (accessor.getCommandFunctionManager().getTag(Identifier.of(Identity2.MOD_ID, "on_before_player_join")) != null) {
            for (CommandFunction<ServerCommandSource> function : accessor.getCommandFunctionManager()
                .getTag(Identifier.of(Identity2.MOD_ID, "on_before_player_join"))) {
                accessor.getCommandFunctionManager().execute(
                    function,
                    player.getEntityWorld().getServer().getCommandSource().withEntity(player).withPosition(player.getEntityPos()).withSilent()
                );
            }
        }
    }

    private static <T extends net.minecraft.network.packet.CustomPayload> void sendToWorldPlayers(ServerPlayerEntity source, T payload) {
        if (source.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player != source) {
                    NetworkManager.sendToPlayer(player, payload);
                }
            }
        }
    }
}
