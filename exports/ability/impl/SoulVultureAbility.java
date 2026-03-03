
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntitySoulVulture;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class SoulVultureAbility extends IdentityAbility<EntitySoulVulture> {
    @Override
    public void onUse(PlayerEntity player, EntitySoulVulture identity, World world) {
        AbilityUtils.healNearbyPlayers(player, 4.0F, 4.0F);
    }

    @Override
    public Item getIcon() {
        return Items.GHAST_TEAR;
    }
}
