package net.Gabou.identity2.mixin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin<T> {
    @Inject(method = "add", at = @At("HEAD"))
    private void oninit(RegistryKey<T> key, T value, RegistryEntryInfo info, CallbackInfoReturnable<?> cir) {
    }
}
