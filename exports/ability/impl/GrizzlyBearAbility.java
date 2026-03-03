
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class GrizzlyBearAbility extends IdentityAbility<EntityGrizzlyBear> {
    @Override
    public void onUse(PlayerEntity player, EntityGrizzlyBear identity, World world) {
        AbilityUtils.knockbackNearbyEntities(player, 3.0F, 1.0D);
    }

    @Override
    public Item getIcon() {
        return Items.BEEF;
    }
}
