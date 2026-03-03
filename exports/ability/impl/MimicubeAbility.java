
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class MimicubeAbility extends IdentityAbility<EntityMimicube> {
    @Override
    public void onUse(PlayerEntity player, EntityMimicube identity, World world) {
        AbilityUtils.randomMorphNearby(player);
    }

    @Override
    public Item getIcon() {
        return Items.SLIME_BALL;
    }
}
