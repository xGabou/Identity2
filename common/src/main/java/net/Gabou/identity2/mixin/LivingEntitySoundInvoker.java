package net.Gabou.identity2.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for the protected LivingEntity sound getters.
 *
 * <p>These must be reached through a mixin {@code @Invoker} rather than a plain
 * interface added onto LivingEntity: the vanilla methods are protected, so a
 * public interface method with the same signature cannot link against them and
 * the first interface dispatch crashes (seen as the "damage while morphed with
 * useIdentitySounds" crash).</p>
 */
@Mixin(LivingEntity.class)
public interface LivingEntitySoundInvoker {
    @Nullable
    @Invoker("getHurtSound")
    SoundEvent identity2$invokeGetHurtSound(DamageSource source);

    @Nullable
    @Invoker("getDeathSound")
    SoundEvent identity2$invokeGetDeathSound();
}
