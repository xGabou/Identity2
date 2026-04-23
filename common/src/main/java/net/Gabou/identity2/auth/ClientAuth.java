package net.Gabou.identity2.auth;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;

public final class ClientAuth {
    private ClientAuth() {
    }

    public static void handleChallenge(S2CChallengePacket packet) {
        if (packet == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        NetworkManager.sendToServer(new C2SChallengeReplyPacket(
            packet.nonce(),
            SharedSecret.computeResponse(minecraft.player.getUUID(), packet.nonce()),
            ClientLauncherGuards.getDetectedReason()
        ));
    }
}
