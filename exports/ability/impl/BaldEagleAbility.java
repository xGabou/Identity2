
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;

import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class BaldEagleAbility extends IdentityAbility<EntityBaldEagle> {
    @Override
    public void onUse(PlayerEntity player, EntityBaldEagle identity, World world) {
        AbilityUtils.dashForward(player, 1.2D);
    }

    @Override
    public Item getIcon() {
        return Items.FEATHER;
    }
}
