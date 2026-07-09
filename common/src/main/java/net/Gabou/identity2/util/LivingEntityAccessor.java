package net.Gabou.identity2.util;

/**
 * Extra state exposed on every LivingEntity by LivingEntityMixin.
 *
 * <p>Hurt/death sound access intentionally lives in
 * {@code net.Gabou.identity2.mixin.LivingEntitySoundInvoker} instead: the vanilla
 * getters are protected, so declaring them here as public interface methods can
 * never link and crashes on first dispatch.</p>
 */
public interface LivingEntityAccessor {

    boolean identity2$isJumping();
}
