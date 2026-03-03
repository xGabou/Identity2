
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntitySnowLeopard;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class SnowLeopardAbility extends IdentityAbility<EntitySnowLeopard> {
    @Override
    public void onUse(PlayerEntity player, EntitySnowLeopard identity, World world) {
        if (player.getWorld().getBlockState(player.getBlockPos().down()).isOf(Blocks.SNOW_BLOCK)) {
            AbilityUtils.dashForward(player, 1.3D);
        }
    }

    @Override
    public Item getIcon() {
        return Items.SNOWBALL;
    }
}
