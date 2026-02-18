package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EnderDragonAbility extends IdentityAbility<EnderDragonEntity> {

    @Override
    public void onUse(PlayerEntity player, EnderDragonEntity identity, World world) {
        if (!world.isClient) {
            Vec3d look = player.getRotationVec(1.0F);
            Vec3d velocity = look.multiply(0.5); // control speed
            Vec3d spawnPos = player.getEyePos().add(look.multiply(2.0)); // mouth-level offset

            DragonFireballEntity dragonFireball = new DragonFireballEntity(world, player, velocity);

            // Manually move fireball to spawn in front of the player’s head
            dragonFireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
            dragonFireball.setOwner(player);

            world.spawnEntity(dragonFireball);

            world.playSoundFromEntity(
                    null,
                    player,
                    SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                    SoundCategory.HOSTILE,
                    3.0F,
                    1.0F
            );
        }
    }



    @Override
    public Item getIcon() {
        return Items.DRAGON_BREATH;
    }
}
