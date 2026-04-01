package net.Gabou.identity2;

import java.util.*;
import java.util.function.Predicate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import dev.architectury.platform.Platform;
import net.Gabou.identity2.util.NetworkCompat;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.ShulkerEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.monster.Shulker;
public final class PredefIdentityAbilities {
    private static final float GENERIC_MIN_DAMAGE = 2.0F;
    private static final float GENERIC_MAX_DAMAGE = 10.0F;
    private static final double GENERIC_STRIKE_RANGE = 4.5D;
    private static final double GENERIC_DASH_STRENGTH = 0.75D;
    private static final double GENERIC_DASH_UP = 0.18D;
    public static final String SHULKER_OPEN_STATE_KEY = "identity2.shulker_open";
    private static final int ILLUSIONER_CLONE_COUNT = 3;
    private static final int ILLUSIONER_CLONE_LIFETIME_TICKS = 20 * 25;
    private static final double ILLUSIONER_CLONE_RADIUS = 3.5D;
    private static final double ILLUSIONER_SWAP_RANGE = 28.0D;
    private static final String ILLUSIONER_CLONE_TAG = "identity2.illusioner_clone";
    private static final String ILLUSIONER_OWNER_TAG_PREFIX = "identity2.illusioner_owner:";


    @Deprecated
    public abstract static class IdentityAbility implements BuiltinIdentityAbility {
        @Override
        public void execute(Entity player) {
        }

        @Override
        public void executeSecondary(Entity player) {
        }

        @Override
        public void tick(Entity player, int cooldown) {
        }

        @Override
        public void passiveTick(Entity player, boolean used) {
            passivetick(player, used);
        }

        public void passivetick(Entity player, boolean used) {
        }

        @Override
        public boolean overrideAttack(Entity player) {
            return false;
        }
    }

    public static final Map<ResourceLocation, BuiltinIdentityAbility> predef = create();
    private static final BuiltinIdentityAbility genericMobAbility = createGenericMobAbility();
    private static final Map<UUID, List<IllusionerCloneRef>> illusionerCloneRefs = new HashMap<>();

    private record IllusionerCloneRef(UUID cloneUuid, long expiresAt, Vec3 offset) {
    }

    private PredefIdentityAbilities() {
    }

