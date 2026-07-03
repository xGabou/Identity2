package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Wolf.class)
public interface WolfAccessor {

    @Accessor("isWet")
    void identity2$setWet(boolean wet);

    @Accessor("isShaking")
    void identity2$setShaking(boolean shaking);

    @Accessor("shakeAnim")
    void identity2$setShakeAnim(float value);

    @Accessor("shakeAnimO")
    void identity2$setShakeAnimO(float value);

    @Invoker("setCollarColor")
    void identity2$setCollarColor(net.minecraft.world.item.DyeColor color);
}
