package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EndermiteAbility extends IdentityAbility<EndermiteEntity> {

    @Override
    public void onUse(PlayerEntity player, EndermiteEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        double startX = player.getX();
        double startY = player.getY();
        double startZ = player.getZ();

        for (int i = 0; i < 16; ++i) {
            // Random target position around the player
            double targetX = startX + (player.getRandom().nextDouble() - 0.5D) * 16.0D;
            double targetY = MathHelper.clamp(startY + (player.getRandom().nextInt(16) - 8), world.getBottomY(), world.getTopY() - 1);
            double targetZ = startZ + (player.getRandom().nextDouble() - 0.5D) * 16.0D;

            // Ensure player dismounts before teleport
            if (player.hasVehicle()) {
                player.stopRiding();
            }

            // Try teleporting; returns true if success
            if (player.teleport(targetX, targetY, targetZ, true)) {
                SoundEvent teleportSound = SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;

                // Departure sound
                world.playSound(null, startX, startY, startZ, teleportSound, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Arrival sound
                player.playSound(teleportSound, 1.0F, 1.0F);
                break;
            }
        }
    }


    @Override
    public Item getIcon() {
        return Items.CHORUS_FRUIT;
    }
}
