package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import draylar.identity.api.platform.IdentityConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EndermanAbility extends IdentityAbility<EndermanEntity> {

    @Override
    public void onUse(PlayerEntity player, EndermanEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        double maxDistance = IdentityConfig.getInstance().endermanAbilityTeleportDistance();
        HitResult hit = player.raycast(maxDistance, 0, true);
        Vec3d targetPos = hit.getPos();

        // Point de base converti en BlockPos
        BlockPos blockPos = BlockPos.ofFloored(targetPos);

        // Monte tant que le bloc est solide pour éviter de téléporter dans un bloc
        while (!isSafeTeleportSpot(world, blockPos) && blockPos.getY() < world.getTopY()) {
            blockPos = blockPos.up();
        }

        Vec3d safePos = Vec3d.ofCenter(blockPos);

        // Son départ
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        // Téléportation
        player.requestTeleport(safePos.x, safePos.y, safePos.z);

        // Son arrivée
        world.playSound(
                null,
                safePos.x,
                safePos.y,
                safePos.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private boolean isSafeTeleportSpot(World world, BlockPos pos) {
        BlockState blockAtFeet = world.getBlockState(pos);
        BlockState blockAtHead = world.getBlockState(pos.up());
        return blockAtFeet.isAir() && blockAtHead.isAir();
    }


    @Override
    public Item getIcon() {
        return Items.ENDER_PEARL;
    }
}
