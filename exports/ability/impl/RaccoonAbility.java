
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class RaccoonAbility extends IdentityAbility<EntityRaccoon> {
    @Override
    public void onUse(PlayerEntity player, EntityRaccoon identity, World world) {
        AbilityUtils.dropRandomItemFromInventory(player);
    }

    @Override
    public Item getIcon() {
        return Items.CHEST;
    }
}
