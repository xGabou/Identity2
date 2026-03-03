
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class BoneSerpentAbility extends IdentityAbility<EntityBoneSerpent> {
    @Override
    public void onUse(PlayerEntity player, EntityBoneSerpent identity, World world) {
        if (player.isInLava()) {
            AbilityUtils.dashForward(player, 1.8D);
        }
    }

    @Override
    public Item getIcon() {
        return Items.MAGMA_CREAM;
    }
}
