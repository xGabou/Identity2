package draylar.identity.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import draylar.identity.ability.AbilityRegistry;
import draylar.identity.api.PlayerIdentity;
import draylar.identity.api.PlayerAbilities;
import draylar.identity.network.impl.FavoritePackets;
import draylar.identity.network.impl.SwapPackets;
import draylar.identity.network.impl.VillagerProfessionPackets;
import draylar.identity.network.impl.VillagerTradePackets;
import net.fabricmc.api.EnvType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import draylar.identity.network.impl.Payload.*;

public class ServerNetworking implements NetworkHandler {

    private static boolean s2cPayloadsRegistered = false;

    public static void initialize() {
        registerS2CPayloadTypes();
        FavoritePackets.registerFavoriteRequestHandler();
        SwapPackets.registerIdentityRequestPacketHandler();
        VillagerProfessionPackets.registerServerHandler();
        VillagerTradePackets.registerTradeRequestHandler();
    }

    private static void registerS2CPayloadTypes() {
        if (s2cPayloadsRegistered || Platform.getEnv().equals(EnvType.CLIENT)) {
            return;
        }
        NetworkManager.registerS2CPayloadType(IdentitySyncPayload.ID, IdentitySyncPayload.CODEC);
        NetworkManager.registerS2CPayloadType(AbilitySyncPayload.ID, AbilitySyncPayload.CODEC);
        NetworkManager.registerS2CPayloadType(FavoriteSyncPayload.ID, FavoriteSyncPayload.CODEC);
        NetworkManager.registerS2CPayloadType(UnlockSyncPayload.ID, UnlockSyncPayload.CODEC);
        NetworkManager.registerS2CPayloadType(VillagerIdentitiesSyncPayload.ID, VillagerIdentitiesSyncPayload.CODEC);
        NetworkManager.registerS2CPayloadType(OpenProfessionScreenPayload.ID, OpenProfessionScreenPayload.CODEC);
        NetworkManager.registerS2CPayloadType(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        s2cPayloadsRegistered = true;
    }

    public static void registerUseAbilityPacketHandler() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                UseAbilityPayload.ID,
                UseAbilityPayload.CODEC,
                (payload, context) -> {
                    PlayerEntity player = context.getPlayer();
                    context.queue(() -> {
                        LivingEntity identity = PlayerIdentity.getIdentity(player);

                        if (identity != null) {
                            EntityType<?> identityType = identity.getType();
                            if (AbilityRegistry.has(identityType) && PlayerAbilities.canUseAbility(player)) {
                                AbilityRegistry.get(identityType).onUse(player, identity, player.getWorld());
                                PlayerAbilities.setCooldown(player, AbilityRegistry.get(identityType).getCooldown(identity));
                                PlayerAbilities.sync((ServerPlayerEntity) player);
                            }
                        }
                    });
                }
        );

    }



}
