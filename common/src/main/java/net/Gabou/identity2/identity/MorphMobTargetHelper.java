package net.Gabou.identity2.identity;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class MorphMobTargetHelper {
    private static final double CLEAR_TARGET_RADIUS = 48.0D;

    private MorphMobTargetHelper() {
    }

    public static void clearStaleTargets(@Nullable ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB search = player.getBoundingBox().inflate(CLEAR_TARGET_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, search, mob -> mob.getTarget() != null)) {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                continue;
            }
            if (target == player || ((EntityAccessor) target).getIdentityOwner() == player) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                mob.getNavigation().stop();
            }
        }
    }
}
