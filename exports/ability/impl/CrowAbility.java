
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class CrowAbility extends IdentityAbility<EntityCrow> {
    @Override
    public void onUse(PlayerEntity player, EntityCrow identity, World world) {
        AbilityUtils.dashUpward(player, 0.5D);
    }

    @Override
    public Item getIcon() {
        return Items.FEATHER;
    }
}
