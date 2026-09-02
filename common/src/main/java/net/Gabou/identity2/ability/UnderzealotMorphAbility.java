package net.Gabou.identity2.ability;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Optional-mod-safe primary burrow and cooperative Underzealot ritual. */
public final class UnderzealotMorphAbility implements BuiltinIdentityAbility {
    public static final UnderzealotMorphAbility INSTANCE = new UnderzealotMorphAbility();

    public static final String BURIED_STATE_KEY = "identity2.underzealot_buried";
    public static final String CARRYING_STATE_KEY = "identity2.underzealot_carrying";
    public static final String PRAYING_STATE_KEY = "identity2.underzealot_praying";

    private static final ResourceLocation UNDERZEALOT = id("alexscaves", "underzealot");
    private static final ResourceLocation GLOOMOTH = id("alexscaves", "gloomoth");
    private static final ResourceLocation VESPER = id("alexscaves", "vesper");
    private static final ResourceLocation WATCHER = id("alexscaves", "watcher");
    private static final ResourceLocation FORSAKEN = id("alexscaves", "forsaken");
    private static final int BASE_RITUAL_TICKS = 500;
    private static final double HOLD_WALK_AWAY_DISTANCE = 6.0D;
    private static final double ASSIST_DISTANCE = 12.0D;
    private static final double RECRUIT_DISTANCE = 30.0D;
    private static final double PRAYER_RING_RADIUS = 2.0D;
    private static final int MAX_NATIVE_HELPERS = 10;
    private static final float PRAYER_EXHAUSTION_PER_TICK = 0.02F;
    private static final String MINION_TAG = "identity2.underzealot_ritual_minion";
    private static final String OWNER_TAG_PREFIX = "identity2.underzealot_owner:";
    private static final String HAS_RITUAL_MINIONS_KEY = "identity2.has_underzealot_minions";
    private static final String RITUAL_COOLDOWN_END_KEY = "identity2.underzealot_ritual_cooldown_end";

    private static final Map<UUID, RitualGroup> GROUPS = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_GROUPS = new HashMap<>();
    private static final Map<UUID, UUID> HELPER_GROUPS = new HashMap<>();
    private static final Map<UUID, Long> BURROW_ANIMATION_UNTIL = new HashMap<>();
    private static MinecraftServer trackedServer;
    private static long lastGlobalTick = Long.MIN_VALUE;

    private UnderzealotMorphAbility() {
    }

