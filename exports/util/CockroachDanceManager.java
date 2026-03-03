package draylar.identity.forge.util;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.item.ItemMaraca;
import draylar.identity.api.PlayerIdentity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CockroachDanceManager {
    private static final Map<UUID, Integer> DANCE_TICKS = new HashMap<>();

    /**
     * Forces a player to dance for a specified number of ticks.
     *
     * @param player The player to force to dance.
     * @param ticks  The number of ticks to force the player to dance for.
     */

    public static void forceDance(ServerPlayerEntity player, int ticks) {
        DANCE_TICKS.put(player.getUuid(), ticks);
    }


    public static void tick(ServerPlayerEntity player) {
        var identity = PlayerIdentity.getIdentity(player);
        if (!(identity instanceof EntityCockroach cockroach)) {
            DANCE_TICKS.remove(player.getUuid());
            return;
        }

        UUID id = player.getUuid();
        int remaining = DANCE_TICKS.getOrDefault(id, 0);

        // If you stand near a jukebox, reset to 200:
        if (player.getWorld().getBlockState(player.getBlockPos().down()).isOf(Blocks.JUKEBOX)) {
            remaining = 200;
        }

        ServerWorld world = (ServerWorld) cockroach.getWorld();
        if (remaining > 0) {
            // count down
            DANCE_TICKS.put(id, remaining - 1);

            // tell clients “start/continue dancing”
            if (player.getMainHandStack().getItem() instanceof ItemMaraca ||
                    player.getOffHandStack().getItem() instanceof ItemMaraca) {
                world.sendEntityStatus(player, (byte) 69);
                cockroach.setMaracas(true);
                tellOthersImPlayingLaCucaracha(player);
            }
            else{
                world.sendEntityStatus(player, (byte) 67);
                cockroach.setMaracas(false);
                identity$tellOtherCockroachesToStopDancing(cockroach, player);

            }
            // server‐side animate hitbox & physics
            cockroach.setDancing(true);
            cockroach.prevDanceProgress = cockroach.danceProgress;
            if (cockroach.danceProgress < 5.0F) cockroach.danceProgress++;
            if (!cockroach.isOnGround() || cockroach.getRandom().nextInt(200) == 0) {
                cockroach.randomWingFlapTick = 5 + cockroach.getRandom().nextInt(15);
            }
            if (cockroach.randomWingFlapTick > 0) cockroach.randomWingFlapTick--;
            cockroach.tick();
        } else if (DANCE_TICKS.containsKey(id)) {
            // dance just ended
            DANCE_TICKS.remove(id);

            // tell clients “stop dancing”
            world.sendEntityStatus(player, (byte) 68);
            cockroach.setNearestMusician(null);
            cockroach.setMaracas(false);
            identity$tellOtherCockroachesToStopDancing(cockroach, player);

            // server‐side final cleanup
            cockroach.setDancing(false);
            cockroach.prevDanceProgress = cockroach.danceProgress;
            if (cockroach.danceProgress > 0.0F) cockroach.danceProgress--;
        }
    }
    private static void tellOthersImPlayingLaCucaracha(ServerPlayerEntity identity) {
        for(EntityCockroach roach : identity.getWorld().getEntitiesByClass(EntityCockroach.class, getMusicianDistance(identity), EntityPredicates.EXCEPT_SPECTATOR)) {
            if (!roach.hasMaracas()) {
                roach.setNearestMusician(identity.getUuid());
            }
        }

    }
    private static Box getMusicianDistance(ServerPlayerEntity identity) {
        return identity.getBoundingBox().expand((double)10.0F, (double)10.0F, (double)10.0F);
    }
    @Unique
    private static void identity$tellOtherCockroachesToStopDancing(EntityCockroach cockroach, ServerPlayerEntity player) {
        for(EntityCockroach roach : player.getWorld().getEntitiesByClass(EntityCockroach.class, getMusicianDistance(player), EntityPredicates.VALID_ENTITY)) {
            if(roach != cockroach){
                try{
                    if (roach.hasMaracas() && roach.getNearestMusician().getUuid() == player.getUuid()) {
                        roach.setMaracas(false);
                        roach.setDancing(false);
                        roach.setNearbySongPlaying(player.getBlockPos(), false);
                    }
                }
                catch (NullPointerException e) {
                    roach.setDancing(false);
                    roach.setNearbySongPlaying(player.getBlockPos(), false);
                }
            }


        }
    }
}
