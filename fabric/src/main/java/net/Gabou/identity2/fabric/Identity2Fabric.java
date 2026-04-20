package net.Gabou.identity2.fabric;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.ModNetworking;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.fabric.auth.Identity2FabricNetworkingPlatform;
import net.Gabou.identity2.fabric.platform.Identity2FabricRegistryPlatform;
import net.fabricmc.api.ModInitializer;

public final class Identity2Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        ModRegistries.setPlatform(new Identity2FabricRegistryPlatform());
        ModNetworking.setPlatform(new Identity2FabricNetworkingPlatform());
        Identity2.init();
    }
}
