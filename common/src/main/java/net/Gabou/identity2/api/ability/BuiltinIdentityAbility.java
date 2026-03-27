package net.Gabou.identity2.api.ability;

import net.minecraft.world.entity.Entity;

public interface BuiltinIdentityAbility {
    default void execute(Entity player) {
    }

    default void executeSecondary(Entity player) {
    }

    default void tick(Entity player, int cooldown) {
    }

    default void passiveTick(Entity player, boolean used) {
    }

    default boolean overrideAttack(Entity player) {
        return false;
    }
}
