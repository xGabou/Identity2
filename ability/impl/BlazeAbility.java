package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlazeAbility extends IdentityAbility<BlazeEntity> {

    @Override
    public void onUse(PlayerEntity player, BlazeEntity identity, World world) {
        if (!world.isClient) {
            Vec3d look = player.getRotationVec(1.0F);
            Vec3d spawnPos = player.getEyePos().add(look.multiply(0.6)); // near head, slightly forward
            Vec3d velocity = look.multiply(0.5); // optional: slower for vanilla-like feel

            SmallFireballEntity smallFireball = new SmallFireballEntity(
                    world,
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    velocity
            );

            smallFireball.setOwner(player);
            world.spawnEntity(smallFireball);

            world.playSoundFromEntity(
                    null,
                    player,
                    SoundEvents.ENTITY_BLAZE_SHOOT,
                    SoundCategory.HOSTILE,
                    2.0F,
                    (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F
            );
        }
    }



    @Override
    public Item getIcon() {
        return Items.BLAZE_POWDER;
    }
}
