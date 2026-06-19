package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IronGolem.class)
public interface IronGolemAccessor {
    @Accessor("attackAnimationTick")
    int identity2$getAttackAnimationTick();

    @Accessor("attackAnimationTick")
    void identity2$setAttackAnimationTick(int attackAnimationTick);
}
