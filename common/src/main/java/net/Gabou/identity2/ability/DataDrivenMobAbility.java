package net.Gabou.identity2.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.Gabou.identity2.util.EntityNbtIoCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Executes the small, composable action vocabulary used by modded identity JSON files.
 * No optional-mod classes are referenced here, so these definitions are safe when a mod is absent.
 */
public final class DataDrivenMobAbility {
    private static final double MAX_RANGE = 24.0D;
    private static final double MAX_STRENGTH = 10.0D;
    private static final Map<IdentityAbilityDefinition, BuiltinIdentityAbility> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Long>> DREAD_SUMMONS = new ConcurrentHashMap<>();
    private static final String DREAD_SUMMON_TAG = "identity2.dread_summon";
    private static final String DREAD_SUMMON_TAG_PREFIX = "identity2.dread_summon:";
    private static final String DREAD_SUMMON_EXPIRY_KEY = "identity2.dread_summon_expiry";
    private static MinecraftServer dreadSummonServer;

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

    /** Maintains the bounded, opt-in Dread Lich helpers after their caster changes morph. */
    public static void serverTick(ServerPlayer owner) {
        if (owner == null || owner.level().isClientSide() || owner.tickCount % 10 != 0) {
            return;
        }
        if (dreadSummonServer != owner.server) {
            DREAD_SUMMONS.clear();
            dreadSummonServer = owner.server;
        }
        Map<UUID, Long> summons = DREAD_SUMMONS.get(owner.getUUID());
        if (summons == null || summons.isEmpty()) {
            DREAD_SUMMONS.remove(owner.getUUID());
            return;
        }
        LivingEntity defendTarget = owner.getLastHurtByMob();
        if (defendTarget == null || !defendTarget.isAlive() || defendTarget.isAlliedTo(owner)) {
            defendTarget = owner.getLastHurtMob();
        }
        if (defendTarget != null && (!defendTarget.isAlive() || defendTarget.isAlliedTo(owner))) {
            defendTarget = null;
        }
        long gameTime = owner.serverLevel().getGameTime();
        for (Map.Entry<UUID, Long> entry : new HashMap<>(summons).entrySet()) {
            Entity entity = findServerEntity(owner, entry.getKey());
            if (!(entity instanceof Mob minion)) {
                if (gameTime >= entry.getValue()) {
                    summons.remove(entry.getKey());
                }
                continue;
            }
            if (gameTime >= entry.getValue() || !owner.isAlive()) {
                removeFromOwnerTeam(minion);
                minion.discard();
                summons.remove(entry.getKey());
                continue;
            }
            if (minion.getTarget() == owner || (minion.getTarget() != null && minion.getTarget().isAlliedTo(owner))) {
                minion.setTarget(null);
            }
            if (defendTarget != null && minion.distanceToSqr(defendTarget) <= 64.0D * 64.0D) {
                minion.setTarget(defendTarget);
            }
        }
        if (summons.isEmpty()) {
            DREAD_SUMMONS.remove(owner.getUUID());
        }
    }

