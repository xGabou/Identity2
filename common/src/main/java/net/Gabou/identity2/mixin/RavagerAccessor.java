package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.monster.Ravager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ravager.class)
public interface RavagerAccessor {
    @Accessor("attackTick")
    int identity2$getAttackTick();

    @Accessor("attackTick")
    void identity2$setAttackTick(int attackTick);
}
