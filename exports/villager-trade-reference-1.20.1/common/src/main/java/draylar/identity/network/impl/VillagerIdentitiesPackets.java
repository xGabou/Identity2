package draylar.identity.network.impl;

import dev.architectury.networking.NetworkManager;
import draylar.identity.impl.PlayerDataProvider;
import draylar.identity.network.ClientNetworking;
import draylar.identity.network.impl.Payload.VillagerIdentitiesSyncPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public class VillagerIdentitiesPackets {

    public static void sendSync(ServerPlayerEntity player) {
        PlayerDataProvider data = (PlayerDataProvider) player;

        NbtCompound root = new NbtCompound();
        NbtCompound villagerTag = new NbtCompound();
        for (Map.Entry<String, NbtCompound> entry : data.getVillagerIdentities().entrySet()) {
            villagerTag.put(entry.getKey(), entry.getValue().copy());
        }
        root.put("VillagerIdentities", villagerTag);
        String active = data.getActiveVillagerKey();
        if (active != null && !active.isEmpty()) {
            root.putString("ActiveVillagerKey", active);
        }

        NetworkManager.sendToPlayer(player, new VillagerIdentitiesSyncPayload(root));
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientHandler() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                VillagerIdentitiesSyncPayload.ID,
                VillagerIdentitiesSyncPayload.CODEC,
                (payload, context) -> {
                    NbtCompound root = payload.data();

                    ClientNetworking.runOrQueue(context, player -> {
                        if (root == null) {
                            return;
                        }
                        PlayerDataProvider data = (PlayerDataProvider) player;
                        data.getVillagerIdentities().clear();
                        NbtCompound villagerTag = root.getCompound("VillagerIdentities");
                        for (String key : villagerTag.getKeys()) {
                            data.getVillagerIdentities().put(key, villagerTag.getCompound(key));
                        }
                        String active = root.contains("ActiveVillagerKey", NbtElement.STRING_TYPE)
                                ? root.getString("ActiveVillagerKey") : null;
                        data.setActiveVillagerKey(active == null || active.isEmpty() ? null : active);

                        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                        if (mc.currentScreen instanceof draylar.identity.screen.IdentityScreen screen) {
                            screen.resize(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
                        }
                    });
                }
        );
    }
}
