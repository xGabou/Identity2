
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityCrocodile;

import com.github.alexthe666.alexsmobs.entity.EntityCrocodile;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class CrocodileAbility extends IdentityAbility<EntityCrocodile> {
    @Override
    public void onUse(PlayerEntity player, EntityCrocodile identity, World world) {
        EntityHitResult hit = AbilityUtils.raycastEntities(player, 5.0D);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            AbilityUtils.pullEntityTowardPlayer(player, target, 1.5D);
        }
    }

    @Override
    public Item getIcon() {
        return Items.LEAD;
    }
}
