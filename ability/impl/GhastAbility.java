package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GhastAbility extends IdentityAbility<GhastEntity> {

    @Override
    public void onUse(PlayerEntity player, GhastEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        Vec3d look = player.getRotationVec(1.0F);

        // Position the fireball slightly forward and at head height
        Vec3d spawnPos = player.getEyePos().add(look.multiply(4.0));

        // Fireball speed vector
        Vec3d velocity = look.multiply(1.0);

        FireballEntity fireball = new FireballEntity(
                world,
                player,
                velocity,
                1 // explosion power (same as ghast)
        );

        // Move the fireball to appear at mouth level
        fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        fireball.setOwner(player);

        world.spawnEntity(fireball);

        // Ghast sound effects
        world.playSoundFromEntity(null, player, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 10.0F, 1.0F);
        world.playSoundFromEntity(null, player, SoundEvents.ENTITY_GHAST_WARN, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }



    @Override
    public Item getIcon() {
        return Items.FIRE_CHARGE;
    }
}
