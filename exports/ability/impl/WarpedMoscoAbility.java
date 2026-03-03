package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityWarpedMosco;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class WarpedMoscoAbility extends IdentityAbility<EntityWarpedMosco> {
    @Override
    public void onUse(PlayerEntity player, EntityWarpedMosco identity, World world) {
        AbilityUtils.dashForward(player, 2.5D);
        AbilityUtils.knockbackNearbyEntities(player, 3.0F, 2.5D);
    }

    @Override
    public Item getIcon() {
        return Items.NETHERITE_SCRAP;
    }
}
