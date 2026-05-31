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
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            )
    )
    private void identity2$throttleMorphFlopSound(AbstractFish fish, SoundEvent sound, float volume, float pitch) {
        if (((EntityAccessor) fish).getIdentityOwner() != null && ((Entity) fish).tickCount % 20 != 0) {
            return;
        }
        fish.playSound(sound, volume, pitch);
    }
}
