
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityGiantSquid;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class GiantSquidAbility extends IdentityAbility<EntityGiantSquid> {
    @Override
    public void onUse(PlayerEntity player, EntityGiantSquid identity, World world) {
        AbilityUtils.waterDash(player, 1.5D);
    }

    @Override
    public Item getIcon() {
        return Items.INK_SAC;
    }
}
