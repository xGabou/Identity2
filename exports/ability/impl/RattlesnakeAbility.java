
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityRattlesnake;

import draylar.identity.ability.IdentityAbility;
import net.Gabou.gaboulibs.util.AbilityUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class RattlesnakeAbility extends IdentityAbility<EntityRattlesnake> {
    @Override
    public void onUse(PlayerEntity player, EntityRattlesnake identity, World world) {
        EntityHitResult hit = AbilityUtils.raycastEntities(player, 2.0D);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
        }
    }

    @Override
    public Item getIcon() {
        return Items.BONE;
    }
}
