package net.Gabou.identity2.identity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

public final class KeepInventoryHelper {
    private KeepInventoryHelper() {
    }

    public static boolean isKeepInventoryEnabled(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return serverPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }
}
