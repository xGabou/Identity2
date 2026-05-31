package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Bee.class)
public interface BeeAccessor {

    @Invoker("setRolling")
    void identity2$setRolling(boolean value);

    @Accessor("rollAmount")
    void identity2$setRollAmount(float value);

    @Accessor("rollAmountO")
    void identity2$setRollAmountO(float value);
}
