
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntitySunbird;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class SunbirdAbility extends IdentityAbility<EntitySunbird> {
    @Override
    public void onUse(PlayerEntity player, EntitySunbird identity, World world) {
        AbilityUtils.dashUpward(player, 2.0D);
    }

    @Override
    public Item getIcon() {
        return Items.GOLDEN_APPLE;
    }
}
