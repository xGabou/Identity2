
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;

import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class CrimsonMosquitoAbility extends IdentityAbility<EntityCrimsonMosquito> {
    @Override
    public void onUse(PlayerEntity player, EntityCrimsonMosquito identity, World world) {
        EntityHitResult hit = AbilityUtils.raycastEntities(player, 2.5D);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.damage(player.getDamageSources().playerAttack(player), 2.0f);
            player.heal(1.0f);
        }
    }

    @Override
    public Item getIcon() {
        return Items.SPIDER_EYE;
    }
}
