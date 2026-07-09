package net.Gabou.identity2.neoforge.platform;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class Identity2NeoForgeRegistryPlatform implements ModRegistryPlatform {
    private static boolean registryRequested = false;
    private static boolean eventBound = false;

    public static void bind(IEventBus modEventBus) {
        if (eventBound) {
            return;
        }
        eventBound = true;
        modEventBus.addListener(Identity2NeoForgeRegistryPlatform::onNewDataPackRegistry);
    }

    @Override
    public synchronized void registerIdentityAbilityRegistry() {
        // Direct access to ModRegistries is safe here: the neoforge module compiles
        // against the common module, exactly like the Fabric platform does. The old
        // Class.forName reflection dated from a classloading scare that never applied
        // to static field reads and only hid genuine wiring errors.
        registryRequested = true;
    }

    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        if (!registryRequested) {
            return;
        }
        event.dataPackRegistry(ModRegistries.IDENTITY_ABILITY_KEY, ModRegistries.IDENTITY_ABILITY_CODEC, ModRegistries.IDENTITY_ABILITY_CODEC);
        Identity2.LOGGER.debug(
            "Declared NeoForge datapack registry {}. Runtime identity ability entries will resolve from active RegistryAccess once worlds load.",
            ModRegistries.IDENTITY_ABILITY_KEY.location()
        );
    }
}
