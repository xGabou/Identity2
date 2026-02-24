package net.Gabou.identity2.neoforge;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.neoforge.client.Identity2NeoForgeClient;
import net.Gabou.identity2.neoforge.platform.Identity2NeoForgeRegistryPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Identity2.MOD_ID)
public final class Identity2NeoForge {
    public Identity2NeoForge(IEventBus modEventBus) {
        Identity2NeoForgeRegistryPlatform.bind(modEventBus);
        ModRegistries.setPlatform(new Identity2NeoForgeRegistryPlatform());
        Identity2.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            Identity2NeoForgeClient.initialize();
        }
    }
}
