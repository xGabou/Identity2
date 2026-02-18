package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SnowGolemAbility extends IdentityAbility<SnowGolemEntity> {

    @Override
    public void onUse(PlayerEntity player, SnowGolemEntity identity, World world) {
        // Play throw sound
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL,
                0.5F,
                0.4F / (world.random.nextFloat() * 0.4F + 0.8F)
        );

        if (!world.isClient) {
            Vec3d look = player.getRotationVec(1.0F);
            Vec3d spawnPos = player.getEyePos().add(look.multiply(0.8)); // Slightly forward from eyes

            for (int i = 0; i < 10; i++) {
                SnowballEntity snowball = new SnowballEntity(world, player);
                snowball.setItem(new ItemStack(Items.SNOWBALL));

                // Randomize direction slightly for spread
                float pitchOffset = (float) (player.getPitch() + world.random.nextGaussian() * 5.0);
                float yawOffset = (float) (player.getYaw() + world.random.nextGaussian() * 5.0);

                snowball.setVelocity(player, pitchOffset, yawOffset, 0.0F, 1.5F, 1.0F);
                snowball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yawOffset, pitchOffset);

                world.spawnEntity(snowball);
            }
        }
    }


    @Override
    public Item getIcon() {
        return Items.SNOWBALL;
    }
}
