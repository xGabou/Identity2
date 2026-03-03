
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityRoadrunner;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class RoadrunnerAbility extends IdentityAbility<EntityRoadrunner> {
    @Override
    public void onUse(PlayerEntity player, EntityRoadrunner identity, World world) {
        AbilityUtils.dashForward(player, 1.8D);
    }

    @Override
    public Item getIcon() {
        return Items.FEATHER;
    }
}
