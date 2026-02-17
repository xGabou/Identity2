package net.Gabou.identity2.mixin;

import net.Gabou.identity2.Identity2;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleRegistry.class)
public class RegistryMixin<T> {
    @Inject(method = "add", at = @At("HEAD"))
    private void onadd(RegistryKey<T> key, T value, RegistryEntryInfo info, CallbackInfoReturnable<?> cir) {
        if (key.getRegistry().getPath().matches("root")) {
            Identity2.LOGGER.info("Registry registering: {}/{}", key.getRegistry(), key.getValue());
        }
    }
}
