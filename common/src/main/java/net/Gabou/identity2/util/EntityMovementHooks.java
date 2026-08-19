package net.Gabou.identity2.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class EntityMovementHooks {
    private static final String HORIZONTAL_COLLISION_MULTIPLIER = "horizontal_collision_speed_multiplier_override";

    private EntityMovementHooks() {
    }

    public static void setDeltaMovementAfterCollisions(Entity entity, Vec3 movement) {
        EntityAccessor accessor = (EntityAccessor) entity;
        if (accessor.getCurrentIdentity() == null) {
            entity.setDeltaMovement(movement);
            return;
        }

        var multiplier = ((NbtComponentAccessor) (Object) accessor.getCustomData())
                .getNbt()
                .getDouble(HORIZONTAL_COLLISION_MULTIPLIER);

        if (multiplier.isEmpty() || multiplier.get() == 0.0D) {
            entity.setDeltaMovement(movement);
            return;
        }

        Vec3 originalMovement = entity.getDeltaMovement();
        double value = multiplier.get();
        entity.setDeltaMovement(
                movement.x != originalMovement.x ? originalMovement.x * value : movement.x,
                movement.y,
                movement.z != originalMovement.z ? originalMovement.z * value : movement.z
        );
    }
}
