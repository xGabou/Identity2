package net.Gabou.identity2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.ShulkerEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PredefIdentityAbilities {
    abstract static class IdentityAbility {
        public void execute(Entity player) {
        }

        public void tick(Entity player, int cooldown) {
        }

        public void passivetick(Entity player, boolean used) {
        }

        public boolean overrideAttack(Entity player) {
            return false;
        }
    }

    public static final Map<Identifier, IdentityAbility> predef = create();

    private PredefIdentityAbilities() {
    }

    private static Map<Identifier, IdentityAbility> create() {
        Map<Identifier, IdentityAbility> map = new HashMap<>();

        map.put(Identifier.parse("ghast"), new IdentityAbility() {
            @Override
            public void tick(Entity player, int cooldown) {
                HitResult target = player.pick(1000, 0, false);
                Level world = player.level();

                if (cooldown == 10 && !player.isSilent()) {
                    world.levelEvent(null, LevelEvent.SOUND_GHAST_WARNING, player.blockPosition(), 0);
                }
                if (cooldown > 10 && ((EntityAccessor) player).getCurrentIdentity() instanceof Ghast ghastIdentity) {
                    ghastIdentity.setCharging(true);
                }

                if (cooldown == 20) {
                    Vec3 look = player.getViewVector(1.0F);
                    Vec3 direction = new Vec3(
                        target.getLocation().x - (player.getX() + look.x * 4.0),
                        target.getLocation().y - (0.5 + player.getY(0.5)),
                        target.getLocation().z - (player.getZ() + look.z * 4.0)
                    );
                    if (!player.isSilent()) {
                        world.levelEvent(null, LevelEvent.SOUND_GHAST_FIREBALL, player.blockPosition(), 0);
                    }
                    LargeFireball fireball = new LargeFireball(world, (LivingEntity) player, direction.normalize(), 1);
                    fireball.setPos(player.getX() + look.x * 4.0, player.getY(0.5) + 0.5, player.getZ() + look.z * 4.0);
                    world.addFreshEntity(fireball);
                    if (((EntityAccessor) player).getCurrentIdentity() instanceof Ghast ghastIdentity) {
                        ghastIdentity.setCharging(false);
                    }
                }
            }
        });

        map.put(Identifier.parse("enderman"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                double maxDistance = IdentitySettings.endermanAbilityTeleportDistance;
                HitResult hit = player.pick(maxDistance, 0, true);
                Vec3 targetPos = hit.getLocation();
                BlockPos blockPos = BlockPos.containing(targetPos);

                while (!isSafeTeleportSpot(world, blockPos) && world.isInsideBuildHeight((int) targetPos.y)) {
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

        map.put(Identifier.parse("shulker"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return;
                }
                if (((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 100) {
                    ((ShulkerEntityAccessor) shulker).setPeekAmount(100);
                }
            }

            @Override
            public void passivetick(Entity player, boolean usedLastTick) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return;
                }
                if (!usedLastTick && ((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 0) {
                    ((ShulkerEntityAccessor) shulker).setPeekAmount(0);
                }
                if (!shulker.level().isClientSide() && !shulker.isPassenger() && !canStay(shulker.blockPosition(), shulker.getAttachFace(), shulker)) {
                    BlockPos pos = shulker.blockPosition();
                    ((ShulkerEntityAccessor) shulker).runTryAttachOrTeleport();
                    if (!pos.equals(shulker.blockPosition())) {
                        player.teleportTo(shulker.getX(), shulker.getY(), shulker.getZ());
                    }
                }
            }

            @Override
            public boolean overrideAttack(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof Shulker shulker)) {
                    return true;
                }
                if (((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 0) {
                    double range = 128;
                    double rangeSq = Mth.square(range);
                    Vec3 start = player.getEyePosition(1);
                    Vec3 look = player.getViewVector(1);
                    Vec3 end = start.add(look.x * range, look.y * range, look.z * range);
                    AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0, 1.0, 1.0);
                    HitResult target = ProjectileUtil.getEntityHitResult(player, start, end, box, EntitySelector.CAN_BE_PICKED, rangeSq);
                    if (target != null && target.getType() == HitResult.Type.ENTITY) {
                        shulker.level().addFreshEntity(
                            new ShulkerBullet(shulker.level(), shulker, ((EntityHitResult) target).getEntity(), shulker.getAttachFace().getAxis())
                        );
                        shulker.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 1.0F);
                    }
                }
                return true;
            }

            private boolean canStay(BlockPos pos, Direction direction, Shulker entity) {
                if (isInvalidPosition(pos, entity)) {
                    return false;
                }
                Direction opposite = direction.getOpposite();
                if (!entity.level().loadedAndEntityCanStandOnFace(pos.relative(direction), entity, opposite)) {
                    return false;
                }
                AABB box = Shulker.getProgressAabb(entity.getScale(), opposite, 1.0F, pos.getBottomCenter()).deflate(1.0E-6);
                return entity.level().noCollision(entity, box);
            }

            private boolean isInvalidPosition(BlockPos pos, Shulker entity) {
                if (entity.level().getBlockState(pos).isAir()) {
                    return false;
                }
                return !entity.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MOVING_PISTON) || !pos.equals(entity.blockPosition());
            }
        });

        map.put(Identifier.parse("blaze"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(0.6));
                SmallFireball smallFireball = new SmallFireball(world, spawnPos.x, spawnPos.y, spawnPos.z, look);
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

        map.put(Identifier.parse("cow"), new IdentityAbility() {
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

        map.put(Identifier.parse("creeper"), new IdentityAbility() {
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

        map.put(Identifier.parse("endermite"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                double startX = player.getX();
                double startY = player.getY();
                double startZ = player.getZ();

                for (int i = 0; i < 16; ++i) {
                    double targetX = startX + (player.getRandom().nextDouble() - 0.5D) * 16.0D;
                    int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(targetX), Mth.floor(startZ));
                    double targetY = Mth.clamp(startY + (player.getRandom().nextInt(16) - 8), world.getMinY(), topY - 1);
                    double targetZ = startZ + (player.getRandom().nextDouble() - 0.5D) * 16.0D;
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

        map.put(Identifier.parse("evoker"), new IdentityAbility() {
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

        map.put(Identifier.parse("guardian"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                List<Player> targets = player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(50.0D));
                for (Player target : targets) {
                    if (target != player) {
                        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20 * 60, 2));
                    }
                }
            }
        });

        map.put(Identifier.parse("llama"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                LlamaSpit spit = new LlamaSpit(world, (Llama) ((EntityAccessor) player).getCurrentIdentity());
                Vec3 spawnPos = player.getEyePosition().add(look.scale(1.0));
                spit.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
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

        map.put(Identifier.parse("snow_golem"), new IdentityAbility() {
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
                    Snowball snowball = new Snowball(net.minecraft.world.entity.EntityType.SNOWBALL, world);
                    snowball.setOwner(player);
                    snowball.setItem(new ItemStack(Items.SNOWBALL));
                    float pitchOffset = (float) (player.getXRot() + world.random.nextGaussian() * 5.0);
                    float yawOffset = (float) (player.getYRot() + world.random.nextGaussian() * 5.0);
                    snowball.shootFromRotation((LivingEntity) player, pitchOffset, yawOffset, 0.0F, 1.5F, 1.0F);
                    snowball.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, yawOffset, pitchOffset);
                    world.addFreshEntity(snowball);
                }
            }
        });

        map.put(Identifier.parse("witch"), new IdentityAbility() {
            private final List<Holder<Potion>> validPotions = List.of(Potions.HARMING, Potions.POISON, Potions.SLOWNESS, Potions.WEAKNESS);

            @Override
            public void execute(Entity player) {
                Level world = player.level();
                ThrownSplashPotion potionEntity = new ThrownSplashPotion(net.minecraft.world.entity.EntityType.SPLASH_POTION, world);
                potionEntity.setOwner(player);
                Holder<Potion> potion = validPotions.get(world.random.nextInt(validPotions.size()));
                ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
                potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                potionEntity.setItem(potionStack);
                potionEntity.setXRot(-20.0F);
                Vec3 look = player.getViewVector(1.0F);
                potionEntity.shoot(look.x(), look.y(), look.z(), 0.75F, 8.0F);
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

        map.put(Identifier.parse("wither"), new IdentityAbility() {
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
                WitherSkull skull = new WitherSkull(world, (LivingEntity) player, look);
                skull.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                skull.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
                world.addFreshEntity(skull);
            }
        });

        map.put(Identifier.parse("ender_dragon"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Level world = player.level();
                Vec3 look = player.getViewVector(1.0F);
                Vec3 velocity = look.scale(0.5);
                Vec3 spawnPos = player.getEyePosition().add(look.scale(2.0));
                DragonFireball fireball = new DragonFireball(world, (LivingEntity) player, velocity);
                fireball.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
                fireball.setOwner(player);
                world.addFreshEntity(fireball);
                world.playSound(null, player, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3.0F, 1.0F);
            }
        });

        return map;
    }
}
