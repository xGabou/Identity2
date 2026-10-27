package net.Gabou.identity2.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.SwingState.class)
public interface SwingStateAccessor {
    @Accessor("currentSwing")
    LivingEntity.SwingDescription identity2$getCurrentSwing();

    @Accessor("currentSwing")
    void identity2$setCurrentSwing(LivingEntity.SwingDescription swing);

    @Accessor("ticks")
    int identity2$getTicks();

    @Accessor("ticks")
    void identity2$setTicks(int ticks);

    @Accessor("oldAnimation")
    float identity2$getOldAnimation();

    @Accessor("oldAnimation")
    void identity2$setOldAnimation(float animation);

    @Accessor("animation")
    float identity2$getAnimation();

    @Accessor("animation")
    void identity2$setAnimation(float animation);

}
