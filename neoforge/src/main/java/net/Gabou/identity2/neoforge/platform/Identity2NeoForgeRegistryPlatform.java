package net.Gabou.identity2.neoforge.platform;

import com.mojang.serialization.Codec;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class Identity2NeoForgeRegistryPlatform implements ModRegistryPlatform {
    private static ResourceKey<? extends Registry<?>> pendingRegistryKey;
    private static Codec<?> pendingCodec;
    private static boolean eventBound = false;

    public static void bind(IEventBus modEventBus) {
        if (eventBound) {
            return;
        }
        eventBound = true;
        modEventBus.addListener(Identity2NeoForgeRegistryPlatform::onNewDataPackRegistry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void registerIdentityAbilityRegistry() {
        try {
            Class<?> registriesClass = Class.forName("net.Gabou.identity2.ModRegistries");
            pendingRegistryKey = (ResourceKey<? extends Registry<?>>) registriesClass.getField("IDENTITY_ABILITY_KEY").get(null);
            pendingCodec = (Codec<?>) registriesClass.getField("IDENTITY_ABILITY_CODEC").get(null);
        } catch (ReflectiveOperationException ignored) {
            pendingRegistryKey = null;
            pendingCodec = null;
        }
    }

    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        if (pendingRegistryKey == null || pendingCodec == null) {
            return;
        }
        register(event, pendingRegistryKey, pendingCodec);
        Identity2.LOGGER.debug(
            "Declared NeoForge datapack registry {}. Runtime identity ability entries will resolve from active RegistryAccess once worlds load.",
            pendingRegistryKey.location()
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(DataPackRegistryEvent.NewRegistry event, ResourceKey<? extends Registry<?>> key, Codec<?> codec) {
        event.dataPackRegistry((ResourceKey<Registry<T>>) key, (Codec<T>) codec, (Codec<T>) codec);
    }
}
