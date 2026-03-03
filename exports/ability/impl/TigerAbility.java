
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityTiger;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class TigerAbility extends IdentityAbility<EntityTiger> {
    @Override
    public void onUse(PlayerEntity player, EntityTiger identity, World world) {
        AbilityUtils.dashForward(player, 1.5D);
    }

    @Override
    public Item getIcon() {
        return Items.BEEF;
    }
}
