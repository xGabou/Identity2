package net.Gabou.identity2.forge.client;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.client.platform.ModClientPlatform;

public final class Identity2ForgeClientPlatform implements ModClientPlatform {
    @Override
    public void logClientRegistries() {
        Identity2.LOGGER.info("NeoForge client platform initialized.");
    }
}
