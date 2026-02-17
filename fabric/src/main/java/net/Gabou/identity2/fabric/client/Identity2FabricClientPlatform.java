package net.Gabou.identity2.fabric.client;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.client.platform.ModClientPlatform;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;

public final class Identity2FabricClientPlatform implements ModClientPlatform {
    @Override
    public void logClientRegistries() {
        for (RegistryLoader.Entry entry : DynamicRegistries.getDynamicRegistries()) {
            Identity2.LOGGER.info("Client Dynamic registry at: " + entry.key().getRegistry() + "/" + entry.key().getValue());
        }
        for (RegistryKey entry : net.minecraft.registry.Registries.REGISTRIES.getKeys()) {
            Identity2.LOGGER.info("Client registry at: " + entry.getRegistry() + "/" + entry.getValue());
        }
        for (RegistryKey entry : net.minecraft.registry.BuiltinRegistries.createWrapperLookup().streamAllRegistryKeys().toList()) {
            Identity2.LOGGER.info("??? registry at: " + entry.getRegistry() + "/" + entry.getValue());
        }
    }
}
