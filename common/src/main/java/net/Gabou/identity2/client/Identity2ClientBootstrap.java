package net.Gabou.identity2.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.gaboulibs.client.platform.ModClientPlatform;

public final class Identity2ClientBootstrap {
    private Identity2ClientBootstrap() {
    }

    public static void initialize(ModClientPlatform platform) {
        Identity2Client.initialize(platform);
    }
}

