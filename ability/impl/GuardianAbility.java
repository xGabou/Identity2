package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.World;

import java.util.List;

public class GuardianAbility extends IdentityAbility<GuardianEntity> {

    @Override
    public void onUse(PlayerEntity player, GuardianEntity identity, World world) {
        if (!world.isClient) {
            List<PlayerEntity> targets = world.getNonSpectatingEntities(
                    PlayerEntity.class,
                    player.getBoundingBox().expand(50.0D)
            );

            for (PlayerEntity target : targets) {
                if (target != player) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 60, 2));
                }
            }
        }
    }

    @Override
    public Item getIcon() {
        return Items.PRISMARINE_SHARD;
    }

    @Override
    public int getCooldown(GuardianEntity entity) {
        return 20 * 30;
    }
}

