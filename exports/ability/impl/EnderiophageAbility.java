
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EnderiophageAbility extends IdentityAbility<EntityEnderiophage> {
    @Override
    public void onUse(PlayerEntity player, EntityEnderiophage identity, World world) {
        AbilityUtils.shortTeleportForward(player, 5.0D);
    }

    @Override
    public Item getIcon() {
        return Items.ENDER_PEARL;
    }
}