    @Override
    public void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !isUnderzealot(player)) {
            return;
        }
        performBurrow(player);
    }

    @Override
    public void executeSecondary(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !isUnderzealot(player)) {
            return;
        }
        UUID currentTarget = PLAYER_GROUPS.get(player.getUUID());
        if (currentTarget != null) {
            RitualGroup group = GROUPS.get(currentTarget);
            if (group != null && group.holderUuid.equals(player.getUUID())) {
                cancelGroup(player.server, group, true, "Ritual cancelled.");
            } else {
                leaveGroup(player, group, "You stop praying.");
            }
            return;
        }

        long cooldownTicks = ritualCooldownRemaining(player);
        if (cooldownTicks > 0L) {
            long seconds = (cooldownTicks + 19L) / 20L;
            player.displayClientMessage(Component.literal(
                    "The ritual cannot be repeated for another " + seconds + " second" + (seconds == 1L ? "." : "s.")
            ), true);
            return;
        }

        LivingEntity lookedAt = findLookTarget(player, 6.0D);
        if (isSacrifice(lookedAt) && !lookedAt.isPassenger()) {
            if (!lookedAt.startRiding(player, true)) {
                player.displayClientMessage(Component.literal("The sacrifice cannot be lifted here."), true);
                return;
            }
            RitualGroup group = new RitualGroup(
                    lookedAt.getUUID(), player.getUUID(), player.getUUID(), true, player.position()
            );
            group.participants.add(player.getUUID());
            GROUPS.put(lookedAt.getUUID(), group);
            PLAYER_GROUPS.put(player.getUUID(), lookedAt.getUUID());
            setMorphBoolean(player, "setCarrying", true);
            player.displayClientMessage(Component.literal("Hold still: nearby Underzealots can accelerate the ritual."), true);
            return;
        }

        RitualGroup nearby = findNearbyRitual(player);
        if (nearby == null) {
            player.displayClientMessage(Component.literal("Look at a Gloomoth or Vesper, or approach an active Underzealot ritual."), true);
            return;
        }
        nearby.participants.add(player.getUUID());
        PLAYER_GROUPS.put(player.getUUID(), nearby.targetUuid);
        setMorphBoolean(player, "setPraying", true);
        player.displayClientMessage(Component.literal("You join the ritual. Praying consumes hunger."), true);
    }

    /** Called from the authoritative player base tick so cancellation survives key/network state. */
    public static void serverTick(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (trackedServer != player.server) {
            GROUPS.clear();
            PLAYER_GROUPS.clear();
            HELPER_GROUPS.clear();
            BURROW_ANIMATION_UNTIL.clear();
            trackedServer = player.server;
            lastGlobalTick = Long.MIN_VALUE;
        }
        tickBurrowAnimation(player);
        tickOwnedMinions(player);

        UUID targetUuid = PLAYER_GROUPS.get(player.getUUID());
        if (targetUuid != null) {
            RitualGroup ownGroup = GROUPS.get(targetUuid);
            if (ownGroup == null || !isUnderzealot(player)) {
                leaveGroup(player, ownGroup, null);
            }
        }

        long gameTick = player.serverLevel().getGameTime();
        if (lastGlobalTick == gameTick) {
            return;
        }
        lastGlobalTick = gameTick;
        tickGroups(player.server, gameTick);
    }

    private static void performBurrow(ServerPlayer player) {
        BlockState floor = player.level().getBlockState(player.blockPosition().below());
        if (floor.isAir() || !player.onGround()) {
            player.displayClientMessage(Component.literal("You need solid ground to burrow."), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 horizontal = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            horizontal = horizontal.normalize();
        }
        Vec3 start = player.position();
        Vec3 destination = start;
        Vec3 clipStart = start.add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 clipEnd = clipStart.add(horizontal.scale(5.0D));
        HitResult wallHit = level.clip(new ClipContext(
                clipStart, clipEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        double maxDistance = wallHit.getType() == HitResult.Type.MISS
                ? 5.0D
                : Math.max(0.0D, clipStart.distanceTo(wallHit.getLocation()) - 0.75D);
        for (double distance = Math.min(5.0D, Math.floor(maxDistance)); distance >= 1.0D; distance -= 1.0D) {
            Vec3 candidate = start.add(horizontal.scale(distance));
            AABB moved = player.getBoundingBox().move(candidate.subtract(start));
            if (level.noCollision(player, moved)
                    && !level.getBlockState(BlockPos.containing(candidate).below()).isAir()) {
                destination = candidate;
                break;
            }
        }

        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, floor);
        level.sendParticles(dust, player.getX(), player.getY() + 0.1D, player.getZ(), 32, 0.45D, 0.15D, 0.45D, 0.08D);
        player.teleportTo(destination.x, destination.y, destination.z);
        level.sendParticles(dust, player.getX(), player.getY() + 0.1D, player.getZ(), 32, 0.45D, 0.15D, 0.45D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 0.65F);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false, true));
        Entity morph = ((EntityAccessor) player).getCurrentIdentity();
        if (morph != null && UNDERZEALOT.equals(typeId(morph))) {
            // Native Underzealot code assumes this position exists whenever buried is true.
            invokeTwoArgs(morph, "reemergeAt", player.blockPosition(), 40);
        }
        setMorphBoolean(player, "setBuried", true);
        BURROW_ANIMATION_UNTIL.put(player.getUUID(), level.getGameTime() + 18L);
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 18);
    }

    private static void tickBurrowAnimation(ServerPlayer player) {
        Long until = BURROW_ANIMATION_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (player.serverLevel().getGameTime() < until && isUnderzealot(player)) {
            setMorphBoolean(player, "setBuried", true);
            return;
        }
        BURROW_ANIMATION_UNTIL.remove(player.getUUID());
        setMorphBoolean(player, "setBuried", false);
    }

    private static void tickGroups(MinecraftServer server, long gameTick) {
        if (server == null || GROUPS.isEmpty()) {
            return;
        }
        for (RitualGroup group : new ArrayList<>(GROUPS.values())) {
            ServerLevel level = findLevel(server, group.targetUuid);
            Entity target = level == null ? null : level.getEntity(group.targetUuid);
            Entity holder = level == null ? null : level.getEntity(group.holderUuid);
            if (!(target instanceof LivingEntity sacrifice) || !isSacrifice(sacrifice)
                    || !sacrifice.isAlive() || sacrifice.isRemoved()
                    || holder == null || !holder.isAlive() || holder.isRemoved()
                    || sacrifice.getVehicle() != holder) {
                cancelGroup(server, group, false, null);
                continue;
            }
            if (group.playerHolder && (!(holder instanceof ServerPlayer holderPlayer)
                    || !isUnderzealot(holderPlayer)
                    || holder.position().distanceToSqr(group.anchor) > HOLD_WALK_AWAY_DISTANCE * HOLD_WALK_AWAY_DISTANCE)) {
                cancelGroup(server, group, true, "You moved away; the Underzealots return to wandering.");
                continue;
            }
            if (holder instanceof LivingEntity livingHolder) {
                if (livingHolder.hurtTime > group.lastHolderHurtTime && level.random.nextFloat() < 0.65F) {
                    cancelGroup(server, group, true, "The hit makes you drop the sacrifice.");
                    continue;
                }
                group.lastHolderHurtTime = livingHolder.hurtTime;
            }

            pruneParticipants(server, group, holder);
            if (group.playerHolder && !group.participants.contains(group.holderUuid)) {
                cancelGroup(server, group, true, null);
                continue;
            }

            List<Mob> underzealots = nearbyUnderzealots(level, holder, group);
            Set<UUID> activeHelpers = new HashSet<>();
            for (Mob underzealot : underzealots) {
                activeHelpers.add(underzealot.getUUID());
                HELPER_GROUPS.put(underzealot.getUUID(), group.targetUuid);
            }
            releaseInactiveHelpers(server, group, activeHelpers);
            group.touchedUnderzealots.clear();
            group.touchedUnderzealots.addAll(activeHelpers);
            int nativePrayers = 0;
            for (int index = 0; index < underzealots.size(); index++) {
                Mob underzealot = underzealots.get(index);
                double angle = Math.PI * 2.0D * index / Math.max(1, underzealots.size());
                Vec3 ringPosition = holder.position().add(
                        Math.cos(angle) * PRAYER_RING_RADIUS,
                        0.0D,
                        Math.sin(angle) * PRAYER_RING_RADIUS
                );
                underzealot.getNavigation().moveTo(ringPosition.x, ringPosition.y, ringPosition.z, 1.0D);
                boolean inPrayerPosition = underzealot.distanceToSqr(ringPosition) <= 4.0D;
                invokeOneArg(underzealot, "setPraying", inPrayerPosition);
                invokeOneArg(underzealot, "setWorshipTime", inPrayerPosition ? Math.min(BASE_RITUAL_TICKS, (int) group.progress) : 0);
                invokeOneArg(underzealot, "setParticlePos", inPrayerPosition ? holder.blockPosition().above(5) : null);
                underzealot.getLookControl().setLookAt(sacrifice);
                if (inPrayerPosition) {
                    nativePrayers++;
                }
            }

            int joiners = 0;
            for (UUID participantUuid : group.participants) {
                ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
                if (participant == null) {
                    continue;
                }
                if (participantUuid.equals(group.holderUuid)) {
                    setMorphBoolean(participant, "setCarrying", true);
                } else {
                    joiners++;
                    setMorphBoolean(participant, "setPraying", true);
                    participant.causeFoodExhaustion(PRAYER_EXHAUSTION_PER_TICK);
                }
                if ((gameTick & 7L) == 0L) {
                    level.sendParticles(ParticleTypes.PORTAL, participant.getX(), participant.getEyeY(), participant.getZ(), 4, 0.25D, 0.35D, 0.25D, 0.02D);
                }
            }

            int prayers = nativePrayers + joiners;
            if (prayers >= 3) {
                group.progress += 1.0D + 0.1D * (Math.min(10, prayers) - 3);
            } else if ((gameTick % 40L) == 0L) {
                int needed = 3 - prayers;
                for (UUID participantUuid : group.participants) {
                    ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
                    if (participant != null) {
                        participant.displayClientMessage(Component.literal(
                                "The ritual needs " + needed + " more praying Underzealot" + (needed == 1 ? "." : "s.")
                        ), true);
                    }
                }
            }
            if (group.progress >= BASE_RITUAL_TICKS) {
                finishRitual(server, level, group, sacrifice, holder);
            }
        }
    }

    private static void pruneParticipants(MinecraftServer server, RitualGroup group, Entity holder) {
        Iterator<UUID> iterator = group.participants.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            ServerPlayer participant = server.getPlayerList().getPlayer(uuid);
            boolean isHolder = uuid.equals(group.holderUuid);
            boolean valid = participant != null
                    && participant.isAlive()
                    && !participant.isSpectator()
                    && isUnderzealot(participant)
                    && participant.level() == holder.level()
                    && (isHolder || participant.distanceToSqr(holder) <= ASSIST_DISTANCE * ASSIST_DISTANCE)
                    && (isHolder || participant.getFoodData().getFoodLevel() > 0);
            if (valid) {
                continue;
            }
            iterator.remove();
            PLAYER_GROUPS.remove(uuid);
            if (participant != null) {
                setMorphBoolean(participant, "setPraying", false);
                setMorphBoolean(participant, "setCarrying", false);
            }
        }
    }

    private static void finishRitual(
            MinecraftServer server,
            ServerLevel level,
            RitualGroup group,
            LivingEntity sacrifice,
            Entity holder
    ) {
        ResourceLocation outputId = GLOOMOTH.equals(typeId(sacrifice)) ? WATCHER : FORSAKEN;
        EntityType<?> outputType = BuiltInRegistries.ENTITY_TYPE.containsKey(outputId)
                ? BuiltInRegistries.ENTITY_TYPE.get(outputId)
                : null;
        sacrifice.stopRiding();
        Mob converted = null;
        if (sacrifice instanceof Mob mob && outputType != null) {
            try {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Mob result = mob.convertTo((EntityType) outputType, true);
                converted = result;
            } catch (Throwable ignored) {
            }
        }
        if (converted == null) {
            for (UUID participantUuid : group.participants) {
                ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
                if (participant != null) {
                    participant.displayClientMessage(Component.literal(
                            "The ritual falters; the sacrifice could not transform."
                    ), true);
                }
            }
            cancelGroup(server, group, false, null);
            return;
        }

        converted.setPersistenceRequired();
        if (group.initiatorUuid != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(group.initiatorUuid);
            bindMinion(owner, converted);
        }
        int successCooldown = 6000 + level.random.nextInt(6000);
        long cooldownEnd = level.getGameTime() + successCooldown;
        for (UUID participantUuid : group.participants) {
            ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
                ((EntityAccessor) participant).getCustomData().putLong(RITUAL_COOLDOWN_END_KEY, cooldownEnd);
                // Keep attempts responsive after the ordinary key cooldown so the custom timer can explain rejections.
                ((EntityAccessor) participant).setSecondaryAbilityCooldown(80);
            }
        }
        if (!(holder instanceof ServerPlayer)) {
            setIntField(holder, "sacrificeCooldown", successCooldown);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, holder.getX(), holder.getY() + 1.5D, holder.getZ(), 70, 1.2D, 1.2D, 1.2D, 0.08D);
        level.playSound(null, holder.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F, 0.55F);
        for (UUID participantUuid : group.participants) {
            ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
                participant.displayClientMessage(Component.literal(
                        outputId.equals(WATCHER) ? "The Gloomoth becomes a Watcher." : "The Vesper becomes a Forsaken."
                ), true);
            }
        }
        cancelGroup(server, group, false, null);
    }

    private static void bindMinion(ServerPlayer owner, Mob minion) {
        if (owner == null || minion == null) {
            return;
        }
        minion.addTag(MINION_TAG);
        minion.addTag(OWNER_TAG_PREFIX + owner.getUUID());
        ((EntityAccessor) owner).getCustomData().putBoolean(HAS_RITUAL_MINIONS_KEY, true);
        Scoreboard scoreboard = owner.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(owner.getScoreboardName());
        if (team == null) {
            String compact = owner.getUUID().toString().replace("-", "");
            String name = "id2" + compact.substring(0, Math.min(12, compact.length()));
            team = scoreboard.getPlayerTeam(name);
            if (team == null) {
                team = scoreboard.addPlayerTeam(name);
            }
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        }
        scoreboard.addPlayerToTeam(minion.getScoreboardName(), team);
    }

    private static void tickOwnedMinions(ServerPlayer owner) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(UNDERZEALOT)
                || !net.Gabou.identity2.util.NbtCompat.getBooleanOr(
                        ((EntityAccessor) owner).getCustomData(), HAS_RITUAL_MINIONS_KEY, false
                )
                || owner.tickCount % 10 != 0) {
            return;
        }
        String ownerTag = OWNER_TAG_PREFIX + owner.getUUID();
        LivingEntity defendTarget = owner.getLastHurtByMob();
        if (defendTarget == null || !defendTarget.isAlive() || defendTarget.isAlliedTo(owner)) {
            defendTarget = owner.getLastHurtMob();
        }
        LivingEntity finalTarget = defendTarget != null && defendTarget.isAlive() && !defendTarget.isAlliedTo(owner)
                ? defendTarget
                : null;
        for (Mob minion : owner.serverLevel().getEntitiesOfClass(
                Mob.class,
                owner.getBoundingBox().inflate(64.0D),
                mob -> mob.getTags().contains(MINION_TAG) && mob.getTags().contains(ownerTag)
        )) {
            if (minion.getTarget() == owner) {
                minion.setTarget(null);
            }
            if (finalTarget != null && minion.distanceToSqr(finalTarget) <= 64.0D * 64.0D) {
                minion.setTarget(finalTarget);
            }
        }
    }

    private static void cancelGroup(
            MinecraftServer server,
            RitualGroup group,
            boolean dismount,
            String holderMessage
    ) {
        if (group == null) {
            return;
        }
        GROUPS.remove(group.targetUuid);
        ServerLevel level = findLevel(server, group.targetUuid);
        Entity target = level == null ? null : level.getEntity(group.targetUuid);
        if (dismount && target != null && target.getVehicle() != null
                && target.getVehicle().getUUID().equals(group.holderUuid)) {
            target.stopRiding();
        }
        for (UUID participantUuid : group.participants) {
            PLAYER_GROUPS.remove(participantUuid);
            ServerPlayer participant = server == null ? null : server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
                setMorphBoolean(participant, "setPraying", false);
                setMorphBoolean(participant, "setCarrying", false);
                if (participantUuid.equals(group.holderUuid) && holderMessage != null) {
                    participant.displayClientMessage(Component.literal(holderMessage), true);
                }
            }
        }
        for (UUID underzealotUuid : group.touchedUnderzealots) {
            resetHelper(server, group, underzealotUuid);
        }
    }

    private static void leaveGroup(ServerPlayer player, RitualGroup group, String message) {
        PLAYER_GROUPS.remove(player.getUUID());
        if (group != null) {
            group.participants.remove(player.getUUID());
        }
        setMorphBoolean(player, "setPraying", false);
        setMorphBoolean(player, "setCarrying", false);
        if (message != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }

    private static RitualGroup findNearbyRitual(ServerPlayer player) {
        RitualGroup nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (RitualGroup group : GROUPS.values()) {
            ServerLevel level = findLevel(player.server, group.targetUuid);
            Entity holder = level == null ? null : level.getEntity(group.holderUuid);
            if (holder == null || holder.level() != player.level()) {
                continue;
            }
            double distance = player.distanceToSqr(holder);
            if (distance <= ASSIST_DISTANCE * ASSIST_DISTANCE && distance < nearestDistance) {
                nearest = group;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            return nearest;
        }

        for (Mob holder : player.serverLevel().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(ASSIST_DISTANCE),
                mob -> UNDERZEALOT.equals(typeId(mob)) && hasActiveNativeRitual(mob)
        )) {
            LivingEntity sacrifice = (LivingEntity) holder.getFirstPassenger();
            RitualGroup existing = GROUPS.get(sacrifice.getUUID());
            if (existing != null) {
                return existing;
            }
            RitualGroup group = new RitualGroup(
                    sacrifice.getUUID(), holder.getUUID(), player.getUUID(), false, holder.position()
            );
            GROUPS.put(sacrifice.getUUID(), group);
            return group;
        }
        return null;
    }

    private static List<Mob> nearbyUnderzealots(ServerLevel level, Entity holder, RitualGroup group) {
        Vec3 center = holder.position();
        AABB box = new AABB(center, center).inflate(RECRUIT_DISTANCE);
        List<Mob> candidates = new ArrayList<>(level.getEntitiesOfClass(
                Mob.class,
                box,
                mob -> UNDERZEALOT.equals(typeId(mob))
                        && !mob.getUUID().equals(holder.getUUID())
                        && mob.isAlive()
                        && !mob.isRemoved()
                        && !mob.isNoAi()
                        && !mob.isVehicle()
                        && (mob.getTarget() == null || !mob.getTarget().isAlive())
                        && helperAvailable(mob, group)
                        && mob.distanceToSqr(center) <= RECRUIT_DISTANCE * RECRUIT_DISTANCE
        ));
        candidates.sort(java.util.Comparator.comparingDouble(holder::distanceToSqr));
        if (candidates.size() > MAX_NATIVE_HELPERS) {
            return List.copyOf(candidates.subList(0, MAX_NATIVE_HELPERS));
        }
        return List.copyOf(candidates);
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        Vec3 ray = end.subtract(start);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                player.getBoundingBox().expandTowards(ray).inflate(1.0D),
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && candidate != player,
                start.distanceToSqr(end)
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void setMorphBoolean(ServerPlayer player, String method, boolean value) {
        String stateKey = switch (method) {
            case "setBuried" -> BURIED_STATE_KEY;
            case "setCarrying" -> CARRYING_STATE_KEY;
            case "setPraying" -> PRAYING_STATE_KEY;
            default -> null;
        };
        if (stateKey != null) {
            net.minecraft.nbt.CompoundTag customData = ((EntityAccessor) player).getCustomData();
            boolean current = net.Gabou.identity2.util.NbtCompat.getBooleanOr(customData, stateKey, !value);
            if (!customData.contains(stateKey) || current != value) {
                IdentityApi.syncBoolean(player, stateKey, value);
            }
        }
        Entity morph = ((EntityAccessor) player).getCurrentIdentity();
        if (morph != null && UNDERZEALOT.equals(typeId(morph))) {
            invokeOneArg(morph, method, value);
        }
    }

    private static boolean isUnderzealot(ServerPlayer player) {
        return player != null && UNDERZEALOT.equals(IdentityApi.getCurrentMorphId(player));
    }

    private static boolean isSacrifice(Entity entity) {
        ResourceLocation id = typeId(entity);
        return GLOOMOTH.equals(id) || VESPER.equals(id);
    }

    private static long ritualCooldownRemaining(ServerPlayer player) {
        net.minecraft.nbt.CompoundTag customData = ((EntityAccessor) player).getCustomData();
        if (!customData.contains(RITUAL_COOLDOWN_END_KEY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            return 0L;
        }
        return Math.max(0L, customData.getLong(RITUAL_COOLDOWN_END_KEY) - player.serverLevel().getGameTime());
    }

    private static boolean hasActiveNativeRitual(Mob holder) {
        if (holder == null || !holder.isAlive() || !isSacrifice(holder.getFirstPassenger())) {
            return false;
        }
        Object altar = invokeNoArg(holder, "getLastSacrificePos");
        return altar instanceof BlockPos pos
                && holder.distanceToSqr(Vec3.atCenterOf(pos)) < 6.25D;
    }

    private static boolean helperAvailable(Mob helper, RitualGroup group) {
        UUID owner = HELPER_GROUPS.get(helper.getUUID());
        boolean assignedHere = group.targetUuid.equals(owner);
        if (owner != null && !assignedHere) {
            return false;
        }
        if (Boolean.TRUE.equals(invokeNoArg(helper, "isBuried"))
                || Boolean.TRUE.equals(invokeNoArg(helper, "isCarrying"))) {
            return false;
        }
        if (!assignedHere && Boolean.TRUE.equals(invokeNoArg(helper, "isPraying"))) {
            return false;
        }
        return getIntField(helper, "sacrificeCooldown", 0) <= 0;
    }

    private static void releaseInactiveHelpers(MinecraftServer server, RitualGroup group, Set<UUID> activeHelpers) {
        for (UUID helperUuid : new HashSet<>(group.touchedUnderzealots)) {
            if (!activeHelpers.contains(helperUuid)) {
                resetHelper(server, group, helperUuid);
            }
        }
    }

    private static void resetHelper(MinecraftServer server, RitualGroup group, UUID helperUuid) {
        HELPER_GROUPS.remove(helperUuid, group.targetUuid);
        ServerLevel helperLevel = findLevel(server, helperUuid);
        Entity helper = helperLevel == null ? null : helperLevel.getEntity(helperUuid);
        if (helper != null && UNDERZEALOT.equals(typeId(helper))) {
            invokeOneArg(helper, "setPraying", false);
            invokeOneArg(helper, "setWorshipTime", 0);
            invokeOneArg(helper, "setParticlePos", null);
            if (helper instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
    }

    private static ServerLevel findLevel(MinecraftServer server, UUID entityUuid) {
        if (server == null || entityUuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(entityUuid) != null) {
                return level;
            }
        }
        return null;
    }

    private static ResourceLocation typeId(Entity entity) {
        return entity == null ? null : EntityType.getKey(entity.getType());
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    private static void invokeOneArg(Object target, String methodName, Object value) {
        if (target == null) {
            return;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (value != null && !wrap(parameter).isAssignableFrom(value.getClass())) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    method.invoke(target, value);
                } catch (Throwable ignored) {
                }
                return;
            }
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(target);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void invokeTwoArgs(Object target, String methodName, Object first, Object second) {
        if (target == null) {
            return;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if ((first != null && !wrap(parameters[0]).isAssignableFrom(first.getClass()))
                        || (second != null && !wrap(parameters[1]).isAssignableFrom(second.getClass()))) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    method.invoke(target, first, second);
                } catch (Throwable ignored) {
                }
                return;
            }
        }
    }

    private static void setIntField(Object target, String fieldName, int value) {
        if (target == null) {
            return;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                field.setInt(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static int getIntField(Object target, String fieldName, int fallback) {
        if (target == null) {
            return fallback;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                return field.getInt(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static final class RitualGroup {
        private final UUID targetUuid;
        private final UUID holderUuid;
        private final UUID initiatorUuid;
        private final boolean playerHolder;
        private final Vec3 anchor;
        private final Set<UUID> participants = new HashSet<>();
        private final Set<UUID> touchedUnderzealots = new HashSet<>();
        private double progress;
        private int lastHolderHurtTime;

        private RitualGroup(UUID targetUuid, UUID holderUuid, UUID initiatorUuid, boolean playerHolder, Vec3 anchor) {
            this.targetUuid = targetUuid;
            this.holderUuid = holderUuid;
            this.initiatorUuid = initiatorUuid;
            this.playerHolder = playerHolder;
            this.anchor = anchor;
        }
    }
}
