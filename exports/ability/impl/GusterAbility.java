
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityGuster;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class GusterAbility extends IdentityAbility<EntityGuster> {
    @Override
    public void onUse(PlayerEntity player, EntityGuster identity, World world) {
        AbilityUtils.knockbackNearbyEntities(player, 5.0F, 2.0D);
    }

    @Override
    public Item getIcon() {
        return Items.PHANTOM_MEMBRANE;
    }
}