    public static boolean hasFallbackAbility(ResourceLocation identityTypeId) {
        if (identityTypeId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identityTypeId)) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(identityTypeId);
        if (type == null || type == EntityType.PLAYER) {
            return false;
        }
        return IdentityProgression.isMorphableType(type);
    }

    public static BuiltinIdentityAbility resolveFallbackAbility(ResourceLocation identityTypeId) {
        if (!hasFallbackAbility(identityTypeId)) {
            return null;
        }
        return genericMobAbility;
    }

    public static void register(ResourceLocation id, BuiltinIdentityAbility ability) {
        if (id == null) {
            throw new IllegalArgumentException("Ability id cannot be null.");
        }
        if (ability == null) {
            throw new IllegalArgumentException("Ability cannot be null.");
        }
        predef.put(id, ability);
    }

    public static void register(EntityType<?> type, BuiltinIdentityAbility ability) {
        if (type == null) {
            throw new IllegalArgumentException("Entity type cannot be null.");
        }
        ResourceLocation id = EntityType.getKey(type);
        if (id == null) {
            throw new IllegalArgumentException("Entity type is not registered: " + type);
        }
        register(id, ability);
    }

    private static Map<ResourceLocation, BuiltinIdentityAbility> create() {
        Map<ResourceLocation, BuiltinIdentityAbility> map = new HashMap<>();

        map.put(new ResourceLocation("ghast"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }
                Level world = player.level();
                HitResult target = player.pick(128.0D, 0.0F, false);
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(1.6D));
                Vec3 targetPos = target != null ? target.getLocation() : spawnPos.add(look.scale(48.0D));
                Vec3 direction = targetPos.subtract(spawnPos);
                if (direction.lengthSqr() < 1.0E-6D) {
                    direction = look;
                }
                if (!player.isSilent()) {
                    world.levelEvent(null, LevelEvent.SOUND_GHAST_FIREBALL, player.blockPosition(), 0);
                }
                Vec3 normalized = direction.normalize();
                LargeFireball fireball = new LargeFireball(world, livingPlayer, normalized.x, normalized.y, normalized.z, 1);
                fireball.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                world.addFreshEntity(fireball);
                if (((EntityAccessor) player).getCurrentIdentity() instanceof Ghast ghastIdentity) {
                    //ghastIdentity.setCharging(false);
                }
            }

            @Override
            public void tick(Entity player, int cooldown) {
                Level world = player.level();

                if (cooldown == 10 && !player.isSilent()) {
                    world.levelEvent(null, LevelEvent.SOUND_GHAST_WARNING, player.blockPosition(), 0);
                }
                if (cooldown > 10 && ((EntityAccessor) player).getCurrentIdentity() instanceof Ghast ghastIdentity) {
                    ghastIdentity.setCharging(true);
                }
                if (cooldown <= 1 && ((EntityAccessor) player).getCurrentIdentity() instanceof Ghast ghastIdentity) {
                    ghastIdentity.setCharging(false);
                }
            }
        });



        map.put(new ResourceLocation("enderman"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                double maxDistance = IdentitySettings.endermanAbilityTeleportDistance;
                HitResult hit = player.pick(maxDistance, 0, true);
                Vec3 targetPos = hit.getLocation();
                BlockPos blockPos = BlockPos.containing(targetPos);

                while (!isSafeTeleportSpot(world, blockPos) && ((int) targetPos.y >= world.getMinBuildHeight() && (int) targetPos.y <= world.getMaxBuildHeight())) {
                    blockPos = blockPos.above();
                }

                Vec3 safePos = Vec3.atCenterOf(blockPos);
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
                );
                player.teleportTo(safePos.x, safePos.y, safePos.z);
                world.playSound(
                    null,
                    safePos.x,
                    safePos.y,
                    safePos.z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
                );
            }

            private boolean isSafeTeleportSpot(Level world, BlockPos pos) {
                return world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir();
            }
        });

        map.put(new ResourceLocation("shulker"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return;
                }
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }
                boolean open = !isShulkerOpen(player);
                setShulkerOpenState(serverPlayer, open);
                ((ShulkerEntityAccessor) shulker).setPeekAmount(open ? 100 : 0);
                if (open) {
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(0.0D, motion.y, 0.0D);
                }
            }

            @Override
            public void executeSecondary(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return;
                }
                tryTeleportShulkerToNewAnchor(player, shulker);
            }

            @Override
            public boolean overrideAttack(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return false;
                }
                if (!isShulkerOpen(player)) {
                    return false;
                }
                return tryShootShulkerBullet(player, shulker);
            }
            @Override
            public void passivetick(Entity player,boolean usedLastTick){
                Identity2.LOGGER.info("Passive Tick");
                Shulker shulker=(Shulker)((EntityAccessor)player).getCurrentIdentity();
                if(usedLastTick==false){
                    if(((ShulkerEntityAccessor)shulker).runGetPeekAmount()!=0){
                        ((ShulkerEntityAccessor)shulker).setPeekAmount(0);
                    }
                }
                if (!shulker.level().isClientSide() && !shulker.isPassenger() && !canStay(shulker.blockPosition(), shulker.getAttachFace(),shulker)) {
                    BlockPos pos=shulker.blockPosition();
                    ((ShulkerEntityAccessor)shulker).runTryAttachOrTeleport();
                    if((pos==shulker.blockPosition())==false){
                        player.teleportTo(shulker.position().x(),shulker.position().y(),shulker.position().z());
                        
                    }
                }
            }
            boolean canStay(BlockPos pos, Direction direction,Shulker entity) {
                if (isInvalidPosition(pos,entity)) {
                    return false;
                } else {
                    Direction direction2 = direction.getOpposite();
                    if (!entity.level().loadedAndEntityCanStandOnFace(pos.offset(direction.getNormal()), entity, direction2)) {
                        return false;
                    } else {
                        AABB box = Shulker.getProgressAabb(direction2, 1.0F).deflate(1.0E-6);
                        return entity.level().noCollision(entity, box);
                    }
                }
            }

            private boolean isInvalidPosition(BlockPos pos,Shulker entity) {
                BlockState blockState = entity.level().getBlockState(pos);
                if (blockState.isAir()) {
                    return false;
                } else {
                    boolean bl = blockState.is(Blocks.MOVING_PISTON) && pos.equals(entity.blockPosition());
                    return !bl;
                }
            }
        });

        map.put(new ResourceLocation("blaze"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(0.6));
                SmallFireball smallFireball = new SmallFireball(world, (LivingEntity) player, look.x, look.y, look.z);
                smallFireball.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                world.addFreshEntity(smallFireball);
                world.playSound(
                    null,
                    player,
                    SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE,
                    2.0F,
                    (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F
                );
            }
        });

        map.put(new ResourceLocation("cow"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (player instanceof LivingEntity living) {
                    living.removeAllEffects();
                }
                player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
                );
            }
        });

        map.put(new ResourceLocation("villager"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                openVillagerTrade(player);
            }
        });

        map.put(new ResourceLocation("wandering_trader"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                openVillagerTrade(player);
            }
        });

        map.put(new ResourceLocation("creeper"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                float power = 3.0F;
                Entity current = ((EntityAccessor) player).getCurrentIdentity();
                if (current instanceof Creeper creeper && creeper.isPowered()) {
                    power = 6.0F;
                }
                player.level().explode(player, player.getX(), player.getY(), player.getZ(), power, Level.ExplosionInteraction.NONE);
            }
        });

        map.put(new ResourceLocation("alexscaves", "nucleeper"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                if (world == null || world.isClientSide()) {
                    return;
                }

                Entity current = ((EntityAccessor) player).getCurrentIdentity();
                boolean charged = identity2$isAlexsCavesNucleeperCharged(current);

                Entity explosionBase = identity2$createAlexsCavesEntity(
                    "com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry",
                    "NUCLEAR_EXPLOSION",
                    world
                );
                if (explosionBase == null) {
                    return;
                }

                explosionBase.copyPosition(player);
                identity2$invokeOneArgLoose(explosionBase, "setSize", charged ? 1.75F : 1.0F);
                if (!world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
                    identity2$invokeOneArgLoose(explosionBase, "setNoGriefing", true);
                }
                world.addFreshEntity(explosionBase);
            }
        });

        map.put(new ResourceLocation("endermite"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                double startX = player.getX();
                double startY = player.getY();
                double startZ = player.getZ();

                for (int i = 0; i < 16; ++i) {
                    double targetX = startX + (player.level().random.nextDouble() - 0.5D) * 16.0D;
                    int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(targetX), Mth.floor(startZ));
                    double targetY = Mth.clamp(startY + (player.level().random.nextInt(16) - 8), world.getMinBuildHeight(), topY - 1);
                    double targetZ = startZ + (player.level().random.nextDouble() - 0.5D) * 16.0D;
                    if (player.isPassenger()) {
                        player.stopRiding();
                    }
                    player.teleportTo(targetX, targetY, targetZ);
                    world.playSound(null, startX, startY, startZ, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                    break;
                }
            }
        });

        map.put(new ResourceLocation("evoker"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Vec3 origin = player.position();
                Vec3 facing = player.getViewVector(1.0F).multiply(1, 0, 1);
                Level world = player.level();

                for (int blockOut = 0; blockOut < 8; blockOut++) {
                    origin = origin.add(facing);
                    EvokerFangs fangs = new EvokerFangs(world, origin.x, origin.y, origin.z, player.getYRot(), blockOut * 2, (LivingEntity) player);

                    BlockPos pos = BlockPos.containing(origin);
                    BlockPos below = pos.below();
                    if (world.getBlockState(below).isFaceSturdy(world, below, Direction.UP) && world.isEmptyBlock(pos)) {
                        world.addFreshEntity(fangs);
                        continue;
                    }

                    BlockPos below2 = pos.below(2);
                    if (world.getBlockState(below2).isFaceSturdy(world, below2, Direction.UP) && world.isEmptyBlock(below2.above())) {
                        fangs.setPosRaw(fangs.getX(), fangs.getY() - 1, fangs.getZ());
                        world.addFreshEntity(fangs);
                        origin = origin.add(0, -1, 0);
                        continue;
                    }

                    BlockPos up = pos.above();
                    if (world.getBlockState(pos).isFaceSturdy(world, up, Direction.UP) && world.isEmptyBlock(up)) {
                        fangs.setPosRaw(fangs.getX(), fangs.getY() + 1, fangs.getZ());
                        world.addFreshEntity(fangs);
                        origin = origin.add(0, 1, 0);
                        continue;
                    }
                    break;
                }
            }
        });

        map.put(new ResourceLocation("illusioner"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                summonIllusionerClones(player);
            }

            @Override
            public void executeSecondary(Entity player) {
                if (!swapWithIllusionerClone(player)) {
                    summonIllusionerClones(player);
                    swapWithIllusionerClone(player);
                }
            }

            @Override
            public void passivetick(Entity player, boolean used) {
                if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide()) {
                    return;
                }
                syncIllusionerClones(serverPlayer);
                if ((serverPlayer.tickCount & 7) != 0) {
                    return;
                }
                collectIllusionerClones(serverPlayer, true);
            }
        });

        map.put(new ResourceLocation("guardian"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeGuardianLaser(player);
            }
        });

        map.put(new ResourceLocation("elder_guardian"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeGuardianLaser(player);
            }

            @Override
            public void executeSecondary(Entity player) {
                if (!(player.level() instanceof ServerLevel serverLevel)) {
                    return;
                }
                double radius = Math.max(1.0D, IdentitySettings.elderGuardianMiningFatigueRadius);
                int duration = Math.max(20, IdentitySettings.elderGuardianMiningFatigueDurationTicks);
                int amplifier = Math.max(0, IdentitySettings.elderGuardianMiningFatigueAmplifier);
                List<Player> targets = serverLevel.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius), target -> target != player);
                for (Player target : targets) {
                    if (target.isSpectator()) {
                        continue;
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, amplifier, false, true, true));
                }
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        });

        map.put(new ResourceLocation("warden"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeWardenSonicBoom(player);
            }
        });

        map.put(new ResourceLocation("ravager"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeRavagerRoar(player);
            }

            @Override
            public void executeSecondary(Entity player) {
                executeRavagerRoar(player);
            }
        });

        map.put(new ResourceLocation("breeze"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                executeBreezeWindProjectile(player);
            }
        });
        
        map.put(new ResourceLocation("iron_golem"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }
                Level world = player.level();
                EntityHitResult hit = findLivingTarget(player, 6.0D);
                if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 0.7F, 0.7F);
                    return;
                }

                float damage = 14.0F;
                target.hurt(player.damageSources().mobAttack(livingPlayer), damage);
                Vec3 look = player.getViewVector(1.0F).normalize();
                target.push(look.x * 1.6D, 0.65D, look.z * 1.6D);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.85F);
            }
        });

        map.put(new ResourceLocation("llama"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                LlamaSpit spit = new LlamaSpit(EntityType.LLAMA_SPIT, world);
                spit.setOwner(livingPlayer);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(1.0));
                spit.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                spit.shoot(look.x, look.y, look.z, 1.5F, 10.0F);
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.LLAMA_SPIT,
                    player.getSoundSource(),
                    1.0F,
                    1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F
                );
                world.addFreshEntity(spit);
            }
        });

        map.put(new ResourceLocation("snow_golem"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.SNOWBALL_THROW,
                    SoundSource.NEUTRAL,
                    0.5F,
                    0.4F / (world.random.nextFloat() * 0.4F + 0.8F)
                );
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(0.8));
                for (int i = 0; i < 10; i++) {
                    Snowball snowball = new Snowball(EntityType.SNOWBALL, world);
                    snowball.setOwner(player);
                    snowball.setItem(new ItemStack(Items.SNOWBALL));
                    float pitchOffset = (float) (player.getXRot() + world.random.nextGaussian() * 5.0);
                    float yawOffset = (float) (player.getYRot() + world.random.nextGaussian() * 5.0);
                    snowball.shootFromRotation((LivingEntity) player, pitchOffset, yawOffset, 0.0F, 1.5F, 1.0F);
                    snowball.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, yawOffset, pitchOffset);
                    world.addFreshEntity(snowball);
                }
            }
        });

        map.put(new ResourceLocation("witch"), new IdentityAbility() {
            private final List<Potion> validPotions = List.of(Potions.HARMING, Potions.POISON, Potions.SLOWNESS, Potions.WEAKNESS);

            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }

                Level world = player.level();

                ThrownPotion potionEntity = new ThrownPotion(EntityType.POTION, world);
                potionEntity.setOwner(livingPlayer);

                Potion potion = validPotions.get(world.random.nextInt(validPotions.size()));

                ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
                potionStack = PotionUtils.setPotion(potionStack, potion);

                potionEntity.setItem(potionStack);

                potionEntity.setXRot(-20.0F);

                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(0.8D));

                potionEntity.moveTo(
                        spawnPos.x,
                        spawnPos.y,
                        spawnPos.z,
                        player.getYRot(),
                        player.getXRot()
                );

                potionEntity.shoot(
                        look.x(),
                        look.y(),
                        look.z(),
                        0.75F,
                        8.0F
                );

                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.WITCH_THROW,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.8F + world.getRandom().nextFloat() * 0.4F
                );

                world.addFreshEntity(potionEntity);
            }
        });

        map.put(new ResourceLocation("wither"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.WITHER_SHOOT,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.8F + world.random.nextFloat() * 0.4F
                );
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(2.0));
                WitherSkull skull = new WitherSkull(world, (LivingEntity) player, look.x, look.y, look.z);
                skull.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                skull.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
                world.addFreshEntity(skull);
            }
        });

        map.put(new ResourceLocation("ender_dragon"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(1.4D));
                HitResult target = player.pick(96.0D, 0.0F, false);
                Vec3 targetPos = target != null ? target.getLocation() : spawnPos.add(look.scale(48.0D));
                Vec3 direction = targetPos.subtract(spawnPos);
                if (direction.lengthSqr() < 1.0E-6D) {
                    direction = look;
                }

                Vec3 normalized = direction.normalize();
                DragonFireball fireball = new DragonFireball(world, livingPlayer, normalized.x, normalized.y, normalized.z);
                fireball.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                world.addFreshEntity(fireball);
                world.playSound(null, player, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3.0F, 1.0F);
            }
        });

        registerNaturalistAbilities(map);
        registerAlexsMobsAbilities(map);

        return map;
    }

    private static void registerNaturalistAbilities(Map<ResourceLocation, BuiltinIdentityAbility> map) {
        map.put(
            new ResourceLocation("naturalist", "bear"),
            simpleAbility(player -> {
                if (player instanceof LivingEntity livingPlayer) {
                    livingPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                }
            })
        );
    }

    private static void registerAlexsMobsAbilities(Map<ResourceLocation, BuiltinIdentityAbility> map) {
        map.put(new ResourceLocation("alexsmobs", "anaconda"), alexsMobsAbility(player -> constrictNearby(player, 3.0F)));
        map.put(new ResourceLocation("alexsmobs", "bald_eagle"), alexsMobsAbility(player -> dashForward(player, 1.2D)));
        map.put(
            new ResourceLocation("alexsmobs", "bone_serpent"),
            alexsMobsAbility(player -> {
                if (player.isInLava()) {
                    dashForward(player, 1.8D);
                }
            })
        );
        map.put(
            new ResourceLocation("alexsmobs", "cockroach"),
            alexsMobsAbility(player -> {
                if (player instanceof LivingEntity livingPlayer) {
                    livingPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                }
                player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.2F
                );
            })
        );
        map.put(new ResourceLocation("alexsmobs", "crimson_mosquito"), alexsMobsAbility(player -> damageAndLeechRaycastTarget(player, 2.5D, 2.0F, 1.0F)));
        map.put(new ResourceLocation("alexsmobs", "crocodile"), alexsMobsAbility(player -> pullRaycastTargetTowardPlayer(player, 5.0D, 1.5D)));
        map.put(new ResourceLocation("alexsmobs", "crow"), alexsMobsAbility(player -> dashUpward(player, 0.5D)));
        map.put(new ResourceLocation("alexsmobs", "dropbear"), alexsMobsAbility(player -> dashForward(player, 1.0D)));
        map.put(new ResourceLocation("alexsmobs", "elephant"), alexsMobsAbility(player -> knockbackNearbyEntities(player, 4.0F, 2.0D)));
        map.put(new ResourceLocation("alexsmobs", "emu"), alexsMobsAbility(player -> dashForward(player, 1.4D)));
        map.put(new ResourceLocation("alexsmobs", "enderiophage"), alexsMobsAbility(player -> shortTeleportForward(player, 5.0D)));
        map.put(new ResourceLocation("alexsmobs", "fly"), alexsMobsAbility(player -> dashForward(player, 0.4D)));
        map.put(new ResourceLocation("alexsmobs", "giant_squid"), alexsMobsAbility(player -> waterDash(player, 1.5D)));
        map.put(new ResourceLocation("alexsmobs", "gorilla"), alexsMobsAbility(player -> knockbackNearbyEntities(player, 3.0F, 1.5D)));
        map.put(new ResourceLocation("alexsmobs", "grizzly_bear"), alexsMobsAbility(player -> knockbackNearbyEntities(player, 3.0F, 1.0D)));
        map.put(new ResourceLocation("alexsmobs", "guster"), alexsMobsAbility(player -> knockbackNearbyEntities(player, 5.0F, 2.0D)));
        map.put(new ResourceLocation("alexsmobs", "hummingbird"), alexsMobsAbility(player -> dashUpward(player, 0.7D)));
        map.put(
            new ResourceLocation("alexsmobs", "kangaroo"),
            alexsMobsAbility(player -> {
                dashForward(player, 1.2D);
                dashUpward(player, 0.5D);
            })
        );
        map.put(new ResourceLocation("alexsmobs", "komodo_dragon"), alexsMobsAbility(player -> poisonNearbyEnemies(player, 3.0D, 100, 0)));
        map.put(new ResourceLocation("alexsmobs", "mimicube"), alexsMobsAbility(PredefIdentityAbilities::randomMorphNearby));
        map.put(
            new ResourceLocation("alexsmobs", "moose"),
            alexsMobsAbility(player -> {
                dashForward(player, 1.4D);
                knockbackNearbyEntities(player, 2.5F, 1.2D);
            })
        );
        map.put(new ResourceLocation("alexsmobs", "orca"), alexsMobsAbility(player -> waterDash(player, 1.2D)));
        map.put(new ResourceLocation("alexsmobs", "raccoon"), alexsMobsAbility(PredefIdentityAbilities::dropRandomItemFromInventory));
        map.put(new ResourceLocation("alexsmobs", "rattlesnake"), alexsMobsAbility(player -> applyPoisonToRaycastTarget(player, 2.0D, 60, 0)));
        map.put(new ResourceLocation("alexsmobs", "roadrunner"), alexsMobsAbility(player -> dashForward(player, 1.8D)));
        map.put(new ResourceLocation("alexsmobs", "skunk"), alexsMobsAbility(player -> healNearbyPlayers(player, 2.5D, -2.0F)));
        map.put(
            new ResourceLocation("alexsmobs", "snow_leopard"),
            alexsMobsAbility(player -> {
                if (player.level().getBlockState(player.blockPosition().below()).is(Blocks.SNOW_BLOCK)) {
                    dashForward(player, 1.3D);
                }
            })
        );
        map.put(new ResourceLocation("alexsmobs", "soul_vulture"), alexsMobsAbility(player -> healNearbyPlayers(player, 4.0D, 4.0F)));
        map.put(new ResourceLocation("alexsmobs", "spectre"), alexsMobsAbility(player -> dashForward(player, 2.0D)));
        map.put(new ResourceLocation("alexsmobs", "sunbird"), alexsMobsAbility(player -> dashUpward(player, 2.0D)));
        map.put(new ResourceLocation("alexsmobs", "tarantula_hawk"), alexsMobsAbility(player -> applyPoisonToRaycastTarget(player, 3.0D, 100, 0)));
        map.put(new ResourceLocation("alexsmobs", "tasmanian_devil"), alexsMobsAbility(player -> dashForward(player, 1.8D)));
        map.put(new ResourceLocation("alexsmobs", "tiger"), alexsMobsAbility(player -> dashForward(player, 1.5D)));
        map.put(new ResourceLocation("alexsmobs", "void_worm"), alexsMobsAbility(player -> dashForward(player, 2.5D)));
        map.put(
            new ResourceLocation("alexsmobs", "warped_mosco"),
            alexsMobsAbility(player -> {
                dashForward(player, 2.5D);
                knockbackNearbyEntities(player, 3.0F, 2.5D);
            })
        );
    }

    private static BuiltinIdentityAbility simpleAbility(java.util.function.Consumer<Entity> action) {
        return new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (player == null || action == null) {
                    return;
                }
                action.accept(player);
            }
        };
    }

    private static BuiltinIdentityAbility alexsMobsAbility(java.util.function.Consumer<Entity> action) {
        return simpleAbility(player -> {
            if (!isAlexsMobsLoaded()) {
                return;
            }
            action.accept(player);
        });
    }

    private static boolean isAlexsMobsLoaded() {
        return Platform.isModLoaded("alexsmobs");
    }

    private static void dashForward(Entity player, double strength) {
        if (player == null) {
            return;
        }
        Vec3 look = player.getViewVector(1.0F);
        player.setDeltaMovement(
            player.getDeltaMovement().add(look.x * strength, Math.max(0.06D, strength * 0.08D), look.z * strength)
        );
        player.hurtMarked = true;
    }

    private static void dashUpward(Entity player, double strength) {
        if (player == null) {
            return;
        }
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, strength, 0.0D));
        player.hurtMarked = true;
    }

    private static void waterDash(Entity player, double strength) {
        if (player == null) {
            return;
        }
        if (player.isInWater()) {
            dashForward(player, strength);
            return;
        }
        dashForward(player, Math.max(0.25D, strength * 0.5D));
    }

    private static void shortTeleportForward(Entity player, double distance) {
        if (player == null) {
            return;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        if (look.lengthSqr() < 1.0E-6D) {
            return;
        }
        for (double step = distance; step >= 1.0D; step -= 1.0D) {
            Vec3 target = player.position().add(look.scale(step));
            if (!canTeleportTo(player, target)) {
                continue;
            }
            player.teleportTo(target.x, target.y, target.z);
            player.level().playSound(
                null,
                target.x,
                target.y,
                target.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
            );
            return;
        }
    }

    private static boolean canTeleportTo(Entity player, Vec3 target) {
        if (player == null || target == null) {
            return false;
        }
        Vec3 delta = target.subtract(player.position());
        AABB movedBox = player.getBoundingBox().move(delta);
        return player.level().noCollision(player, movedBox);
    }

    private static void constrictNearby(Entity player, double radius) {
        if (!(player instanceof LivingEntity livingPlayer)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            target.hurt(player.damageSources().mobAttack(livingPlayer), 2.0F);
        }
    }

    private static void knockbackNearbyEntities(Entity player, double radius, double strength) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive())) {
            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() < 1.0E-6D) {
                push = player.getViewVector(1.0F);
            }
            Vec3 normalized = push.normalize();
            target.push(normalized.x * strength, 0.2D + (strength * 0.08D), normalized.z * strength);
        }
    }

    private static void poisonNearbyEnemies(Entity player, double radius, int duration, int amplifier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(1, duration), amplifier));
        }
    }

    private static void healNearbyPlayers(Entity player, double radius, float amount) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);
        for (Player target : serverLevel.getEntitiesOfClass(Player.class, area, target -> target.isAlive())) {
            if (amount > 0.0F) {
                target.heal(amount);
            } else if (amount < 0.0F && player instanceof LivingEntity livingPlayer) {
                target.hurt(player.damageSources().mobAttack(livingPlayer), -amount);
            }
        }
    }

    private static void applyPoisonToRaycastTarget(Entity player, double range, int duration, int amplifier) {
        EntityHitResult hit = findLivingTarget(player, range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(1, duration), amplifier));
    }

    private static void pullRaycastTargetTowardPlayer(Entity player, double range, double strength) {
        EntityHitResult hit = findLivingTarget(player, range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Vec3 toPlayer = player.position().subtract(target.position());
        if (toPlayer.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 pull = toPlayer.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(pull.x, pull.y * 0.35D, pull.z));
        target.hurtMarked = true;
    }

    private static void damageAndLeechRaycastTarget(Entity player, double range, float damage, float healAmount) {
        if (!(player instanceof LivingEntity livingPlayer)) {
            return;
        }
        EntityHitResult hit = findLivingTarget(player, range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        target.hurt(player.damageSources().mobAttack(livingPlayer), damage);
        livingPlayer.heal(healAmount);
    }

    private static void dropRandomItemFromInventory(Entity player) {
        if (!(player instanceof Player user)) {
            return;
        }
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < user.getInventory().getContainerSize(); i++) {
            if (!user.getInventory().getItem(i).isEmpty()) {
                nonEmptySlots.add(i);
            }
        }
        if (nonEmptySlots.isEmpty()) {
            return;
        }
        int slot = nonEmptySlots.get(user.getRandom().nextInt(nonEmptySlots.size()));
        ItemStack dropped = user.getInventory().removeItem(slot, 1);
        if (dropped.isEmpty()) {
            return;
        }
        user.drop(dropped, true, false);
    }

    private static void randomMorphNearby(Entity player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            serverPlayer.getBoundingBox().inflate(8.0D),
            target -> target != serverPlayer && target.isAlive() && IdentityProgression.isMorphableType(target.getType())
        );
        if (candidates.isEmpty()) {
            return;
        }
        LivingEntity target = candidates.get(serverPlayer.getRandom().nextInt(candidates.size()));
        ResourceLocation targetId = EntityType.getKey(target.getType());
        if (targetId == null || !IdentityProgression.isMorphableIdentity(targetId)) {
            return;
        }
        IdentityProgression.morph(serverPlayer, targetId);
    }

    private static EntityHitResult findLivingTarget(Entity player, double range) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        return ProjectileUtil.getEntityHitResult(
            player,
            start,
            end,
            player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.2D),
            candidate -> candidate != player && candidate instanceof LivingEntity,
            range * range
        );
    }

    private static void executeGuardianLaser(Entity player) {
        if (!(player instanceof LivingEntity livingPlayer)) {
            return;
        }
        Level world = player.level();
        EntityHitResult hit = findLivingTarget(player, 30.0D);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity livingTarget)) {
            return;
        }
        renderGuardianBeam(world, player.getEyePosition(1.0F), livingTarget.getEyePosition(1.0F));
        livingTarget.hurt(player.damageSources().mobAttack(livingPlayer), 6.0F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static boolean isShulkerOpen(Entity player) {
        if (player == null) {
            return false;
        }
        CompoundTag customData = ((EntityAccessor) player).getCustomData();
        CompoundTag nbt = customData;
        return net.Gabou.identity2.util.NbtCompat.getBooleanOr(nbt, SHULKER_OPEN_STATE_KEY, false);
    }

    private static void setShulkerOpenState(ServerPlayer player, boolean open) {
        if (player == null) {
            return;
        }
        CompoundTag customData = ((EntityAccessor) player).getCustomData();
        customData.putBoolean(SHULKER_OPEN_STATE_KEY, open);
        syncBoolData(player, SHULKER_OPEN_STATE_KEY, open);
    }

    private static void syncBoolData(ServerPlayer player, String key, boolean value) {
        IdentityApi.syncBoolean(player, key, value);
    }

    private static boolean tryShootShulkerBullet(Entity player, Shulker shulker) {
        if (player == null || shulker == null || player.level().isClientSide() || !isShulkerOpen(player)) {
            return false;
        }
        if (((ShulkerEntityAccessor) shulker).runGetPeekAmount() == 0) {
            ((ShulkerEntityAccessor) shulker).setPeekAmount(100);
        }
        double range = 64.0D;
        double rangeSq = Mth.square(range);
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        HitResult target = ProjectileUtil.getEntityHitResult(player, start, end, box, candidate -> candidate.isPickable() && !candidate.isSpectator(), rangeSq);
        Entity targetEntity = null;
        if (target instanceof EntityHitResult entityTarget) {
            targetEntity = entityTarget.getEntity();
        } else {
            targetEntity = findNearestForwardLivingTarget(player, range);
        }
        if (!(targetEntity instanceof LivingEntity)) {
            return false;
        }
        ShulkerBullet bullet = new ShulkerBullet(shulker.level(), shulker, targetEntity, shulker.getAttachFace().getAxis());
        boolean spawned = shulker.level().addFreshEntity(bullet);
        if (spawned) {
            shulker.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 1.0F);
        }
        return spawned;
    }

    private static LivingEntity findNearestForwardLivingTarget(Entity player, double range) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        double bestDistance = Double.MAX_VALUE;
        LivingEntity best = null;
        for (LivingEntity candidate : serverLevel.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive() && entity.isPickable())) {
            Vec3 toCandidate = candidate.getEyePosition(1.0F).subtract(eye);
            double distanceSq = toCandidate.lengthSqr();
            if (distanceSq > (range * range) || distanceSq < 1.0E-6D) {
                continue;
            }
            double forward = toCandidate.normalize().dot(look);
            if (forward < 0.75D) {
                continue;
            }
            if (distanceSq < bestDistance) {
                bestDistance = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean tryTeleportShulkerToNewAnchor(Entity player, Shulker shulker) {
        if (player == null || shulker == null || shulker.level().isClientSide()) {
            return false;
        }
        if (player instanceof LivingEntity livingPlayer) {
            for (int i = 0; i < 16; i++) {
                double offsetX = (livingPlayer.getRandom().nextDouble() - 0.5D) * 24.0D;
                double offsetY = livingPlayer.getRandom().nextInt(13) - 6;
                double offsetZ = (livingPlayer.getRandom().nextDouble() - 0.5D) * 24.0D;
                if (livingPlayer.randomTeleport(livingPlayer.getX() + offsetX, livingPlayer.getY() + offsetY, livingPlayer.getZ() + offsetZ, true)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                    return true;
                }
            }
        }

        BlockPos previous = shulker.blockPosition();
        ((ShulkerEntityAccessor) shulker).runTryAttachOrTeleport();
        if (previous.equals(shulker.blockPosition())) {
            return false;
        }
        player.teleportTo(shulker.getX(), shulker.getY(), shulker.getZ());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        return true;
    }
    private static void summonIllusionerClones(Entity player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        discardAllOwnedIllusionerClones(serverPlayer);
        List<Illusioner> existing = collectIllusionerClones(serverPlayer, true);
        for (Illusioner clone : existing) {
            clone.discard();
        }

        long expiresAt = serverLevel.getGameTime() + ILLUSIONER_CLONE_LIFETIME_TICKS;
        List<IllusionerCloneRef> refs = new ArrayList<>(ILLUSIONER_CLONE_COUNT);
        String ownerTag = illusionerOwnerTag(serverPlayer.getUUID());

        for (int i = 0; i < ILLUSIONER_CLONE_COUNT; i++) {
            Illusioner clone = EntityType.ILLUSIONER.create(serverLevel);
            if (clone == null) {
                continue;
            }
            Vec3 spawn = computeIllusionerCloneSpawn(serverPlayer, i);
            clone.moveTo(spawn.x, spawn.y, spawn.z, serverPlayer.getYRot(), serverPlayer.getXRot());
            clone.setYHeadRot(serverPlayer.getYHeadRot());
            clone.setNoAi(true);
            clone.setNoGravity(true);
            clone.noPhysics = true;
            clone.setInvulnerable(true);
            clone.setSilent(true);
            clone.addTag(ILLUSIONER_CLONE_TAG);
            clone.addTag(ownerTag);
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, serverPlayer.getMainHandItem().copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, serverPlayer.getOffhandItem().copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy());
            if (!serverLevel.noCollision(clone)) {
                clone.discard();
                continue;
            }
            if (serverLevel.addFreshEntity(clone)) {
                refs.add(new IllusionerCloneRef(clone.getUUID(), expiresAt, spawn.subtract(serverPlayer.position())));
            }
        }

        if (refs.isEmpty()) {
            illusionerCloneRefs.remove(serverPlayer.getUUID());
            return;
        }

        illusionerCloneRefs.put(serverPlayer.getUUID(), refs);
        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(), 18, 0.45D, 0.4D, 0.45D, 0.0D);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void discardAllOwnedIllusionerClones(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return;
        }
        UUID ownerId = player.getUUID();
        illusionerCloneRefs.remove(ownerId);
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            AABB bounds = new AABB(-3.0E7D, level.getMinBuildHeight(), -3.0E7D, 3.0E7D, level.getMaxBuildHeight(), 3.0E7D);
            List<Illusioner> owned = level.getEntitiesOfClass(Illusioner.class, bounds, candidate -> isOwnedIllusionerClone(candidate, ownerId));
            for (Illusioner clone : owned) {
                clone.discard();
            }
        }
    }

    private static void syncIllusionerClones(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        UUID ownerId = player.getUUID();
        List<IllusionerCloneRef> refs = illusionerCloneRefs.get(ownerId);
        if (refs == null || refs.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        List<IllusionerCloneRef> keptRefs = new ArrayList<>(refs.size());
        Vec3 playerPos = player.position();
        Vec3 playerMotion = player.getDeltaMovement();

        for (IllusionerCloneRef ref : refs) {
            Entity entity = findEntityByUuid(player.level().getServer(), ref.cloneUuid());
            if (!(entity instanceof Illusioner clone)) {
                continue;
            }
            if (!clone.isAlive() || clone.level() != player.level() || now > ref.expiresAt() || !isOwnedIllusionerClone(clone, ownerId)) {
                continue;
            }

            Vec3 offset = ref.offset() == null ? Vec3.ZERO : ref.offset();
            Vec3 targetPos = playerPos.add(offset);
            clone.absMoveTo(targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());
            clone.setYHeadRot(player.getYHeadRot());
            clone.setDeltaMovement(playerMotion);
            clone.setShiftKeyDown(player.isShiftKeyDown());
            clone.setSprinting(player.isSprinting());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, player.getOffhandItem().copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy());
            keptRefs.add(ref);
        }

        if (keptRefs.isEmpty()) {
            illusionerCloneRefs.remove(ownerId);
        } else {
            illusionerCloneRefs.put(ownerId, keptRefs);
        }
    }

    private static boolean swapWithIllusionerClone(Entity player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel)) {
            return false;
        }
        List<Illusioner> clones = collectIllusionerClones(serverPlayer, true);
        if (clones.isEmpty()) {
            return false;
        }

        Illusioner target = findAimedIllusionerClone(serverPlayer, clones);
        if (target == null) {
            target = clones.get(serverPlayer.getRandom().nextInt(clones.size()));
        }
        performIllusionerSwap(serverPlayer, target);
        return true;
    }

    private static Illusioner findAimedIllusionerClone(ServerPlayer player, List<Illusioner> clones) {
        if (player == null || clones == null || clones.isEmpty()) {
            return null;
        }
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(look.scale(ILLUSIONER_SWAP_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(ILLUSIONER_SWAP_RANGE)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                box,
                candidate -> candidate instanceof Illusioner && isOwnedIllusionerClone(candidate, player.getUUID()),
                ILLUSIONER_SWAP_RANGE * ILLUSIONER_SWAP_RANGE
        );
        if (hit == null || !(hit.getEntity() instanceof Illusioner illusioner)) {
            return null;
        }
        return clones.contains(illusioner) ? illusioner : null;
    }

    private static List<Illusioner> collectIllusionerClones(ServerPlayer player, boolean discardInvalid) {
        if (player == null) {
            return List.of();
        }
        UUID ownerId = player.getUUID();
        List<IllusionerCloneRef> refs = illusionerCloneRefs.get(ownerId);
        if (refs == null || refs.isEmpty()) {
            illusionerCloneRefs.remove(ownerId);
            return List.of();
        }

        long now = player.level().getGameTime();
        List<Illusioner> clones = new ArrayList<>(refs.size());
        List<IllusionerCloneRef> keptRefs = new ArrayList<>(refs.size());
        for (IllusionerCloneRef ref : refs) {
            Entity entity = findEntityByUuid(player.level().getServer(), ref.cloneUuid());
            if (!(entity instanceof Illusioner clone)) {
                continue;
            }
            boolean valid = clone.isAlive()
                    && clone.level() == player.level()
                    && isOwnedIllusionerClone(clone, ownerId)
                    && now <= ref.expiresAt();
            if (!valid) {
                if (discardInvalid) {
                    clone.discard();
                }
                continue;
            }
            clones.add(clone);
            keptRefs.add(ref);
        }

        if (keptRefs.isEmpty()) {
            illusionerCloneRefs.remove(ownerId);
        } else {
            illusionerCloneRefs.put(ownerId, keptRefs);
        }
        return clones;
    }

    private static Entity findEntityByUuid(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isOwnedIllusionerClone(Entity entity, UUID ownerId) {
        if (entity == null || ownerId == null) {
            return false;
        }
        return entity.getTags().contains(ILLUSIONER_CLONE_TAG) && entity.getTags().contains(illusionerOwnerTag(ownerId));
    }

    private static String illusionerOwnerTag(UUID ownerId) {
        return ILLUSIONER_OWNER_TAG_PREFIX + ownerId;
    }

    private static Vec3 computeIllusionerCloneSpawn(ServerPlayer player, int index) {
        double angle = (Math.PI * 2.0D * index) / Math.max(1, ILLUSIONER_CLONE_COUNT);
        angle += (player.getRandom().nextDouble() - 0.5D) * 0.35D;
        double radius = ILLUSIONER_CLONE_RADIUS + player.getRandom().nextDouble() * 0.8D;
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        return new Vec3(x, player.getY(), z);
    }

    private static void performIllusionerSwap(ServerPlayer player, Illusioner clone) {
        if (player == null || clone == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 playerPos = player.position();
        Vec3 clonePos = clone.position();
        Vec3 playerMotion = player.getDeltaMovement();
        Vec3 cloneMotion = clone.getDeltaMovement();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();
        float playerHeadYaw = player.getYHeadRot();
        float cloneYaw = clone.getYRot();
        float clonePitch = clone.getXRot();
        float cloneHeadYaw = clone.getYHeadRot();

        player.teleportTo(clonePos.x, clonePos.y, clonePos.z);
        player.setYRot(cloneYaw);
        player.setXRot(clonePitch);
        player.setYHeadRot(cloneHeadYaw);
        player.setDeltaMovement(cloneMotion);

        clone.teleportTo(playerPos.x, playerPos.y, playerPos.z);
        clone.setYRot(playerYaw);
        clone.setXRot(playerPitch);
        clone.setYHeadRot(playerHeadYaw);
        clone.setDeltaMovement(playerMotion);
        recalculateIllusionerCloneOffsets(player);

        serverLevel.sendParticles(ParticleTypes.CLOUD, playerPos.x, playerPos.y + 1.0D, playerPos.z, 14, 0.35D, 0.35D, 0.35D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, clonePos.x, clonePos.y + 1.0D, clonePos.z, 14, 0.35D, 0.35D, 0.35D, 0.01D);
        serverLevel.playSound(null, playerPos.x, playerPos.y, playerPos.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverLevel.playSound(null, clonePos.x, clonePos.y, clonePos.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void recalculateIllusionerCloneOffsets(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID ownerId = player.getUUID();
        List<IllusionerCloneRef> refs = illusionerCloneRefs.get(ownerId);
        if (refs == null || refs.isEmpty()) {
            return;
        }
        List<IllusionerCloneRef> updatedRefs = new ArrayList<>(refs.size());
        Vec3 playerPos = player.position();
        for (IllusionerCloneRef ref : refs) {
            Entity entity = findEntityByUuid(player.level().getServer(), ref.cloneUuid());
            if (!(entity instanceof Illusioner clone) || !clone.isAlive() || clone.level() != player.level()) {
                continue;
            }
            updatedRefs.add(new IllusionerCloneRef(ref.cloneUuid(), ref.expiresAt(), clone.position().subtract(playerPos)));
        }
        if (updatedRefs.isEmpty()) {
            illusionerCloneRefs.remove(ownerId);
        } else {
            illusionerCloneRefs.put(ownerId, updatedRefs);
        }
    }


    private static void executeWardenSonicBoom(Entity player) {
        if (!(player instanceof LivingEntity livingPlayer)) {
            return;
        }
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = from.add(look.scale(15.0D));
        EntityHitResult hit = findLivingTarget(player, 15.0D);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            to = target.getEyePosition(1.0F);
            target.hurt(resolveSonicBoomDamageSource(player, livingPlayer), 10.0F);
            Vec3 pushDirection = to.subtract(from).normalize();
            target.push(pushDirection.x * 2.5D, 0.5D, pushDirection.z * 2.5D);
        }

        renderSonicBoom(player.level(), from, to);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
    }

    private static void executeBreezeWindProjectile(Entity player) {
        if (player.level().isClientSide()) {
            return;
        }
        boolean spawned = spawnWindProjectile(player, new ResourceLocation("minecraft", "breeze_wind_charge"));
        if (!spawned) {
            spawned = spawnWindProjectile(player, new ResourceLocation("minecraft", "wind_charge"));
        }
        if (!spawned) {
            EntityHitResult hit = findLivingTarget(player, 20.0D);
            if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                Vec3 look = player.getViewVector(1.0F).normalize();
                target.push(look.x * 1.5D, 0.45D, look.z * 1.5D);
                renderSonicBoom(player.level(), player.getEyePosition(1.0F), target.getEyePosition(1.0F));
            }
        }
        player.level().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            identity2$resolveBreezeShootSound(),
            SoundSource.HOSTILE,
            1.0F,
            1.0F
        );
    }

    private static SoundEvent identity2$resolveBreezeShootSound() {
        SoundEvent breezeShoot = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("minecraft", "breeze_shoot"));
        if (breezeShoot != null) {
            return breezeShoot;
        }
        return SoundEvents.BLAZE_SHOOT;
    }

    private static boolean spawnWindProjectile(Entity player, ResourceLocation projectileId) {
        if (player == null || projectileId == null) {
            return false;
        }
        EntityType<?> projectileType = BuiltInRegistries.ENTITY_TYPE.get(projectileId);
        if (projectileType == null) {
            return false;
        }
        Entity projectile = null;
        try {
            projectile = projectileType.create(player.level());
        } catch (Throwable ignored) {
        }
        if (!(projectile instanceof net.minecraft.world.entity.projectile.Projectile projectileEntity)) {
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 spawnPos = player.getEyePosition().add(look.scale(1.2D));
        projectileEntity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
        projectileEntity.setOwner(player);
        projectileEntity.shoot(look.x, look.y, look.z, 1.6F, 0.0F);
        return player.level().addFreshEntity(projectileEntity);
    }

    private static void renderGuardianBeam(Level world, Vec3 from, Vec3 to) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 1.0E-4D) {
            return;
        }
        Vec3 step = delta.normalize().scale(0.35D);
        int points = Mth.ceil(distance / 0.35D);
        Vec3 cursor = from;
        for (int i = 0; i <= points; i++) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, cursor.x, cursor.y, cursor.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            cursor = cursor.add(step);
        }
    }

    private static void renderSonicBoom(Level world, Vec3 from, Vec3 to) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 1.0E-4D) {
            return;
        }
        Vec3 step = delta.normalize().scale(0.5D);
        int points = Mth.ceil(distance / 0.5D);
        Vec3 cursor = from;
        for (int i = 0; i <= points; i++) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, cursor.x, cursor.y, cursor.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            cursor = cursor.add(step);
        }
    }

    private static void executeRavagerRoar(Entity player) {
        if (!(player instanceof LivingEntity livingPlayer)) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        boolean invokedVanillaRoar = identity != null && invokeNoArg(identity, "roar") != null;
        Level world = player.level();

        if (!invokedVanillaRoar) {
            List<LivingEntity> targets = world.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(4.0D),
                    target -> target != player && !target.isAlliedTo(player)
            );
            for (LivingEntity target : targets) {
                Vec3 delta = target.position().subtract(player.position());
                double horizontal = Math.max(0.001D, Math.sqrt(delta.x * delta.x + delta.z * delta.z));
                target.push(delta.x / horizontal * 1.8D, 0.35D, delta.z / horizontal * 1.8D);
                target.hurt(player.damageSources().mobAttack(livingPlayer), 6.0F);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static DamageSource resolveSonicBoomDamageSource(Entity player, LivingEntity attacker) {
        if (player == null) {
            return attacker.damageSources().generic();
        }
        if (attacker == null) {
            return player.damageSources().generic();
        }
        Object damageSources = invokeNoArg(player, "damageSources");
        if (damageSources != null) {
            Object source = invokeOneArg(damageSources, "sonicBoom", attacker);
            if (source instanceof DamageSource damageSource) {
                return damageSource;
            }
            source = invokeNoArg(damageSources, "sonicBoom");
            if (source instanceof DamageSource damageSource) {
                return damageSource;
            }
        }
        return player.damageSources().mobAttack(attacker);
    }

    private static BuiltinIdentityAbility createGenericMobAbility() {
        return new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(player instanceof LivingEntity livingPlayer)) {
                    return;
                }
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 start = livingPlayer.getEyePosition(1.0F);
                Vec3 end = start.add(look.scale(GENERIC_STRIKE_RANGE));
                AABB hitBox = livingPlayer.getBoundingBox().expandTowards(look.scale(GENERIC_STRIKE_RANGE)).inflate(1.0D);
                EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    livingPlayer,
                    start,
                    end,
                    hitBox,
                    candidate -> candidate != player && candidate.isPickable() && candidate instanceof LivingEntity,
                    GENERIC_STRIKE_RANGE * GENERIC_STRIKE_RANGE
                );

                if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                    float damage = resolveGenericDamage(player);
                    target.hurt(player.damageSources().mobAttack(livingPlayer), damage);
                    target.push(look.x * 0.45D, 0.10D, look.z * 0.45D);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.85F, 0.95F);
                    return;
                }

                player.setDeltaMovement(player.getDeltaMovement().add(look.x * GENERIC_DASH_STRENGTH, GENERIC_DASH_UP, look.z * GENERIC_DASH_STRENGTH));
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.75F, 1.05F);
            }
        };
    }

    private static float resolveGenericDamage(Entity player) {
        Entity currentIdentity = ((EntityAccessor) player).getCurrentIdentity();
        if (currentIdentity instanceof LivingEntity livingIdentity) {
            double attackDamage = livingIdentity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (attackDamage > 0.0D) {
                return (float) Mth.clamp(attackDamage, GENERIC_MIN_DAMAGE, GENERIC_MAX_DAMAGE);
            }
        }
        return 4.0F;
    }

    private static void openVillagerTrade(Entity player) {
        if (!(player instanceof Player tradingPlayer)) {
            return;
        }
        if (tryAcquireVillagerProfession(player)) {
            return;
        }
        if (!IdentitySettings.canTradeWithHimSelf) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("Self villager trading is disabled."), false);
            }
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity instanceof Villager villagerIdentity) {
            villagerIdentity.mobInteract(tradingPlayer, InteractionHand.MAIN_HAND);
            return;
        }
        if (identity instanceof WanderingTrader traderIdentity) {
            traderIdentity.mobInteract(tradingPlayer, InteractionHand.MAIN_HAND);
        }
    }

    private static boolean tryAcquireVillagerProfession(Entity player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!serverPlayer.isShiftKeyDown()) {
            return false;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (!(identity instanceof Villager villagerIdentity)) {
            return false;
        }

        HitResult hit = serverPlayer.pick(5.0D, 0.0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) {
            serverPlayer.displayClientMessage(Component.literal("Look at a villager workstation block to acquire a job."), true);
            return true;
        }

        BlockPos workstationPos = blockHit.getBlockPos();
        BlockState state = serverPlayer.level().getBlockState(workstationPos);
        Object poiReference = resolvePoiReference(state);
        ResourceLocation professionId = null;
        if (poiReference != null) {
            professionId = resolveProfessionForPoi(poiReference);
        }
        if (professionId == null) {
            professionId = resolveProfessionForWorkstationBlock(state);
        }
        if (professionId == null) {
            serverPlayer.displayClientMessage(Component.literal("That block is not a valid villager workstation."), true);
            return true;
        }

        if (!applyVillagerProfession(villagerIdentity, professionId)) {
            serverPlayer.displayClientMessage(Component.literal("Could not apply villager profession: " + professionId), true);
            return true;
        }

        syncVillagerVariantData(serverPlayer, villagerIdentity);
        serverPlayer.displayClientMessage(Component.literal("Acquired villager job: " + professionId), true);
        return true;
    }

    private static Object resolvePoiReference(BlockState state) {
        Object poi = invokePoiLookup("net.minecraft.world.entity.ai.village.poi.PoiTypes", state);
        if (poi != null) {
            return poi;
        }
        return invokePoiLookup("net.minecraft.world.poi.PointOfInterestTypes", state);
    }

    private static Object invokePoiLookup(String className, BlockState state) {
        try {
            Class<?> clazz = Class.forName(className);
            Set<Method> methods = new LinkedHashSet<>();
            for (Method method : clazz.getDeclaredMethods()) {
                methods.add(method);
            }
            for (Method method : clazz.getMethods()) {
                methods.add(method);
            }
            for (Method method : methods) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!(parameterType.isInstance(state) || parameterType.isAssignableFrom(BlockState.class))) {
                    continue;
                }
                if (!Optional.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                if (!method.canAccess(null)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(null, state);
                if (result instanceof Optional<?> optional && optional.isPresent()) {
                    return optional.get();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation resolveProfessionForWorkstationBlock(BlockState state) {
        if (state == null) {
            return null;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return null;
        }
        return switch (blockId.getPath()) {
            case "blast_furnace" -> new ResourceLocation("minecraft", "armorer");
            case "smoker" -> new ResourceLocation("minecraft", "butcher");
            case "cartography_table" -> new ResourceLocation("minecraft", "cartographer");
            case "brewing_stand" -> new ResourceLocation("minecraft", "cleric");
            case "composter" -> new ResourceLocation("minecraft", "farmer");
            case "barrel" -> new ResourceLocation("minecraft", "fisherman");
            case "fletching_table" -> new ResourceLocation("minecraft", "fletcher");
            case "cauldron" -> new ResourceLocation("minecraft", "leatherworker");
            case "lectern" -> new ResourceLocation("minecraft", "librarian");
            case "stonecutter" -> new ResourceLocation("minecraft", "mason");
            case "loom" -> new ResourceLocation("minecraft", "shepherd");
            case "smithing_table" -> new ResourceLocation("minecraft", "toolsmith");
            case "grindstone" -> new ResourceLocation("minecraft", "weaponsmith");
            default -> null;
        };
    }

    private static ResourceLocation resolveProfessionForPoi(Object poiReference) {
        Registry<?> professionRegistry = getBuiltInRegistry("VILLAGER_PROFESSION");
        if (professionRegistry == null || poiReference == null) {
            return null;
        }

        ResourceLocation poiId = resolvePoiResourceLocation(poiReference);
        for (Object profession : professionRegistry) {
            if (profession == null) {
                continue;
            }
            ResourceLocation professionId = getRegistryKey(professionRegistry, profession);
            if (professionId == null || "none".equals(professionId.getPath())) {
                continue;
            }

            if (poiId != null && professionId.getPath().equals(poiId.getPath())) {
                return professionId;
            }
            if (professionMatchesPoi(profession, poiReference)) {
                return professionId;
            }
        }
        return null;
    }

    private static boolean professionMatchesPoi(Object profession, Object poiReference) {
        if (profession == null || poiReference == null) {
            return false;
        }

        for (String methodName : List.of("heldWorkstation", "heldJobSite", "acquirableJobSite", "acquirableWorkstation")) {
            Object resolved = invokeNoArg(profession, methodName);
            if (resolved == null) {
                continue;
            }
            if (resolved instanceof Predicate<?> rawPredicate) {
                if (testPredicate(rawPredicate, poiReference)) {
                    return true;
                }
                continue;
            }
            if (valuesEquivalent(resolved, poiReference)) {
                return true;
            }
        }

        for (Method method : profession.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            String lower = method.getName().toLowerCase();
            if (!lower.contains("work") && !lower.contains("job")) {
                continue;
            }
            try {
                Object resolved = method.invoke(profession);
                if (resolved instanceof Predicate<?> rawPredicate && testPredicate(rawPredicate, poiReference)) {
                    return true;
                }
                if (valuesEquivalent(resolved, poiReference)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private static boolean testPredicate(Predicate<?> predicate, Object value) {
        if (predicate == null || value == null) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            Predicate<Object> cast = (Predicate<Object>) predicate;
            return cast.test(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean valuesEquivalent(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        Object leftValue = unwrapHolderValue(left);
        Object rightValue = unwrapHolderValue(right);
        if (leftValue != null && leftValue.equals(right)) {
            return true;
        }
        if (rightValue != null && left.equals(rightValue)) {
            return true;
        }
        return leftValue != null && rightValue != null && leftValue.equals(rightValue);
    }

    private static Object unwrapHolderValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("value");
            if (method.getParameterCount() == 0) {
                return method.invoke(value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation resolvePoiResourceLocation(Object poiReference) {
        Registry<?> poiRegistry = getBuiltInRegistry("POINT_OF_INTEREST_TYPE", "POI_TYPE");
        if (poiRegistry == null || poiReference == null) {
            return null;
        }

        ResourceLocation direct = getRegistryKey(poiRegistry, poiReference);
        if (direct != null) {
            return direct;
        }

        Object value = unwrapHolderValue(poiReference);
        if (value != null) {
            return getRegistryKey(poiRegistry, value);
        }
        return null;
    }

    private static Registry<?> getBuiltInRegistry(String... fieldNames) {
        if (fieldNames == null) {
            return null;
        }
        for (String name : fieldNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                Object value = BuiltInRegistries.class.getField(name).get(null);
                if (value instanceof Registry<?> registry) {
                    return registry;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean applyVillagerProfession(Villager villagerIdentity, ResourceLocation professionId) {
        if (villagerIdentity == null || professionId == null) {
            return false;
        }

        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.get(professionId);
        if (profession == null) {
            return false;
        }

        Object villagerData = invokeNoArg(villagerIdentity, "getVillagerData");
        if (villagerData == null) {
            return false;
        }

        Object updatedVillagerData = null;
        Holder<VillagerProfession> professionHolder = null;
        try {
            professionHolder = BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession);
        } catch (Throwable ignored) {
        }
        if (professionHolder != null) {
            updatedVillagerData = invokeOneArg(villagerData, "setProfession", professionHolder);
        }
        if (updatedVillagerData == null) {
            updatedVillagerData = invokeOneArg(villagerData, "setProfession", profession);
        }
        if (updatedVillagerData == null) {
            if (professionHolder != null) {
                updatedVillagerData = invokeOneArg(villagerData, "withProfession", professionHolder);
            }
        }
        if (updatedVillagerData == null) {
            updatedVillagerData = invokeOneArg(villagerData, "withProfession", profession);
        }
        if (updatedVillagerData == null) {
            return false;
        }

        Object levelOneData = invokeIntArg(updatedVillagerData, "setLevel", 1);
        if (levelOneData != null) {
            updatedVillagerData = levelOneData;
        }

        Object applied = invokeOneArg(villagerIdentity, "setVillagerData", updatedVillagerData);
        if (applied == null) {
            return false;
        }

        invokeIntArg(villagerIdentity, "setVillagerXp", 0);
        invokeIntArg(villagerIdentity, "setExperience", 0);
        clearVillagerOffers(villagerIdentity);
        invokeVillagerUpdateTrades(villagerIdentity);
        invokeNoArg(villagerIdentity, "restock");
        return true;
    }

    private static void syncVillagerVariantData(ServerPlayer player, Villager villagerIdentity) {
        if (player == null || villagerIdentity == null) {
            return;
        }
        CompoundTag customData = ((EntityAccessor) player).getCustomData();
        CompoundTag nbt = customData;
        CompoundTag variant = IdentityProgression.parseVariantNbt(
            net.Gabou.identity2.util.NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "")
        );
        Object villagerData = invokeNoArg(villagerIdentity, "getVillagerData");
        if (villagerData == null) {
            return;
        }

        Object profession = invokeNoArg(villagerData, "getProfession");
        ResourceLocation professionId = resolveRegistryResourceLocation("VILLAGER_PROFESSION", profession);
        if (professionId != null) {
            variant.putString("VillagerProfession", professionId.toString());
        }

        Object villagerType = invokeNoArg(villagerData, "getType");
        ResourceLocation typeId = resolveRegistryResourceLocation("VILLAGER_TYPE", villagerType);
        if (typeId != null) {
            variant.putString("VillagerType", typeId.toString());
        }

        Object level = invokeNoArg(villagerData, "getLevel");
        if (level instanceof Number number) {
            variant.putInt("VillagerLevel", Math.max(1, number.intValue()));
        }

        IdentityProgression.updateCurrentVariantAndSync(player, variant);
    }

    private static ResourceLocation resolveRegistryResourceLocation(String registryField, Object value) {
        Registry<?> registry = getBuiltInRegistry(registryField);
        if (registry == null || value == null) {
            return null;
        }
        ResourceLocation direct = getRegistryKey(registry, value);
        if (direct != null) {
            return direct;
        }
        Object normalized = normalizeLookupValue(value);
        if (normalized != null) {
            return getRegistryKey(registry, normalized);
        }
        return null;
    }

    private static void invokeVillagerUpdateTrades(Villager villagerIdentity) {
        if (villagerIdentity == null) {
            return;
        }
        if (invokeNoArg(villagerIdentity, "updateTrades") != null) {
            return;
        }

        Object level = invokeNoArg(villagerIdentity, "level");
        if (level instanceof ServerLevel serverLevel) {
            invokeOneArg(villagerIdentity, "updateTrades", serverLevel);
            return;
        }
        if (level != null) {
            invokeOneArg(villagerIdentity, "updateTrades", level);
        }
    }

    private static void clearVillagerOffers(Villager villagerIdentity) {
        Object offers = invokeNoArg(villagerIdentity, "getOffers");
        if (offers instanceof List<?> list) {
            list.clear();
            return;
        }
        if (offers != null) {
            invokeNoArg(offers, "clear");
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (arg == null || !paramType.isAssignableFrom(arg.getClass())) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, arg);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeIntArg(Object target, String methodName, int value) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!(paramType == int.class || paramType == Integer.class)) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, value);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static List<Method> getAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> param : params) {
                    signature += ":" + param.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation getRegistryKey(Registry<?> registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        try {
            return ((Registry<Object>) registry).getKey(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getRegistryValue(Registry<?> registry, ResourceLocation id) {
        if (registry == null || id == null) {
            return null;
        }
        try {
            Object value = ((Registry<Object>) registry).get(id);
            if (value == null) {
                value = invokeOneArg(registry, "getValue", id);
            }
            if (value == null) {
                value = invokeOneArg(registry, "get", id);
            }
            return normalizeLookupValue(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object wrapAsHolder(Registry<?> registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        Object unwrapped = normalizeLookupValue(value);
        for (Method method : registry.getClass().getMethods()) {
            if (!method.getName().equals("wrapAsHolder") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            Object candidate = unwrapped != null ? unwrapped : value;
            if (!parameterType.isAssignableFrom(candidate.getClass())) {
                continue;
            }
            try {
                return method.invoke(registry, candidate);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object normalizeLookupValue(Object value) {
        Object normalized = value;
        if (normalized instanceof Optional<?> optional) {
            normalized = optional.orElse(null);
        }
        if (normalized == null) {
            return null;
        }
        Object unwrapped = unwrapHolderValue(normalized);
        return unwrapped != null ? unwrapped : normalized;
    }

    private static Entity identity2$createAlexsCavesEntity(String registryClassName, String fieldName, Level level) {
        if (registryClassName == null || registryClassName.isBlank() || fieldName == null || fieldName.isBlank() || level == null) {
            return null;
        }
        try {
            Class<?> registryClass = Class.forName(registryClassName);
            Object registryObject = registryClass.getField(fieldName).get(null);
            Object entityTypeObject = invokeNoArg(registryObject, "get");
            if (!(entityTypeObject instanceof EntityType<?> entityType)) {
                return null;
            }
            return entityType.create(level);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean identity2$isAlexsCavesNucleeperCharged(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!"com.github.alexmodguy.alexscaves.server.entity.living.NucleeperEntity".equals(entity.getClass().getName())) {
            return false;
        }
        Object charged = invokeNoArg(entity, "isCharged");
        return charged instanceof Boolean value && value;
    }

    private static Object identity2$invokeOneArgLoose(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!identity2$isParameterCompatible(paramType, arg)) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                return method.invoke(target, arg);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean identity2$isParameterCompatible(Class<?> paramType, Object arg) {
        if (paramType == null) {
            return false;
        }
        if (arg == null) {
            return !paramType.isPrimitive();
        }
        if (paramType.isAssignableFrom(arg.getClass())) {
            return true;
        }
        if (paramType == boolean.class && arg instanceof Boolean) {
            return true;
        }
        if (paramType == float.class && arg instanceof Float) {
            return true;
        }
        if (paramType == double.class && arg instanceof Double) {
            return true;
        }
        if (paramType == int.class && arg instanceof Integer) {
            return true;
        }
        return false;
    }
}






