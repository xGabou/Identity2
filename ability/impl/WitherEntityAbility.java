package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WitherEntityAbility extends IdentityAbility<WitherEntity> {

    @Override
    public void onUse(PlayerEntity player, WitherEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        // Play shoot sound
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.HOSTILE,
                1.0F,
                0.8F + world.random.nextFloat() * 0.4F
        );

        // Direction of fire
        Vec3d look = player.getRotationVec(1.0F);

        // Spawn position: in front of the player's eyes (~2 blocks forward)
        Vec3d spawnPos = player.getEyePos().add(look.multiply(2.0));

        // Create skull entity
        WitherSkullEntity skull = new WitherSkullEntity(world, player, look);
        skull.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());

        // Adjust velocity and accuracy
        skull.setVelocity(look.x, look.y, look.z, 1.5F, 0.0F);

        // Spawn it
        world.spawnEntity(skull);
    }


    @Override
    public Item getIcon() {
        return Items.WITHER_SKELETON_SKULL;
    }
}
