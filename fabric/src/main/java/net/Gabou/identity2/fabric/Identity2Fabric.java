package net.Gabou.identity2.fabric;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.fabric.platform.Identity2FabricRegistryPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class Identity2Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        ModRegistries.setPlatform(new Identity2FabricRegistryPlatform());
        // ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> IdentityProgression.clearMorph(handler.player));
        Identity2.init();
    }
}
