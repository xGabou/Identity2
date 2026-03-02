package net.Gabou.identity2.util;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkCompat {
    private NetworkCompat() {
    }

    public static <T extends NetworkPayload> void sendToPlayer(ServerPlayer player, T payload) {
        if (player == null || payload == null) {
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToPlayer(player, payload.id(), buffer);
    }

    public static <T extends NetworkPayload> void sendToServer(T payload) {
        if (payload == null) {
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToServer(payload.id(), buffer);
    }

    public static <T> void registerReceiver(
        NetworkManager.Side side,
        net.minecraft.resources.ResourceLocation id,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, NetworkManager.PacketContext> handler
    ) {
        NetworkManager.registerReceiver(side, id, (buffer, context) -> {
            T payload = decoder.apply(buffer);
            handler.accept(payload, context);
        });
    }
}