    /** Self-expiry for a summon that unloads or whose owner disconnects before the owner tick sees it. */
    public static void tickTemporarySummon(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !entity.getTags().contains(DREAD_SUMMON_TAG)) {
            return;
        }
        net.minecraft.nbt.CompoundTag data = ((EntityAccessor) entity).getCustomData();
        if (!data.contains(DREAD_SUMMON_EXPIRY_KEY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            entity.discard();
            return;
        }
        if (entity.level().getGameTime() >= data.getLong(DREAD_SUMMON_EXPIRY_KEY)) {
            removeFromOwnerTeam(entity);
            entity.discard();
        }
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
            case "dragon_fire_breath" -> dragonBreath(player, strength, range, duration, DragonElement.FIRE);
            case "dragon_ice_breath" -> dragonBreath(player, strength, range, duration, DragonElement.ICE);
            case "dragon_lightning_breath" -> dragonBreath(player, strength, range, duration, DragonElement.LIGHTNING);
            case "petrifying_gaze" -> petrifyingGaze(player, range, duration);
            case "pixie_blessing" -> pixieBlessing(player, duration);
            case "cyclops_stomp", "troll_slam" -> slam(player, strength, range);
            case "siren_song" -> sirenSong(player, strength, range, duration);
            case "deathworm_burrow_burst" -> deathwormBurrowBurst(player, strength, range, duration);
            case "cockatrice_gaze" -> cockatriceGaze(player, strength, range, duration);
            case "feather_volley" -> featherVolley(player, strength, range);
            case "sentinel_ambush" -> sentinelAmbush(player, strength, range, duration);
            case "sea_serpent_bubbles" -> seaSerpentBubbles(player, strength, range);
            case "dread_summon" -> dreadSummon(player, strength, range, duration);
            case "hydra_venom_volley" -> hydraVenomVolley(player, strength, range, duration);
            case "ghost_phase_charge" -> ghostPhaseCharge(player, strength, range, duration);
            case "blast" -> safeBlast(player, strength, range);
            case "radiation_absorb" -> radiationAbsorb(player, strength, range, duration);
            case "steal" -> stealTaggedItem(player, range);
            default -> player.displayClientMessage(Component.literal("Unknown morph ability action: " + action), true);
        }
    }

    private static void dragonBreath(
            ServerPlayer player,
            double baseStrength,
            double requestedRange,
            int duration,
            DragonElement element
    ) {
        int stage = currentDragonStage(player);
        double damage = baseStrength * stage;
        double range = Math.min(MAX_RANGE, Math.max(8.0D, requestedRange));
        List<LivingEntity> targets = coneTargets(player, range, 0.92D, 8);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        net.minecraft.core.particles.ParticleOptions particle = switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
        };
        spawnParticleLine(player.serverLevel(), particle, start, end);
        for (LivingEntity target : targets) {
            hurtDragon(player, target, damage);
            switch (element) {
                case FIRE -> target.setSecondsOnFire(Math.max(2, duration / 20 + stage));
                case ICE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration + stage * 20, Math.min(3, stage / 2)));
                    target.setTicksFrozen(Math.max(target.getTicksFrozen(), Math.min(300, duration + stage * 30)));
                }
                case LIGHTNING -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(20, duration / 2), 0));
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
                    if (bolt != null) {
                        bolt.moveTo(target.position());
                        bolt.setVisualOnly(true);
                        player.serverLevel().addFreshEntity(bolt);
                    }
                }
            }
        }
        player.level().playSound(
                null,
                player,
                element == DragonElement.LIGHTNING ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE,
                1.4F,
                element == DragonElement.ICE ? 1.35F : 0.8F
        );
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 20 + stage * 2);
    }

    private static void petrifyingGaze(ServerPlayer player, double range, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || isBlindfolded(target) || target.hasEffect(MobEffects.BLINDNESS)) {
            player.displayClientMessage(Component.literal("The gaze needs an unprotected target looking your way."), true);
            return;
        }
        Vec3 toPlayer = player.getEyePosition().subtract(target.getEyePosition()).normalize();
        if (target.getViewVector(1.0F).dot(toPlayer) < 0.25D) {
            player.displayClientMessage(Component.literal("Petrification requires mutual line of sight."), true);
            return;
        }
        int stoneTime = Math.max(40, duration);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stoneTime, 9));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, stoneTime, 9));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stoneTime, 4));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, stoneTime, 2));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        player.serverLevel().sendParticles(ParticleTypes.ASH, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 45, target.getBbWidth(), target.getBbHeight() * 0.5D, target.getBbWidth(), 0.01D);
        player.level().playSound(null, target, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.5F, 0.55F);
    }

    private static void pixieBlessing(ServerPlayer player, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0));
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getEyeY(), player.getZ(), 32, 0.7D, 0.8D, 0.7D, 0.03D);
        player.level().playSound(null, player, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 1.0F, 1.35F);
    }

    private static void slam(ServerPlayer player, double strength, double range) {
        for (LivingEntity target : nearbyTargets(player, range)) {
            hurt(player, target, Math.max(2.0D, strength));
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                target.push(away.x * Math.min(2.0D, strength * 0.18D), Math.min(0.9D, 0.25D + strength * 0.06D), away.z * Math.min(2.0D, strength * 0.18D));
            }
        }
        player.serverLevel().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.2D, player.getZ(), 45, range * 0.45D, 0.15D, range * 0.45D, 0.08D);
        player.level().playSound(null, player, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1F, 0.65F);
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 18);
    }

    private static void sirenSong(ServerPlayer player, double strength, double range, int duration) {
        for (LivingEntity target : nearbyTargets(player, range)) {
            if (isHearingProtected(target) || target.isShiftKeyDown()) {
                continue;
            }
            Vec3 pull = player.position().subtract(target.position());
            if (pull.lengthSqr() > 1.0E-6D) {
                pull = pull.normalize().scale(Math.max(0.12D, Math.min(0.65D, strength)));
                target.setDeltaMovement(target.getDeltaMovement().add(pull.x, Math.max(-0.05D, pull.y * 0.15D), pull.z));
                target.hurtMarked = true;
            }
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0));
        }
        player.serverLevel().sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY(), player.getZ(), 36, range * 0.25D, 0.7D, range * 0.25D, 0.0D);
        player.level().playSound(null, player, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 1.8F, 0.75F);
    }

    private static void deathwormBurrowBurst(ServerPlayer player, double strength, double range, int duration) {
        escape(player, duration);
        addVelocity(player, new Vec3(0.0D, Math.max(0.65D, strength * 0.12D), 0.0D));
        slam(player, strength, range);
    }

    private static void cockatriceGaze(ServerPlayer player, double strength, double range, int duration) {
        LivingEntity target = findTarget(player, range);
        if (target == null || isBlindfolded(target) || target.hasEffect(MobEffects.BLINDNESS)) {
            return;
        }
        hurt(player, target, Math.max(1.0D, strength));
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        spawnParticleLine(player.serverLevel(), ParticleTypes.WITCH, player.getEyePosition(), target.getEyePosition());
    }

    private static void featherVolley(ServerPlayer player, double strength, double range) {
        List<LivingEntity> targets = coneTargets(player, range, 0.86D, 5);
        Vec3 start = player.getEyePosition();
        if (targets.isEmpty()) {
            spawnParticleLine(player.serverLevel(), ParticleTypes.CRIT, start, start.add(player.getViewVector(1.0F).scale(range)));
        }
        for (LivingEntity target : targets) {
            spawnParticleLine(player.serverLevel(), ParticleTypes.CRIT, start, target.getEyePosition());
            hurt(player, target, strength);
        }
        player.level().playSound(null, player, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.2F, 1.35F);
    }

    private static void sentinelAmbush(ServerPlayer player, double strength, double range, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.min(duration, 60), 1, true, false, true));
        pounce(player, Math.max(0.7D, strength * 0.12D), range, false);
    }

    private static void seaSerpentBubbles(ServerPlayer player, double strength, double range) {
        LivingEntity target = findTarget(player, range);
        Vec3 start = player.getEyePosition();
        Vec3 end = target == null ? start.add(player.getViewVector(1.0F).scale(range)) : target.getEyePosition();
        spawnParticleLine(player.serverLevel(), ParticleTypes.BUBBLE, start, end);
        if (target != null) {
            hurt(player, target, strength);
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                target.push(away.x * 0.8D, 0.25D, away.z * 0.8D);
            }
        }
    }

    private static void dreadSummon(ServerPlayer player, double strength, double range, int duration) {
        if (!IdentitySettings.enableModdedMorphSummons) {
            rangedStrike(player, Math.max(2.0D, strength * 4.0D), range, MobEffects.WITHER, Math.max(40, duration), ParticleTypes.SOUL);
            player.displayClientMessage(Component.literal("Dread summons are disabled; you fire a lich skull instead."), true);
            return;
        }
        ResourceLocation thrallId = new ResourceLocation("iceandfire", "dread_thrall");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.containsKey(thrallId)
                ? BuiltInRegistries.ENTITY_TYPE.get(thrallId)
                : null;
        Map<UUID, Long> owned = DREAD_SUMMONS.computeIfAbsent(
                player.getUUID(), ignored -> new ConcurrentHashMap<>()
        );
        long now = player.serverLevel().getGameTime();
        owned.entrySet().removeIf(entry -> entry.getValue() <= now);
        long existing = owned.size();
        if (type == null || existing >= 3) {
            rangedStrike(player, Math.max(2.0D, strength * 4.0D), range, MobEffects.WITHER, Math.max(40, duration), ParticleTypes.SOUL);
            return;
        }
        Entity spawned = type.create(player.serverLevel());
        if (!(spawned instanceof Mob minion)) {
            return;
        }
        Vec3 position = player.position().add(player.getLookAngle().multiply(2.0D, 0.0D, 2.0D));
        minion.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        minion.addTag(DREAD_SUMMON_TAG);
        minion.addTag(DREAD_SUMMON_TAG_PREFIX + player.getUUID());
        bindTemporarySummon(player, minion);
        long expiresAt = player.serverLevel().getGameTime() + Math.max(600, duration);
        ((EntityAccessor) minion).getCustomData().putLong(DREAD_SUMMON_EXPIRY_KEY, expiresAt);
        if (!player.serverLevel().addFreshEntity(minion)) {
            owned.remove(minion.getUUID());
            return;
        }
        owned.put(minion.getUUID(), expiresAt);
        player.serverLevel().sendParticles(ParticleTypes.SOUL, position.x, position.y + 1.0D, position.z, 30, 0.5D, 0.8D, 0.5D, 0.04D);
    }

    private static Entity findServerEntity(ServerPlayer owner, UUID entityUuid) {
        if (owner == null || entityUuid == null) {
            return null;
        }
        for (ServerLevel level : owner.server.getAllLevels()) {
            Entity entity = level.getEntity(entityUuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void bindTemporarySummon(ServerPlayer owner, Mob minion) {
        Scoreboard scoreboard = owner.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(owner.getScoreboardName());
        if (team == null) {
            String compact = owner.getUUID().toString().replace("-", "");
            String teamName = "id2" + compact.substring(0, Math.min(12, compact.length()));
            team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
            }
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        }
        scoreboard.addPlayerToTeam(minion.getScoreboardName(), team);
    }

    private static void removeFromOwnerTeam(Entity entity) {
        if (entity == null || !(entity.getTeam() instanceof PlayerTeam team)) {
            return;
        }
        entity.level().getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
    }

    private static void hydraVenomVolley(ServerPlayer player, double strength, double range, int duration) {
        List<LivingEntity> targets = coneTargets(player, range, 0.82D, 3);
        Vec3 start = player.getEyePosition();
        for (LivingEntity target : targets) {
            spawnParticleLine(player.serverLevel(), ParticleTypes.ITEM_SLIME, start, target.getEyePosition());
            hurt(player, target, strength);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1));
        }
        if (targets.isEmpty()) {
            spawnParticleLine(player.serverLevel(), ParticleTypes.ITEM_SLIME, start, start.add(player.getViewVector(1.0F).scale(range)));
        }
    }

    private static void ghostPhaseCharge(ServerPlayer player, double strength, double range, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.min(60, duration), 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.min(40, duration), 2, true, false, true));
        charge(player, Math.max(0.9D, strength * 0.16D), range, false);
        player.serverLevel().sendParticles(ParticleTypes.SOUL, player.getX(), player.getEyeY(), player.getZ(), 28, 0.6D, 0.8D, 0.6D, 0.04D);
    }

    private static void safeBlast(ServerPlayer player, double strength, double range) {
        for (LivingEntity target : nearbyTargetsThroughWalls(player, range)) {
            hurt(player, target, strength);
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                target.push(away.x * 1.5D, 0.55D, away.z * 1.5D);
            }
        }
        player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.8D, player.getZ(), 16, range * 0.35D, range * 0.25D, range * 0.35D, 0.0D);
        player.level().playSound(null, player, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.8F);
        PredefIdentityAbilities.triggerMorphAttackAnimation(player, 20);
    }

    private static void radiationAbsorb(ServerPlayer player, double strength, double range, int duration) {
        ResourceLocation irradiatedId = new ResourceLocation("alexscaves", "irradiated");
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(irradiatedId)) {
            player.displayClientMessage(Component.literal("No Irradiated effect is available to absorb."), true);
            return;
        }
        MobEffect irradiated = BuiltInRegistries.MOB_EFFECT.get(irradiatedId);
        LivingEntity source = null;
        for (LivingEntity candidate : nearbyTargetsThroughWalls(player, range)) {
            if (candidate.hasEffect(irradiated)
                    && (source == null || player.distanceToSqr(candidate) < player.distanceToSqr(source))) {
                source = candidate;
            }
        }
        if (source == null) {
            player.displayClientMessage(Component.literal("No nearby Irradiated creature was found."), true);
            return;
        }
        MobEffectInstance old = source.getEffect(irradiated);
        source.removeEffect(irradiated);
        if (old != null && old.getAmplifier() > 0) {
            source.addEffect(new MobEffectInstance(
                    irradiated,
                    old.getDuration(),
                    old.getAmplifier() - 1,
                    old.isAmbient(),
                    old.isVisible(),
                    old.showIcon()
            ));
        }
        player.heal((float) Math.max(1.0D, strength));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.max(40, duration), 0));
        spawnParticleLine(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, source.getEyePosition(), player.getEyePosition());
    }

    private static void stealTaggedItem(ServerPlayer player, double range) {
        if (!IdentitySettings.enableModdedMorphInventoryInteractions) {
            player.displayClientMessage(Component.literal("Morph inventory-interaction abilities are disabled by the server."), true);
            return;
        }
        LivingEntity lookedAt = findTarget(player, range);
        if (!(lookedAt instanceof ServerPlayer victim) || victim.isCreative() || !canAffect(player, victim)) {
            player.displayClientMessage(Component.literal("Look at an eligible player carrying stealable candy."), true);
            return;
        }
        TagKey<Item> steals = TagKey.create(Registries.ITEM, new ResourceLocation("alexscaves", "gingerbread_man_steals"));
        for (int slot = 0; slot < victim.getInventory().getContainerSize(); slot++) {
            ItemStack stack = victim.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.is(steals)) {
                continue;
            }
            ItemStack stolen = victim.getInventory().removeItem(slot, 1);
            if (!player.getInventory().add(stolen)) {
                player.drop(stolen, false);
            }
            victim.getInventory().setChanged();
            player.level().playSound(null, player, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);
            return;
        }
        player.displayClientMessage(Component.literal("That player has no stealable candy."), true);
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

    private static int currentDragonStage(ServerPlayer player) {
        Entity morph = IdentityApi.getCurrentMorph(player);
        if (morph == null) {
            return 1;
        }
        int ageDays = Math.max(0, EntityNbtIoCompat.saveWithoutId(morph).getInt("AgeTicks")) / 24_000;
        if (ageDays >= 100) return 5;
        if (ageDays >= 75) return 4;
        if (ageDays >= 50) return 3;
        if (ageDays >= 25) return 2;
        return 1;
    }

    private static List<LivingEntity> coneTargets(
            ServerPlayer player,
            double range,
            double minimumDot,
            int limit
    ) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> candidates = new ArrayList<>(nearbyTargets(player, range));
        candidates.removeIf(target -> {
            Vec3 direction = target.getEyePosition().subtract(eye);
            return direction.lengthSqr() < 1.0E-6D || direction.normalize().dot(look) < minimumDot;
        });
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (candidates.size() > limit) {
            return List.copyOf(candidates.subList(0, limit));
        }
        return List.copyOf(candidates);
    }

    private static boolean isBlindfolded(LivingEntity target) {
        ItemStack head = target.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(head.getItem());
        return itemId != null && "iceandfire".equals(itemId.getNamespace()) && "blindfold".equals(itemId.getPath());
    }

    private static boolean isHearingProtected(LivingEntity target) {
        ItemStack head = target.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(head.getItem());
        return itemId != null && "iceandfire".equals(itemId.getNamespace()) && "earplugs".equals(itemId.getPath())
                || head.getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains("earmuff");
    }

    private static void hurtDragon(ServerPlayer player, LivingEntity target, double requestedDamage) {
        float damage = (float) Mth.clamp(
                requestedDamage,
                0.0D,
                Math.max(0.0D, IdentitySettings.moddedDragonBreathDamageCap)
        );
        if (damage > 0.0F && canAffect(player, target)) {
            target.hurt(player.damageSources().playerAttack(player), damage);
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

    private enum DragonElement {
        FIRE,
        ICE,
        LIGHTNING
    }
}
