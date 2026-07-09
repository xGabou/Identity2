package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.animal.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Rabbit.class)
public interface RabbitAccessor {
    @Accessor("jumpDuration")
    int identity2$getJumpDuration();

    @Accessor("jumpTicks")
    int identity2$getJumpTicks();
}
