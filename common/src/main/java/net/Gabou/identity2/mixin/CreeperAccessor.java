package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Invoker("setSwellDir")
    void identity2$setSwellDir(int state);

    @Accessor("swell")
    void identity2$setSwell(int swell);

    @Accessor("oldSwell")
    void identity2$setOldSwell(int swell);
}
