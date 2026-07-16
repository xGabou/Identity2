package net.Gabou.identity2.ability;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Executes the small, composable action vocabulary used by modded identity JSON files.
 * No optional-mod classes are referenced here, so these definitions are safe when a mod is absent.
 */
public final class DataDrivenMobAbility {
    private static final double MAX_RANGE = 24.0D;
    private static final double MAX_STRENGTH = 10.0D;
    private static final Map<IdentityAbilityDefinition, BuiltinIdentityAbility> CACHE = new ConcurrentHashMap<>();

    private DataDrivenMobAbility() {
    }

    public static boolean isDataDriven(IdentityAbilityDefinition definition) {
        return definition != null && definition.action() != null && !definition.action().isBlank();
    }

    public static boolean hasActiveAction(IdentityAbilityDefinition definition) {
        return isDataDriven(definition) && !"none".equalsIgnoreCase(definition.action().trim());
    }

    /**
     * Checks conditions that are known before an action starts. Both logical sides call this so
     * rejected uses do not begin a local or authoritative cooldown.
     */
    public static boolean canAttempt(Entity entity, IdentityAbilityDefinition definition, boolean showFeedback) {
        if (!(entity instanceof Player player)
                || definition == null
                || !player.isAlive()
                || player.isSpectator()) {
            return false;
        }

        String action = definition.action().trim().toLowerCase(Locale.ROOT);
        if (action.isEmpty() || "none".equals(action)) {
            return false;
        }
        if (!ModdedMobAbilityCoverage.KNOWN_ACTIONS.contains(action)) {
            return rejectAttempt(player, "Unknown morph ability action: " + action, showFeedback);
        }
        if (("water_dash".equals(action) || "water_exit_leap".equals(action))
                && !player.isInWaterOrBubble()) {
            return rejectAttempt(player, "This ability requires water.", showFeedback);
        }
        if ("lava_dash".equals(action) && !player.isInLava()) {
            return rejectAttempt(player, "This ability requires lava.", showFeedback);
        }
        if ("camouflage".equals(action)) {
            Vec3 horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            if (horizontal.lengthSqr() > 0.01D) {
                return rejectAttempt(player, "Stand still to camouflage.", showFeedback);
            }
        }
        if ("crop_growth".equals(action) && !IdentitySettings.enableModdedMorphWorldInteractions) {
            return rejectAttempt(player, "Morph world-interaction abilities are disabled by the server.", showFeedback);
        }
        if ("inventory_drop".equals(action)) {
            if (!IdentitySettings.enableModdedMorphInventoryInteractions) {
                return rejectAttempt(player, "Morph inventory-interaction abilities are disabled by the server.", showFeedback);
            }
            if (player.getInventory().isEmpty()) {
                return rejectAttempt(player, "There is no inventory item to drop.", showFeedback);
            }
        }
        return true;
    }

    private static boolean rejectAttempt(Player player, String message, boolean showFeedback) {
        if (showFeedback) {
            player.displayClientMessage(Component.literal(message), true);
        }
        return false;
    }

