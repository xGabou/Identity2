package net.Gabou.identity2.progression;

import dev.architectury.networking.NetworkManager;
import java.util.Map;
import net.Gabou.identity2.packets.ProgressionJarStateS2CPacketPayload;
import net.Gabou.identity2.packets.ProgressionPlayerChargesS2CPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressionUiSync {
    private ProgressionUiSync() {
    }

    public static void sendPlayerCharges(ServerPlayer player, Map<String, Integer> charges) {
        if (player == null) {
            return;
        }
        NetworkManager.sendToPlayer(
            player,
            new ProgressionPlayerChargesS2CPacketPayload(ProgressionChargeCodec.serialize(charges))
        );
    }

    public static void sendPlayerCharges(ServerPlayer player) {
        if (player == null) {
            return;
        }
        sendPlayerCharges(player, MorphChargeManager.getChargeSnapshot(player));
    }

    public static void sendJarState(
        ServerPlayer player,
        int slotIndex,
        String jarId,
        String jarTier,
        Map<String, Integer> jarCharges,
        String message
    ) {
        if (player == null) {
            return;
        }
        Map<String, Integer> playerCharges = MorphChargeManager.getChargeSnapshot(player);
        sendJarState(player, slotIndex, jarId, jarTier, jarCharges, playerCharges, message);
    }

    public static void sendJarState(
        ServerPlayer player,
        int slotIndex,
        String jarId,
        String jarTier,
        Map<String, Integer> jarCharges,
        Map<String, Integer> playerCharges,
        String message
    ) {
        if (player == null) {
            return;
        }
        NetworkManager.sendToPlayer(
            player,
            new ProgressionJarStateS2CPacketPayload(
                slotIndex,
                jarId == null ? "" : jarId,
                jarTier == null ? "" : jarTier,
                ProgressionChargeCodec.serialize(jarCharges),
                ProgressionChargeCodec.serialize(playerCharges),
                message == null ? "" : message
            )
        );
    }
}


