package net.Gabou.identity2.forge.platform;

import com.mojang.serialization.Codec;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DataPackRegistryEvent;

public final class Identity2ForgeRegistryPlatform implements ModRegistryPlatform {
    private static ResourceKey<? extends Registry<?>> pendingRegistryKey;
    private static Codec<?> pendingCodec;
    private static boolean eventBound = false;

    public static void bind(IEventBus modEventBus) {
        if (eventBound) {
            return;
        }
        eventBound = true;
        modEventBus.addListener(Identity2ForgeRegistryPlatform::onNewDataPackRegistry);
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
        try {
            Class<?> registriesClass = Class.forName("net.Gabou.identity2.ModRegistries");
            registriesClass.getMethod("refreshIdentityAbilityRegistry").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(DataPackRegistryEvent.NewRegistry event, ResourceKey<? extends Registry<?>> key, Codec<?> codec) {
        event.dataPackRegistry((ResourceKey<Registry<T>>) key, (Codec<T>) codec, (Codec<T>) codec);
    }
}
