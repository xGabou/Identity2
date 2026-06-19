package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hoglin.class)
public interface HoglinAccessor {
    @Accessor("attackAnimationRemainingTicks")
    int identity2$getAttackAnimationRemainingTicks();

    @Accessor("attackAnimationRemainingTicks")
    void identity2$setAttackAnimationRemainingTicks(int attackAnimationRemainingTicks);
}
