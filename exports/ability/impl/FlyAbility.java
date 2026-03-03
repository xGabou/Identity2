
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityFly;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class FlyAbility extends IdentityAbility<EntityFly> {
    @Override
    public void onUse(PlayerEntity player, EntityFly identity, World world) {
        AbilityUtils.dashForward(player, 0.4D);
    }

    @Override
    public Item getIcon() {
        return Items.SUGAR;
    }
}
