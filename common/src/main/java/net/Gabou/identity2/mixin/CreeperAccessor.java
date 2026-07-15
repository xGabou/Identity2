package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("oldSwell")
    int identity2$getOldSwell();

    @Accessor("oldSwell")
    void identity2$setOldSwell(int value);

    @Accessor("swell")
    int identity2$getSwell();

    @Accessor("swell")
    void identity2$setSwell(int value);

    @Accessor("maxSwell")
    int identity2$getMaxSwell();
}