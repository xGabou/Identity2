
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class HummingbirdAbility extends IdentityAbility<EntityHummingbird> {
    @Override
    public void onUse(PlayerEntity player, EntityHummingbird identity, World world) {
        AbilityUtils.dashUpward(player, 0.7D);
    }

    @Override
    public Item getIcon() {
        return Items.FEATHER;
    }
}
