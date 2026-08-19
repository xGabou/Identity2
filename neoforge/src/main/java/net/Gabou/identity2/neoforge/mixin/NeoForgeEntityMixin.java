package net.Gabou.identity2.neoforge.mixin;

import net.Gabou.identity2.util.EntityMovementHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public class NeoForgeEntityMixin {
    @Redirect(
            method = "restituteMovementAfterCollisions(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void identity2$applyHorizontalCollisionMultiplier(Entity entity, Vec3 movement) {
        EntityMovementHooks.setDeltaMovementAfterCollisions(entity, movement);
    }
}
