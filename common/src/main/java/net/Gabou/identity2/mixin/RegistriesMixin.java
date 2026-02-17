package net.Gabou.identity2.mixin;

import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Registries.class)
public abstract class RegistriesMixin {
    @Shadow
    private static MutableRegistry<MutableRegistry<?>> ROOT;
}
