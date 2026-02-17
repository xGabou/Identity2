package net.Gabou.identity2.util;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

public interface LivingEntityAccessor {
    int getNextAirUnderwater(int air);

    int getNextAirOnLand(int air);

    @Nullable
    SoundEvent getHurtSound(DamageSource source);

    @Nullable
    SoundEvent getDeathSound();
}
