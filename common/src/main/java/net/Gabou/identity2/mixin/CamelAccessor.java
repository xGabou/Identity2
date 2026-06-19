package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camel.class)
public interface CamelAccessor {
    @Accessor("dashCooldown")
    int identity2$getDashCooldown();

    @Accessor("dashCooldown")
    void identity2$setDashCooldown(int dashCooldown);
}
