
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class KangarooAbility extends IdentityAbility<EntityKangaroo> {
    @Override
    public void onUse(PlayerEntity player, EntityKangaroo identity, World world) {
        AbilityUtils.dashForward(player, 1.2D);
        AbilityUtils.dashUpward(player, 0.5D);
    }

    @Override
    public Item getIcon() {
        return Items.RABBIT_FOOT;
    }
}
