
package draylar.identity.forge.ability.impl;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;

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

public class TarantulaHawkAbility extends IdentityAbility<EntityTarantulaHawk> {
    @Override
    public void onUse(PlayerEntity player, EntityTarantulaHawk identity, World world) {
        EntityHitResult hit = AbilityUtils.raycastEntities(player, 3.0D);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0));
        }
    }

    @Override
    public Item getIcon() {
        return Items.SPIDER_EYE;
    }
}
