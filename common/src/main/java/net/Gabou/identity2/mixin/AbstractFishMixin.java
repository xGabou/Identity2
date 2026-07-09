package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin {

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/AbstractFish;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            ),
            require = 0
    )
    private void identity2$throttleMorphFlopSound(AbstractFish fish, SoundEvent sound, float volume, float pitch) {
        Entity owner = ((EntityAccessor) fish).getIdentityOwner();
        if (owner != null) {
            if (fish.tickCount % 20 != 0) {
                return;
            }
            // Judge movement by the owning player, not the morph fish: the fish's own
            // aiStep injects a random "flop" hop into its delta every land tick, so the
            // fish never looks idle and the sound would loop forever while standing still.
            if (owner.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D) {
                return;
            }
        }

        fish.playSound(sound, volume, pitch);
    }
}
