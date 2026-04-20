package net.Gabou.identity2.platform;

import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.server.level.ServerPlayer;

public interface ModNetworkingPlatform {
    ModNetworkingPlatform NOOP = new ModNetworkingPlatform() {
        @Override
        public void registerCommonPackets() {
        }

        @Override
        public void registerClientPackets() {
        }

        @Override
        public void sendToPlayer(ServerPlayer player, NetworkPayload payload) {
        }

        @Override
        public void sendToServer(NetworkPayload payload) {
        }
    };

    void registerCommonPackets();

    void registerClientPackets();

    void sendToPlayer(ServerPlayer player, NetworkPayload payload);

    void sendToServer(NetworkPayload payload);
}
