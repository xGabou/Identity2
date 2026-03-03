
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityDropBear;

import com.github.alexthe666.alexsmobs.entity.EntityDropBear;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class DropBearAbility extends IdentityAbility<EntityDropBear> {
    @Override
    public void onUse(PlayerEntity player, EntityDropBear identity, World world) {
        AbilityUtils.dashForward(player, 1.0D);
    }

    @Override
    public Item getIcon() {
        return Items.BONE;
    }
}
