package net.Gabou.identity2.mixin;

import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BuiltInRegistries.class)
public abstract class RegistriesMixin {
    @Shadow
    private static WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY;
}
