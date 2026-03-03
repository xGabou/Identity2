package draylar.identity.forge.ability.impl;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import draylar.identity.ability.IdentityAbility;
import draylar.identity.forge.util.CockroachDanceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class CockRoachAbility extends IdentityAbility<EntityCockroach> {

    @Override
    public void onUse(PlayerEntity player, EntityCockroach identity, World world) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            // no longer need debug‐util: just send 67 to kick off the dance
            sp.getWorld().sendEntityStatus(player, (byte) 67);
            CockroachDanceManager.forceDance(sp, 200);
        }
    }


    @Override
    public Item getIcon() {
        return Items.MUSIC_DISC_CAT;
    }
}
