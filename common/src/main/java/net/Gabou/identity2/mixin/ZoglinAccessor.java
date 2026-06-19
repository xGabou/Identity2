package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.monster.Zoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Zoglin.class)
public interface ZoglinAccessor {
    @Accessor("attackAnimationRemainingTicks")
    int identity2$getAttackAnimationRemainingTicks();

    @Accessor("attackAnimationRemainingTicks")
    void identity2$setAttackAnimationRemainingTicks(int attackAnimationRemainingTicks);
}
