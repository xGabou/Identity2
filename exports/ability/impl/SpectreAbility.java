package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntitySpectre;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class SpectreAbility extends IdentityAbility<EntitySpectre> {
    @Override
    public void onUse(PlayerEntity player, EntitySpectre identity, World world) {
        AbilityUtils.dashForward(player, 2.0D);
    }

    @Override
    public Item getIcon() {
        return Items.ENDER_PEARL;
    }
}
