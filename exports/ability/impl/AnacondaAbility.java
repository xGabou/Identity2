
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;

import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class AnacondaAbility extends IdentityAbility<EntityAnaconda> {
    @Override
    public void onUse(PlayerEntity player, EntityAnaconda identity, World world) {
        AbilityUtils.constrictNearby(player, 3.0f);
    }

    @Override
    public Item getIcon() {
        return Items.VINE;
    }
}
