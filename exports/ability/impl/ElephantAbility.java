
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class ElephantAbility extends IdentityAbility<EntityElephant> {
    @Override
    public void onUse(PlayerEntity player, EntityElephant identity, World world) {
        AbilityUtils.knockbackNearbyEntities(player, 4.0f, 2.0D);
    }

    @Override
    public Item getIcon() {
        return Items.HAY_BLOCK;
    }
}
