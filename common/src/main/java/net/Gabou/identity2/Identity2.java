package net.Gabou.identity2;

import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.config.IdentityConfigManager;
import net.Gabou.identity2.ModNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Identity2 {
    public static final String MOD_ID = "identity2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static int indexOverrideActive = 0;
    public static int maxWorldSize = 30000000;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        IdentityConfigManager.initialize();
        // NeoForge 1.21.11 freezes built-in registries before this point.
        // ModBlocks currently performs direct static registration into built-ins,
        // so defer this bootstrap until it is ported to platform-safe registration.
        // ModBlocks.initialize();
        ModItems.initialize();
        ModCreativeTabs.initialize();
        ModEffects.initialize();
        ModComponents.initialize();
        ModRegistries.init();
        ModNetworking.init();
        ModPackets.initialize();
        IdentityProgression.initialize();
        ModCommands.initialize();
    }
}
