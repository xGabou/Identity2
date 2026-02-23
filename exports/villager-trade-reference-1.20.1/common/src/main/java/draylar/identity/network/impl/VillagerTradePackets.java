package draylar.identity.network.impl;

import dev.architectury.networking.NetworkManager;
import draylar.identity.api.PlayerIdentity;
import draylar.identity.network.NetworkHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import draylar.identity.network.impl.Payload.*;

import java.util.UUID;

public class VillagerTradePackets {

    public static void sendTradeRequest(UUID target) {
        NetworkManager.sendToServer(new TradeRequestPayload(target));
    }


    public static void registerTradeRequestHandler() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                TradeRequestPayload.ID,
                TradeRequestPayload.CODEC,
                (payload, context) -> {
                    UUID targetId = payload.target(); // use record accessor
                    ServerPlayerEntity requester = (ServerPlayerEntity) context.getPlayer();
                    context.queue(() -> {
                        ServerPlayerEntity target = requester.getServer().getPlayerManager().getPlayer(targetId);
                        if (target != null) {
                            LivingEntity identity = PlayerIdentity.getIdentity(target);
                            if (identity instanceof VillagerEntity villager) {
                                if (requester.getUuid().equals(target.getUuid()) &&
                                        !draylar.identity.api.platform.IdentityConfig.getInstance().allowSelfTrading()) {
                                    return;
                                }
                                villager.interactMob(requester, net.minecraft.util.Hand.MAIN_HAND);
                            }
                        }
                    });
                }
        );
    }



}