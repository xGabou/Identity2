
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityVoidWorm;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class VoidWormAbility extends IdentityAbility<EntityVoidWorm> {
    @Override
    public void onUse(PlayerEntity player, EntityVoidWorm identity, World world) {
        AbilityUtils.dashForward(player, 2.5D);
    }

    @Override
    public Item getIcon() {
        return Items.ENDER_PEARL;
    }
}
