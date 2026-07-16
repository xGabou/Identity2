package net.Gabou.identity2.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Direct access to LivingEntity's protected sound getters. */
@Mixin(LivingEntity.class)
public interface LivingEntitySoundInvoker {
    @Nullable
    @Invoker("getHurtSound")
    SoundEvent identity2$invokeGetHurtSound(DamageSource source);

    @Nullable
    @Invoker("getDeathSound")
    SoundEvent identity2$invokeGetDeathSound();
}
