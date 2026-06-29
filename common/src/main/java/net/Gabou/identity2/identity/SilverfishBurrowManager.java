package net.Gabou.identity2.identity;

import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SilverfishBurrowManager {
    public static final String HIDDEN_KEY = "identity2.silverfish_hidden";
    private static final String ANCHOR_X_KEY = "identity2.silverfish_anchor_x";
    private static final String ANCHOR_Y_KEY = "identity2.silverfish_anchor_y";
    private static final String ANCHOR_Z_KEY = "identity2.silverfish_anchor_z";
    private static final double EXIT_SEARCH_VERTICAL_STEP = 0.5D;
    private static final double EXIT_SEARCH_MAX_HEIGHT = 4.0D;

    private SilverfishBurrowManager() {
    }

    public static boolean isHidden(@Nullable Entity entity) {
        if (!(entity instanceof EntityAccessor accessor)) {
            return false;
        }
        CompoundTag nbt = ((NbtComponentAccessor) accessor.getCustomData()).getNbt();
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

        BlockPos burrowPos = findBurrowBlock(player);
        if (burrowPos == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("No silverfish-burrowable block found."), true);
            return false;
        }

        Vec3 anchor = Vec3.atCenterOf(burrowPos);
        setAnchor(player, anchor);
        player.noPhysics = true;
        player.teleportTo(anchor.x, anchor.y, anchor.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        IdentityApi.syncBoolean(player, HIDDEN_KEY, true);
        player.level().playSound(null, burrowPos, SoundEvents.SILVERFISH_HURT, SoundSource.HOSTILE, 0.8F, 0.8F);
        ((EntityAccessor) player).setSecondaryAbilityCooldown(40);
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

        if (player.isSprinting() || player.isUsingItem()) {
            stop(player, true);
        }
    }

    public static boolean isBurrowable(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (state.is(Blocks.INFESTED_STONE)
                || state.is(Blocks.INFESTED_COBBLESTONE)
                || state.is(Blocks.INFESTED_STONE_BRICKS)
                || state.is(Blocks.INFESTED_MOSSY_STONE_BRICKS)
                || state.is(Blocks.INFESTED_CRACKED_STONE_BRICKS)
                || state.is(Blocks.INFESTED_CHISELED_STONE_BRICKS)) {
            return true;
        }
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.DEEPSLATE_TILES);
    }

    private static boolean isEligible(@Nullable ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        return identity != null && identity.getType() == EntityType.SILVERFISH;
    }

    @Nullable
    private static BlockPos findBurrowBlock(ServerPlayer player) {
        HitResult hit = player.pick(4.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos targeted = blockHit.getBlockPos();
            if (isBurrowable(player.level().getBlockState(targeted))) {
                return targeted;
            }
            BlockPos below = targeted.relative(Direction.DOWN);
            if (isBurrowable(player.level().getBlockState(below))) {
                return below;
            }
        }

        BlockPos feet = player.blockPosition();
        for (BlockPos candidate : new BlockPos[] { feet, feet.below(), feet.above() }) {
            if (isBurrowable(player.level().getBlockState(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private static void setAnchor(ServerPlayer player, Vec3 anchor) {
        CompoundTag nbt = getCustomData(player);
        nbt.putDouble(ANCHOR_X_KEY, anchor.x);
        nbt.putDouble(ANCHOR_Y_KEY, anchor.y);
        nbt.putDouble(ANCHOR_Z_KEY, anchor.z);
    }

    @Nullable
    private static Vec3 readAnchor(ServerPlayer player) {
        CompoundTag nbt = getCustomData(player);
        if (!nbt.contains(ANCHOR_X_KEY, Tag.TAG_DOUBLE)
                || !nbt.contains(ANCHOR_Y_KEY, Tag.TAG_DOUBLE)
                || !nbt.contains(ANCHOR_Z_KEY, Tag.TAG_DOUBLE)) {
            return null;
        }
        return new Vec3(nbt.getDouble(ANCHOR_X_KEY), nbt.getDouble(ANCHOR_Y_KEY), nbt.getDouble(ANCHOR_Z_KEY));
    }

    private static void clearAnchor(ServerPlayer player) {
        CompoundTag nbt = getCustomData(player);
        nbt.remove(ANCHOR_X_KEY);
        nbt.remove(ANCHOR_Y_KEY);
        nbt.remove(ANCHOR_Z_KEY);
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        return ((NbtComponentAccessor) ((EntityAccessor) player).getCustomData()).getNbt();
    }

    @Nullable
    private static Vec3 findSafeExitPosition(ServerPlayer player, Vec3 anchor) {
        BlockPos origin = BlockPos.containing(anchor);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = origin.relative(direction);
            Vec3 candidate = Vec3.atCenterOf(adjacent);
            if (isSafe(player, candidate)) {
                return candidate;
            }
        }

        for (double y = EXIT_SEARCH_VERTICAL_STEP; y <= EXIT_SEARCH_MAX_HEIGHT; y += EXIT_SEARCH_VERTICAL_STEP) {
            Vec3 candidate = anchor.add(0.0D, y, 0.0D);
            if (isSafe(player, candidate)) {
                return candidate;
            }
        }

        return anchor.add(0.0D, 1.0D, 0.0D);
    }

    private static boolean isSafe(ServerPlayer player, Vec3 candidate) {
        player.setPos(candidate.x, candidate.y, candidate.z);
        return player.level().noCollision(player, player.getBoundingBox());
    }
}
