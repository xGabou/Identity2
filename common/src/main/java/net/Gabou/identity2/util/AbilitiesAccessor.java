package net.Gabou.identity2.util;

import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Abilities.class)
public interface AbilitiesAccessor {
    @Accessor("flyingSpeed")
    float identity2$getFlyingSpeed();

    @Accessor("flyingSpeed")
    void identity2$setFlyingSpeed(float flyingSpeed);
}
