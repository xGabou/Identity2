
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class KomodoDragonAbility extends IdentityAbility<EntityKomodoDragon> {
    @Override
    public void onUse(PlayerEntity player, EntityKomodoDragon identity, World world) {
        AbilityUtils.poisonNearbyEnemies(player, 3.0f, 100, 0);
    }

    @Override
    public Item getIcon() {
        return Items.FERMENTED_SPIDER_EYE;
    }
}
