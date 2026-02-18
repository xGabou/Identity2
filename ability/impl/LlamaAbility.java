package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LlamaAbility <T extends LlamaEntity> extends IdentityAbility<T> {

    @Override
    public void onUse(PlayerEntity player, LlamaEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        Vec3d look = player.getRotationVec(1.0F);

        // Create and configure the spit
        LlamaSpitEntity spit = new LlamaSpitEntity(EntityType.LLAMA_SPIT, world);
        spit.setOwner(player);

        // Spawn position: a bit in front of the face to prevent self-hit
        Vec3d spawnPos = player.getEyePos().add(look.multiply(1.0));
        spit.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());

        // Set trajectory and speed
        spit.setVelocity(look.x, look.y, look.z, 1.5F, 10.0F);

        // Play llama spit sound
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_LLAMA_SPIT,
                player.getSoundCategory(),
                1.0F,
                1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F
        );

        // Spawn entity
        world.spawnEntity(spit);
    }


    @Override
    public Item getIcon() {
        return Items.LEAD;
    }
}
