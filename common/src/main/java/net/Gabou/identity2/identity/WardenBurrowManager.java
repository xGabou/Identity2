package net.Gabou.identity2.identity;

import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    private static final double IDENTITY_BURROW_Y_OFFSET = 1.9D;

    private WardenBurrowManager() {
    }

    public static boolean isHidden(@Nullable Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return false;
        }
        CompoundTag nbt = ((NbtComponentAccessor) (Object) accessor.getCustomData()).getNbt();
        return nbt.contains(HIDDEN_KEY, Tag.TAG_BYTE) && nbt.getBoolean(HIDDEN_KEY);
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

        Vec3 burrowPos = resolveVisualBurrowPosition(player);
        setAnchor(player, burrowPos);
        moveIdentityToAnchor(player, burrowPos);
        player.resetFallDistance();
        IdentityApi.syncBoolean(player, HIDDEN_KEY, true);
        syncAnchor(player, burrowPos);
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
        player.resetFallDistance();
        player.noPhysics = false;
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

        Vec3 anchor = resolveVisualBurrowPosition(player);
        setAnchor(player, anchor);
        moveIdentityToAnchor(player, anchor);
        player.resetFallDistance();

        if (player.isSprinting() || player.isCrouching()) {
            stop(player, true);
        }
    }

    public static Vec3 resolveVisualBurrowPosition(Entity entity) {
        if (entity == null) {
            return Vec3.ZERO;
        }
        BlockPos below = entity.blockPosition().below();
        return new Vec3(entity.getX(), below.getY() - IDENTITY_BURROW_Y_OFFSET, entity.getZ());
    }

    public static boolean isWardenMorphed(@Nullable Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return false;
        }
        Entity currentIdentity = accessor.getCurrentIdentity();
        return currentIdentity != null && currentIdentity.getType() == EntityType.WARDEN;
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

    private static void syncAnchor(ServerPlayer player, Vec3 anchor) {
        if (player == null || anchor == null) {
            return;
        }
        IdentityApi.syncDouble(player, ANCHOR_X_KEY, anchor.x);
        IdentityApi.syncDouble(player, ANCHOR_Y_KEY, anchor.y);
        IdentityApi.syncDouble(player, ANCHOR_Z_KEY, anchor.z);
    }

    @Nullable
    private static Vec3 readAnchor(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        CompoundTag nbt = getCustomData(player);
        if (!nbt.contains(ANCHOR_X_KEY, Tag.TAG_DOUBLE) || !nbt.contains(ANCHOR_Y_KEY, Tag.TAG_DOUBLE) || !nbt.contains(ANCHOR_Z_KEY, Tag.TAG_DOUBLE)) {
            return null;
        }
        return new Vec3(
                nbt.getDouble(ANCHOR_X_KEY),
                nbt.getDouble(ANCHOR_Y_KEY),
                nbt.getDouble(ANCHOR_Z_KEY)
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

    private static void moveIdentityToAnchor(ServerPlayer player, Vec3 pos) {
        if (player == null || pos == null) {
            return;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }
        identity.setPos(pos.x, pos.y, pos.z);
        identity.setDeltaMovement(Vec3.ZERO);
    }
}
