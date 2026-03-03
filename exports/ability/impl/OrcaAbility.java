
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityOrca;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class OrcaAbility extends IdentityAbility<EntityOrca> {
    @Override
    public void onUse(PlayerEntity player, EntityOrca identity, World world) {
        AbilityUtils.waterDash(player, 1.2D);
    }

    @Override
    public Item getIcon() {
        return Items.PRISMARINE_CRYSTALS;
    }
}
