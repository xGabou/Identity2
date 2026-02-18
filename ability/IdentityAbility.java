package draylar.identity.ability;

import draylar.identity.Identity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public abstract class IdentityAbility<E> {

    /**
     * Defines the use action of this ability. Implementers can assume the ability checks, such as cool-downs, have successfully passed.
     */
    public abstract void onUse(PlayerEntity player, E identity, World world);

    /**
     * @return cooldown of this ability, in ticks, after it is used.
     */
    public int getCooldown(E entity) {
        return Identity.getCooldown(null); // workaround: or refactor later
    }

    public abstract Item getIcon();
}
