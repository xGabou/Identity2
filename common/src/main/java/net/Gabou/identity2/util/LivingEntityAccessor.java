package net.Gabou.identity2.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

public interface LivingEntityAccessor {

    @Nullable
    SoundEvent getHurtSound(DamageSource source);

    @Nullable
    SoundEvent getDeathSound();

    boolean identity2$isJumping();
}
