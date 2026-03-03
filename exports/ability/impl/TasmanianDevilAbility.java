
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityTasmanianDevil;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class TasmanianDevilAbility extends IdentityAbility<EntityTasmanianDevil> {
    @Override
    public void onUse(PlayerEntity player, EntityTasmanianDevil identity, World world) {
        AbilityUtils.dashForward(player, 1.8D);
    }

    @Override
    public Item getIcon() {
        return Items.BEEF;
    }
}
