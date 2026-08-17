package net.Gabou.identity2.identity;

import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class WardenBurrowManager {
    public static final String HIDDEN_KEY = "identity2.warden_hidden";
    private static final String ANCHOR_X_KEY = "identity2.warden_hidden_anchor_x";
    private static final String ANCHOR_Y_KEY = "identity2.warden_hidden_anchor_y";
    private static final String ANCHOR_Z_KEY = "identity2.warden_hidden_anchor_z";
    private static final double BURROW_SEARCH_STEP = 0.1D;
    private static final double BURROW_SEARCH_DEPTH = 2.0D;
    private static final double EXIT_SEARCH_VERTICAL_STEP = 0.5D;
    private static final double EXIT_SEARCH_MAX_HEIGHT = 8.0D;
    private static final double EXIT_SEARCH_RADIUS_STEP = 0.5D;
    private static final double EXIT_SEARCH_MAX_RADIUS = 4.0D;
    private static final int EXIT_SEARCH_RADIAL_SAMPLES = 16;

    private WardenBurrowManager() {
    }

    public static boolean isHidden(@Nullable Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return false;
        }
        CompoundTag nbt = ((NbtComponentAccessor) (Object) accessor.getCustomData()).getNbt();
        return nbt.getBooleanOr(HIDDEN_KEY, false);
    }

    public static boolean toggle(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        if (isHidden(player)) {
            return stop(player, true);
        }
        return start(player);
    }

    public static boolean start(ServerPlayer player) {
        if (!isEligible(player) || isHidden(player)) {
            return false;
        }

        Vec3 burrowPos = findBurrowPosition(player);
        setAnchor(player, burrowPos);
        player.teleportTo(burrowPos.x, burrowPos.y, burrowPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.noPhysics = true;
        IdentityApi.syncBoolean(player, HIDDEN_KEY, true);
        return true;
    }

    public static boolean stop(ServerPlayer player, boolean safeExit) {
        if (!isHidden(player)) {
            return false;
        }

        Vec3 anchor = readAnchor(player);
        if (anchor == null) {
            anchor = player.position();
        }

        IdentityApi.syncBoolean(player, HIDDEN_KEY, false);
        clearAnchor(player);
        player.noPhysics = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        if (safeExit) {
            Vec3 exitPos = findSafeExitPosition(player, anchor);
            if (exitPos != null) {
                player.teleportTo(exitPos.x, exitPos.y, exitPos.z);
            }
        }

        player.noPhysics = false;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        return true;
    }

    public static void serverTick(ServerPlayer player) {
        if (!isHidden(player)) {
            return;
        }

        if (!isEligible(player)) {
            stop(player, true);
            return;
        }

        Vec3 anchor = readAnchor(player);
        if (anchor == null) {
            anchor = player.position();
            setAnchor(player, anchor);
        }

        player.noPhysics = true;
        player.teleportTo(anchor.x, anchor.y, anchor.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        if (player.isSprinting() || player.isCrouching() || player.isUsingItem()) {
            stop(player, true);
        }
    }

    public static boolean isWardenMorphed(@Nullable Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return false;
        }
        Entity currentIdentity = accessor.getCurrentIdentity();
        return currentIdentity != null && currentIdentity.getType() == net.minecraft.world.entity.EntityTypes.WARDEN;
    }

    private static boolean isEligible(@Nullable ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        return isWardenMorphed(player);
    }

    private static void setAnchor(ServerPlayer player, Vec3 anchor) {
        if (player == null || anchor == null) {
            return;
        }
        CompoundTag nbt = getCustomData(player);
        nbt.putDouble(ANCHOR_X_KEY, anchor.x);
        nbt.putDouble(ANCHOR_Y_KEY, anchor.y);
        nbt.putDouble(ANCHOR_Z_KEY, anchor.z);
    }

    @Nullable
    private static Vec3 readAnchor(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        CompoundTag nbt = getCustomData(player);
        if (!nbt.contains(ANCHOR_X_KEY) || !nbt.contains(ANCHOR_Y_KEY) || !nbt.contains(ANCHOR_Z_KEY)) {
            return null;
        }
        return new Vec3(
                nbt.getDoubleOr(ANCHOR_X_KEY, player.getX()),
                nbt.getDoubleOr(ANCHOR_Y_KEY, player.getY()),
                nbt.getDoubleOr(ANCHOR_Z_KEY, player.getZ())
        );
    }

    private static void clearAnchor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag nbt = getCustomData(player);
        nbt.remove(ANCHOR_X_KEY);
        nbt.remove(ANCHOR_Y_KEY);
        nbt.remove(ANCHOR_Z_KEY);
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        return ((NbtComponentAccessor) (Object) ((EntityAccessor) player).getCustomData()).getNbt();
    }

    @Nullable
    private static Vec3 findBurrowPosition(ServerPlayer player) {
        Vec3 origin = player.position();
        if (isColliding(player, origin)) {
            return origin;
        }
        for (double depth = BURROW_SEARCH_STEP; depth <= BURROW_SEARCH_DEPTH; depth += BURROW_SEARCH_STEP) {
            Vec3 candidate = origin.add(0.0D, -depth, 0.0D);
            if (isColliding(player, candidate)) {
                return candidate;
            }
        }
        return origin;
    }

    @Nullable
    private static Vec3 findSafeExitPosition(ServerPlayer player, Vec3 anchor) {
        if (player == null || anchor == null) {
            return null;
        }

        for (double y = EXIT_SEARCH_VERTICAL_STEP; y <= EXIT_SEARCH_MAX_HEIGHT; y += EXIT_SEARCH_VERTICAL_STEP) {
            Vec3 candidate = anchor.add(0.0D, y, 0.0D);
            if (isSafe(player, candidate)) {
                return candidate;
            }
        }

        for (double radius = EXIT_SEARCH_RADIUS_STEP; radius <= EXIT_SEARCH_MAX_RADIUS; radius += EXIT_SEARCH_RADIUS_STEP) {
            for (int i = 0; i < EXIT_SEARCH_RADIAL_SAMPLES; i++) {
                double angle = (Math.PI * 2.0D * i) / EXIT_SEARCH_RADIAL_SAMPLES;
                Vec3 candidate = anchor.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                if (isSafe(player, candidate)) {
                    return candidate;
                }
                for (double y = EXIT_SEARCH_VERTICAL_STEP; y <= EXIT_SEARCH_MAX_HEIGHT; y += EXIT_SEARCH_VERTICAL_STEP) {
                    Vec3 elevated = candidate.add(0.0D, y, 0.0D);
                    if (isSafe(player, elevated)) {
                        return elevated;
                    }
                }
            }
        }

        return anchor;
    }

    private static boolean isColliding(ServerPlayer player, Vec3 candidate) {
        if (player == null || candidate == null) {
            return false;
        }
        player.setPos(candidate.x, candidate.y, candidate.z);
        return !player.level().noCollision(player, player.getBoundingBox());
    }

    private static boolean isSafe(ServerPlayer player, Vec3 candidate) {
        if (player == null || candidate == null) {
            return false;
        }
        player.setPos(candidate.x, candidate.y, candidate.z);
        return player.level().noCollision(player, player.getBoundingBox());
    }
}
