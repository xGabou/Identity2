package draylar.identity.forge.ability.impl;

import com.starfish_studios.naturalist.common.entity.Bear;
import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class BearAbility extends IdentityAbility<Bear> {
    @Override
    public void onUse(PlayerEntity player, Bear identity, World world) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
    }

    @Override
    public Item getIcon() {
        return Items.HONEYCOMB;
    }
}
