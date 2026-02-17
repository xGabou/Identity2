package net.Gabou.identity2;

import net.Gabou.identity2.identity.IdentityProgression;
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
        ModBlocks.initialize();
        ModEffects.initialize();
        ModComponents.initialize();
        ModRegistries.init();
        ModPackets.initialize();
        IdentityProgression.initialize();
        ModCommands.initialize();
    }
}
