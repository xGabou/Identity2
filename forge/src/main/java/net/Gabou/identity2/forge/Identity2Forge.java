package net.Gabou.identity2.forge;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.forge.client.Identity2ForgeClient;
import net.Gabou.identity2.forge.platform.Identity2ForgeRegistryPlatform;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Identity2.MOD_ID)
public final class Identity2Forge {
    public Identity2Forge(IEventBus modEventBus) {
        Identity2ForgeRegistryPlatform.bind(modEventBus);
        ModRegistries.setPlatform(new Identity2ForgeRegistryPlatform());
        Identity2.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            Identity2ForgeClient.initialize();
        }
    }
}
