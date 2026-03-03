
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntitySkunk;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class SkunkAbility extends IdentityAbility<EntitySkunk> {
    @Override
    public void onUse(PlayerEntity player, EntitySkunk identity, World world) {
        // Skunk: Placeholder to simulate "poison cloud"
        AbilityUtils.healNearbyPlayers(player, 2.5f, -2.0f);
    }

    @Override
    public Item getIcon() {
        return Items.ROTTEN_FLESH;
    }
}
