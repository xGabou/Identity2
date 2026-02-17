package net.Gabou.identity2.fabric.client;

import net.Gabou.identity2.client.Identity2ClientBootstrap;
import net.fabricmc.api.ClientModInitializer;

public final class Identity2FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Identity2ClientBootstrap.initialize(new Identity2FabricClientPlatform());
    }
}

