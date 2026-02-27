package net.Gabou.identity2.forge.client;

import net.Gabou.identity2.client.Identity2ClientBootstrap;

public final class Identity2NeoForgeClient {
    private static boolean initialized = false;

    private Identity2NeoForgeClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        Identity2ClientBootstrap.initialize(new Identity2NeoForgeClientPlatform());
    }
}

