package net.Gabou.identity2.forge.auth;

import java.util.function.Function;
import java.util.function.Supplier;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.auth.C2SChallengeReplyPacket;
import net.Gabou.identity2.auth.ClientAuth;
import net.Gabou.identity2.auth.S2CChallengePacket;
import net.Gabou.identity2.auth.ServerAuth;
import net.Gabou.identity2.platform.ModNetworkingPlatform;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Identity2ForgeNetworkingPlatform implements ModNetworkingPlatform {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(Identity2.MOD_ID, "auth"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private static boolean registered = false;

    @Override
    public void registerCommonPackets() {
        if (registered) {
            return;
        }
        registered = true;

        registerMessage(
            0,
            S2CChallengePacket.class,
            NetworkDirection.PLAY_TO_CLIENT,
            S2CChallengePacket::decode,
            (packet, contextSupplier) -> {
                NetworkEvent.Context context = contextSupplier.get();
                context.enqueueWork(() -> ClientAuth.handleChallenge(packet));
                context.setPacketHandled(true);
            }
        );

        registerMessage(
            1,
            C2SChallengeReplyPacket.class,
            NetworkDirection.PLAY_TO_SERVER,
            C2SChallengeReplyPacket::decode,
            (packet, contextSupplier) -> {
                NetworkEvent.Context context = contextSupplier.get();
                context.enqueueWork(() -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        ServerAuth.handleChallengeReply(sender, packet);
                    }
                });
                context.setPacketHandled(true);
            }
        );
    }

    @Override
    public void registerClientPackets() {
    }

    @Override
    public void sendToPlayer(ServerPlayer player, NetworkPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    @Override
    public void sendToServer(NetworkPayload payload) {
        if (payload == null) {
            return;
        }
        CHANNEL.sendToServer(payload);
    }

    private static <T extends NetworkPayload> void registerMessage(
        int id,
        Class<T> type,
        NetworkDirection direction,
        Function<FriendlyByteBuf, T> decoder,
        java.util.function.BiConsumer<T, Supplier<NetworkEvent.Context>> handler
    ) {
        CHANNEL.messageBuilder(type, id, direction)
            .encoder((payload, buffer) -> payload.write(buffer))
            .decoder(decoder)
            .consumerMainThread(handler)
            .add();
    }
}
