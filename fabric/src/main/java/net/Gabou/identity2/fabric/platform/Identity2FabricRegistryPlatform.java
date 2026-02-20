package net.Gabou.identity2.fabric.platform;

import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Registry;

public final class Identity2FabricRegistryPlatform implements ModRegistryPlatform {
    private static boolean callbackRegistered = false;

    @Override
    @SuppressWarnings("unchecked")
    public void registerIdentityAbilityRegistry() {
        DynamicRegistries.registerSynced(ModRegistries.IDENTITY_ABILITY_KEY, ModRegistries.IDENTITY_ABILITY_CODEC, ModRegistries.IDENTITY_ABILITY_CODEC);

        if (callbackRegistered) {
            return;
        }
        callbackRegistered = true;

        DynamicRegistrySetupCallback.EVENT.register(registryView -> {
            for (Registry<?> entry : registryView.stream().toList()) {
                if (entry.key().equals(ModRegistries.IDENTITY_ABILITY_KEY)) {
                    ModRegistries.identityAbilityRegistry = (Registry<IdentityAbilityDefinition>) entry;
                    return;
                }
            }
            ModRegistries.refreshIdentityAbilityRegistry();
        });
    }
}
