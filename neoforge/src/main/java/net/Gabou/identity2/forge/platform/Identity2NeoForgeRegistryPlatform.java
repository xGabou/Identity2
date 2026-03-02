package net.Gabou.identity2.forge.platform;

import com.mojang.serialization.Codec;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class Identity2NeoForgeRegistryPlatform implements ModRegistryPlatform {
    private static boolean shouldRegisterIdentityAbilityRegistry = false;
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
        shouldRegisterIdentityAbilityRegistry = true;
    }

    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        if (!shouldRegisterIdentityAbilityRegistry) {
            return;
        }
        register(event, ModRegistries.IDENTITY_ABILITY_KEY, ModRegistries.IDENTITY_ABILITY_CODEC);
        ModRegistries.refreshIdentityAbilityRegistry();
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(DataPackRegistryEvent.NewRegistry event, ResourceKey<? extends Registry<?>> key, Codec<?> codec) {
        event.dataPackRegistry((ResourceKey<Registry<T>>) key, (Codec<T>) codec, (Codec<T>) codec);
    }
}