    public static BuiltinIdentityAbility create(IdentityAbilityDefinition definition) {
        return CACHE.computeIfAbsent(definition, ignored -> new BuiltinIdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeAction(player, definition);
            }
        });
    }

    private static void executeAction(Entity entity, IdentityAbilityDefinition definition) {
        if (!(entity instanceof ServerPlayer player)
                || definition == null
                || !player.isAlive()
                || player.isSpectator()) {
            return;
        }
        String action = definition.action().trim().toLowerCase(Locale.ROOT);
        double strength = Mth.clamp(definition.strength(), 0.0D, MAX_STRENGTH);
        double range = Mth.clamp(definition.range(), 0.5D, MAX_RANGE);
        int duration = Mth.clamp(definition.duration(), 1, 20 * 60);

        switch (action) {
            case "none" -> noSpecialAbility(player);
            case "dash" -> dash(player, strength, Math.max(0.06D, strength * 0.08D), false);
            case "air_dash" -> dash(player, strength, Math.max(0.12D, strength * 0.12D), false);
            case "upward_burst" -> addVelocity(player, new Vec3(0.0D, strength, 0.0D));
            case "leap", "glide_launch" -> dash(player, strength, Math.max(0.35D, strength * 0.55D), false);
            case "pounce" -> pounce(player, strength, range, false);
            case "down_pounce" -> pounce(player, strength, range, true);
            case "water_dash" -> conditionalDash(player, strength, true, false);
            case "lava_dash" -> conditionalDash(player, strength, false, true);
            case "charge" -> charge(player, strength, range, false);
            case "roll", "spin" -> charge(player, strength, range, true);
            case "swipe", "bite", "kick", "punch", "bill_strike", "venom_spur" ->
                    melee(player, strength, range, action, duration);
            case "poison_bite", "poison_sting" -> statusStrike(player, strength, range, MobEffects.POISON, duration, 0);
            case "frost_bite" -> frostStrike(player, strength, range, duration);
            case "fire_strike", "solar_flare" -> fireStrike(player, strength, range, duration);
            case "pull", "tongue_pull", "tentacle_pull", "jaw_pull" -> pull(player, strength, range);
            case "constrict" -> constrict(player, strength, range, duration);
            case "leech", "soul_leech" -> leech(player, strength, range);
            case "knockback", "gust" -> areaPush(player, strength, range, action.equals("gust"));
            case "shell_guard", "cocoon_guard" -> guard(player, duration);
            case "spore_cloud" -> areaEffect(player, range, MobEffects.WEAKNESS, duration, ParticleTypes.SPORE_BLOSSOM_AIR);
            case "ink_cloud" -> areaEffect(player, range, MobEffects.BLINDNESS, duration, ParticleTypes.SQUID_INK);
            case "stink_cloud" -> areaEffect(player, range, MobEffects.CONFUSION, duration, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            case "mucus_trail" -> areaEffect(player, range, MobEffects.MOVEMENT_SLOWDOWN, duration, ParticleTypes.ITEM_SLIME);
            case "mud_shot" -> rangedStrike(player, strength, range, MobEffects.MOVEMENT_SLOWDOWN, duration, ParticleTypes.SPLASH);
            case "void_shot" -> rangedStrike(player, strength, range, MobEffects.WEAKNESS, duration, ParticleTypes.PORTAL);
            case "hemolymph_shot" -> rangedStrike(player, strength, range, MobEffects.POISON, duration, ParticleTypes.CRIMSON_SPORE);
            case "sonar" -> sonar(player, range, duration);
            case "ore_sense" -> oreSense(player, range);
            case "camouflage" -> camouflage(player, duration);
            case "crop_growth" -> growCrops(player, range);
            case "tail_decoy", "burrow_escape" -> escape(player, duration);
            case "dance" -> dance(player);
            case "water_exit_leap" -> waterExitLeap(player, strength);
            case "inventory_drop" -> inventoryDrop(player);
            default -> player.displayClientMessage(Component.literal("Unknown morph ability action: " + action), true);
        }
    }

    private static void noSpecialAbility(ServerPlayer player) {
        player.displayClientMessage(Component.literal("This identity has no special active ability."), true);
    }

    private static void dash(ServerPlayer player, double horizontal, double upward, boolean replace) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-6D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 impulse = horizontalLook.scale(horizontal).add(0.0D, upward, 0.0D);
        setVelocity(player, replace ? impulse : player.getDeltaMovement().add(impulse));
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 10);
    }

    private static void conditionalDash(ServerPlayer player, double strength, boolean water, boolean lava) {
        boolean valid = water ? player.isInWaterOrBubble() : lava && player.isInLava();
        if (!valid) {
            player.displayClientMessage(Component.literal(water ? "This ability requires water." : "This ability requires lava."), true);
            return;
        }
        dash(player, strength, Math.max(0.05D, strength * 0.05D), false);
    }

    private static void pounce(ServerPlayer player, double strength, double range, boolean downward) {
        if (downward && player.onGround()) {
            downward = false;
        }
        if (downward) {
            Vec3 look = player.getViewVector(1.0F);
            setVelocity(player, new Vec3(look.x * strength, -Math.max(0.65D, strength), look.z * strength));
        } else {
            dash(player, strength, Math.max(0.35D, strength * 0.4D), false);
        }
        LivingEntity target = findTarget(player, range);
        if (target != null && canAffect(player, target)) {
            hurt(player, target, Math.max(2.0D, strength * 3.0D));
        }
    }

    private static void charge(ServerPlayer player, double strength, double range, boolean radial) {
        dash(player, strength, 0.12D, false);
        if (radial) {
            areaPush(player, Math.max(0.8D, strength), Math.min(4.0D, range), false);
            return;
        }
        LivingEntity target = findTarget(player, range);
        if (target != null && canAffect(player, target)) {
            hurt(player, target, Math.max(3.0D, strength * 4.0D));
            Vec3 away = target.position().subtract(player.position()).normalize();
            target.push(away.x * strength, 0.3D, away.z * strength);
        }
    }

    private static void melee(ServerPlayer player, double strength, double range, String action, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        hurt(player, target, Math.max(2.0D, strength));
        Vec3 away = target.position().subtract(player.position());
        if (away.lengthSqr() > 1.0E-6D) {
            away = away.normalize();
            target.push(away.x * Math.min(1.4D, strength * 0.15D), 0.12D, away.z * Math.min(1.4D, strength * 0.15D));
        }
        if (action.equals("venom_spur")) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0));
        }
        player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.9F);
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 10);
    }

    private static void statusStrike(ServerPlayer player, double strength, double range, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        hurt(player, target, Math.max(1.0D, strength));
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 10);
    }

    private static void frostStrike(ServerPlayer player, double strength, double range, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        hurt(player, target, Math.max(2.0D, strength));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), Math.min(duration, target.getTicksRequiredToFreeze())));
    }

    private static void fireStrike(ServerPlayer player, double strength, double range, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        hurt(player, target, Math.max(2.0D, strength));
        target.setSecondsOnFire(Math.max(1, duration / 20));
    }

    private static void pull(ServerPlayer player, double strength, double range) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        Vec3 direction = player.position().add(0.0D, 0.4D, 0.0D).subtract(target.position());
        if (direction.lengthSqr() > 1.0E-6D) {
            direction = direction.normalize().scale(Math.min(1.8D, Math.max(0.25D, strength)));
            target.setDeltaMovement(target.getDeltaMovement().add(direction));
            target.hurtMarked = true;
        }
    }

    private static void constrict(ServerPlayer player, double strength, double range, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        hurt(player, target, Math.max(1.0D, strength));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.15D, 0.3D, 0.15D));
        target.hurtMarked = true;
    }

    private static void leech(ServerPlayer player, double strength, double range) {
        LivingEntity target = findTarget(player, range);
        if (target == null || !canAffect(player, target)) {
            return;
        }
        float damage = clampDamage(Math.max(1.0D, strength));
        if (damage <= 0.0F) {
            return;
        }
        if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            player.heal(Math.max(1.0F, damage * 0.5F));
        }
    }

    private static void areaPush(ServerPlayer player, double strength, double range, boolean blind) {
        for (LivingEntity target : nearbyTargets(player, range)) {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-6D) {
                continue;
            }
            away = away.normalize();
            target.push(away.x * strength, Math.min(0.7D, strength * 0.25D), away.z * strength);
            if (blind) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        }
        player.level().playSound(null, player, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static void guard(ServerPlayer player, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4, true, false, true));
    }

    private static void areaEffect(ServerPlayer player, double range, net.minecraft.world.effect.MobEffect effect, int duration, net.minecraft.core.particles.ParticleOptions particle) {
        for (LivingEntity target : nearbyTargets(player, range)) {
            target.addEffect(new MobEffectInstance(effect, duration, 0));
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(particle, player.getX(), player.getY() + 0.6D, player.getZ(), 35, range * 0.35D, 0.5D, range * 0.35D, 0.02D);
    }

    private static void rangedStrike(ServerPlayer player, double strength, double range, net.minecraft.world.effect.MobEffect effect, int duration, net.minecraft.core.particles.ParticleOptions particle) {
        LivingEntity target = findTarget(player, range);
        Vec3 start = player.getEyePosition();
        Vec3 end = target == null ? start.add(player.getViewVector(1.0F).scale(range)) : target.getEyePosition();
        spawnParticleLine(player.serverLevel(), particle, start, end);
        if (target != null && canAffect(player, target)) {
            hurt(player, target, Math.max(1.0D, strength));
            target.addEffect(new MobEffectInstance(effect, duration, 0));
        }
    }

    private static void sonar(ServerPlayer player, double range, int duration) {
        for (LivingEntity target : nearbyTargetsThroughWalls(player, range)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, true, false, true));
        }
        player.serverLevel().sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY(), player.getZ(), 24, range * 0.2D, range * 0.1D, range * 0.2D, 0.0D);
        player.level().playSound(null, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.55F);
    }

    private static void oreSense(ServerPlayer player, double range) {
        int radius = Mth.clamp((int) Math.ceil(range), 2, 12);
        BlockPos origin = player.blockPosition();
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (!isOre(player.serverLevel().getBlockState(pos))) {
                continue;
            }
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 2, 0.15D, 0.15D, 0.15D, 0.0D);
            if (++found >= 24) {
                break;
            }
        }
        player.displayClientMessage(Component.literal("Ore sense found " + found + " nearby ore block" + (found == 1 ? "." : "s.")), true);
    }

    private static boolean isOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES);
    }

    private static void camouflage(ServerPlayer player, int duration) {
        Vec3 horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        if (horizontal.lengthSqr() > 0.01D) {
            player.displayClientMessage(Component.literal("Stand still to camouflage."), true);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false, true));
    }

    private static void growCrops(ServerPlayer player, double range) {
        if (!IdentitySettings.enableModdedMorphWorldInteractions) {
            player.displayClientMessage(Component.literal("Morph world-interaction abilities are disabled by the server."), true);
            return;
        }
        int radius = Mth.clamp((int) Math.ceil(range), 1, 5);
        BlockPos origin = player.blockPosition();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, 2, radius))) {
            BlockState state = player.serverLevel().getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock growable) || !growable.isValidBonemealTarget(player.serverLevel(), pos, state, false)) {
                continue;
            }
            if (growable.isBonemealSuccess(player.serverLevel(), player.getRandom(), pos, state)) {
                growable.performBonemeal(player.serverLevel(), player.getRandom(), pos, state);
                if (++grown >= 12) {
                    break;
                }
            }
        }
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5D, player.getZ(), 24, radius, 0.4D, radius, 0.0D);
    }

    private static void escape(ServerPlayer player, int duration) {
        int shortDuration = Math.min(duration, 60);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, shortDuration, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, shortDuration, 1, true, false, true));
        player.serverLevel().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5D, player.getZ(), 24, 0.4D, 0.4D, 0.4D, 0.02D);
    }

    private static void dance(ServerPlayer player) {
        player.level().playSound(null, player, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        player.serverLevel().sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private static void waterExitLeap(ServerPlayer player, double strength) {
        if (!player.isInWaterOrBubble()) {
            player.displayClientMessage(Component.literal("This ability requires water."), true);
            return;
        }
        dash(player, Math.max(0.5D, strength), Math.max(0.7D, strength), false);
    }

    private static void inventoryDrop(ServerPlayer player) {
        if (!IdentitySettings.enableModdedMorphInventoryInteractions) {
            player.displayClientMessage(Component.literal("Morph inventory-interaction abilities are disabled by the server."), true);
            return;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                continue;
            }
            player.drop(player.getInventory().removeItem(slot, 1), false);
            return;
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        Vec3 ray = end.subtract(start);
        AABB search = player.getBoundingBox().expandTowards(ray).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                search,
                candidate -> candidate instanceof LivingEntity living && canAffect(player, living),
                start.distanceToSqr(end)
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> nearbyTargets(ServerPlayer player, double range) {
        double rangeSqr = range * range;
        return player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> canAffect(player, target)
                        && player.distanceToSqr(target) <= rangeSqr
                        && player.hasLineOfSight(target)
        );
    }

    private static List<LivingEntity> nearbyTargetsThroughWalls(ServerPlayer player, double range) {
        double rangeSqr = range * range;
        return player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> canAffect(player, target) && player.distanceToSqr(target) <= rangeSqr
        );
    }

    private static boolean canAffect(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isAlliedTo(player)) {
            return false;
        }
        if (target instanceof Player other && !IdentitySettings.enableModdedMorphFriendlyFire) {
            return false;
        }
        return !(target instanceof Player other) || player.canHarmPlayer(other);
    }

    private static void hurt(ServerPlayer player, LivingEntity target, double requestedDamage) {
        float damage = clampDamage(requestedDamage);
        if (damage <= 0.0F) {
            return;
        }
        target.hurt(player.damageSources().playerAttack(player), damage);
    }

    private static float clampDamage(double requestedDamage) {
        return (float) Mth.clamp(
                requestedDamage,
                0.0D,
                Math.max(0.0D, IdentitySettings.moddedMorphAbilityDamageCap)
        );
    }

    private static void addVelocity(ServerPlayer player, Vec3 impulse) {
        setVelocity(player, player.getDeltaMovement().add(impulse));
    }

    private static void setVelocity(ServerPlayer player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void spawnParticleLine(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(2, Mth.ceil(delta.length() * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
