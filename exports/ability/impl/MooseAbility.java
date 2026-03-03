
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityMoose;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class MooseAbility extends IdentityAbility<EntityMoose> {
    @Override
    public void onUse(PlayerEntity player, EntityMoose identity, World world) {
        AbilityUtils.dashForward(player, 1.4D);
        AbilityUtils.knockbackNearbyEntities(player, 2.5f, 1.2D);
    }

    @Override
    public Item getIcon() {
        return Items.WHEAT;
    }
}
