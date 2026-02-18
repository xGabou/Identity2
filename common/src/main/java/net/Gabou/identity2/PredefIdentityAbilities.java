package net.Gabou.identity2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.ShulkerEntityAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.Heightmap;

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

        map.put(Identifier.of("ghast"), new IdentityAbility() {
            @Override
            public void tick(Entity player, int cooldown) {
                HitResult target = player.raycast(1000, 0, false);
                World world = player.getEntityWorld();

                if (cooldown == 10 && !player.isSilent()) {
                    world.syncWorldEvent(null, WorldEvents.GHAST_WARNS, player.getBlockPos(), 0);
                }
                if (cooldown > 10 && ((EntityAccessor) player).getCurrentIdentity() instanceof GhastEntity ghastIdentity) {
                    ghastIdentity.setShooting(true);
                }

                if (cooldown == 20) {
                    Vec3d look = player.getRotationVec(1.0F);
                    Vec3d direction = new Vec3d(
                        target.getPos().x - (player.getX() + look.x * 4.0),
                        target.getPos().y - (0.5 + player.getBodyY(0.5)),
                        target.getPos().z - (player.getZ() + look.z * 4.0)
                    );
                    if (!player.isSilent()) {
                        world.syncWorldEvent(null, WorldEvents.GHAST_SHOOTS, player.getBlockPos(), 0);
                    }
                    FireballEntity fireball = new FireballEntity(world, (LivingEntity) player, direction.normalize(), 1);
                    fireball.setPosition(player.getX() + look.x * 4.0, player.getBodyY(0.5) + 0.5, player.getZ() + look.z * 4.0);
                    world.spawnEntity(fireball);
                    if (((EntityAccessor) player).getCurrentIdentity() instanceof GhastEntity ghastIdentity) {
                        ghastIdentity.setShooting(false);
                    }
                }
            }
        });

        map.put(Identifier.of("enderman"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                double maxDistance = IdentitySettings.endermanAbilityTeleportDistance;
                HitResult hit = player.raycast(maxDistance, 0, true);
                Vec3d targetPos = hit.getPos();
                BlockPos blockPos = BlockPos.ofFloored(targetPos);

                while (!isSafeTeleportSpot(world, blockPos) && world.isInHeightLimit((int) targetPos.y)) {
                    blockPos = blockPos.up();
                }

                Vec3d safePos = Vec3d.ofCenter(blockPos);
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
                );
                player.requestTeleport(safePos.x, safePos.y, safePos.z);
                world.playSound(
                    null,
                    safePos.x,
                    safePos.y,
                    safePos.z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
                );
            }

            private boolean isSafeTeleportSpot(World world, BlockPos pos) {
                return world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir();
            }
        });

        map.put(Identifier.of("shulker"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof ShulkerEntity shulker)) {
                    return;
                }
                if (((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 100) {
                    ((ShulkerEntityAccessor) shulker).setPeekAmount(100);
                }
            }

            @Override
            public void passivetick(Entity player, boolean usedLastTick) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof ShulkerEntity shulker)) {
                    return;
                }
                if (!usedLastTick && ((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 0) {
                    ((ShulkerEntityAccessor) shulker).setPeekAmount(0);
                }
                if (!shulker.getEntityWorld().isClient() && !shulker.hasVehicle() && !canStay(shulker.getBlockPos(), shulker.getAttachedFace(), shulker)) {
                    BlockPos pos = shulker.getBlockPos();
                    ((ShulkerEntityAccessor) shulker).runTryAttachOrTeleport();
                    if (!pos.equals(shulker.getBlockPos())) {
                        player.requestTeleport(shulker.getX(), shulker.getY(), shulker.getZ());
                    }
                }
            }

            @Override
            public boolean overrideAttack(Entity player) {
                if (!(((EntityAccessor) player).getCurrentIdentity() instanceof ShulkerEntity shulker)) {
                    return true;
                }
                if (((ShulkerEntityAccessor) shulker).runGetPeekAmount() != 0) {
                    double range = 128;
                    double rangeSq = MathHelper.square(range);
                    Vec3d start = player.getCameraPosVec(1);
                    Vec3d look = player.getRotationVec(1);
                    Vec3d end = start.add(look.x * range, look.y * range, look.z * range);
                    Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0, 1.0, 1.0);
                    HitResult target = ProjectileUtil.raycast(player, start, end, box, EntityPredicates.CAN_HIT, rangeSq);
                    if (target != null && target.getType() == HitResult.Type.ENTITY) {
                        shulker.getEntityWorld().spawnEntity(
                            new ShulkerBulletEntity(shulker.getEntityWorld(), shulker, ((EntityHitResult) target).getEntity(), shulker.getAttachedFace().getAxis())
                        );
                        shulker.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 2.0F, 1.0F);
                    }
                }
                return true;
            }

            private boolean canStay(BlockPos pos, Direction direction, ShulkerEntity entity) {
                if (isInvalidPosition(pos, entity)) {
                    return false;
                }
                Direction opposite = direction.getOpposite();
                if (!entity.getEntityWorld().isDirectionSolid(pos.offset(direction), entity, opposite)) {
                    return false;
                }
                Box box = ShulkerEntity.calculateBoundingBox(entity.getScale(), opposite, 1.0F, pos.toBottomCenterPos()).contract(1.0E-6);
                return entity.getEntityWorld().isSpaceEmpty(entity, box);
            }

            private boolean isInvalidPosition(BlockPos pos, ShulkerEntity entity) {
                if (entity.getEntityWorld().getBlockState(pos).isAir()) {
                    return false;
                }
                return !entity.getEntityWorld().getBlockState(pos).isOf(net.minecraft.block.Blocks.MOVING_PISTON) || !pos.equals(entity.getBlockPos());
            }
        });

        map.put(Identifier.of("blaze"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                Vec3d look = player.getRotationVec(1.0F);
                Vec3d spawnPos = player.getEyePos().add(look.multiply(0.6));
                SmallFireballEntity smallFireball = new SmallFireballEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, look);
                world.spawnEntity(smallFireball);
                world.playSoundFromEntity(
                    null,
                    player,
                    SoundEvents.ENTITY_BLAZE_SHOOT,
                    SoundCategory.HOSTILE,
                    2.0F,
                    (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F
                );
            }
        });

        map.put(Identifier.of("cow"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                if (player instanceof LivingEntity living) {
                    living.clearStatusEffects();
                }
                player.getEntityWorld().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
                );
            }
        });

        map.put(Identifier.of("creeper"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                float power = 3.0F;
                Entity current = ((EntityAccessor) player).getCurrentIdentity();
                if (current instanceof CreeperEntity creeper && creeper.isCharged()) {
                    power = 6.0F;
                }
                player.getEntityWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(), power, World.ExplosionSourceType.NONE);
            }
        });

        map.put(Identifier.of("endermite"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                double startX = player.getX();
                double startY = player.getY();
                double startZ = player.getZ();

                for (int i = 0; i < 16; ++i) {
                    double targetX = startX + (player.getRandom().nextDouble() - 0.5D) * 16.0D;
                    int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(targetX), MathHelper.floor(startZ));
                    double targetY = MathHelper.clamp(startY + (player.getRandom().nextInt(16) - 8), world.getBottomY(), topY - 1);
                    double targetZ = startZ + (player.getRandom().nextDouble() - 0.5D) * 16.0D;
                    if (player.hasVehicle()) {
                        player.stopRiding();
                    }
                    player.requestTeleport(targetX, targetY, targetZ);
                    world.playSound(null, startX, startY, startZ, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    player.playSound(SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                    break;
                }
            }
        });

        map.put(Identifier.of("evoker"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                Vec3d origin = player.getEntityPos();
                Vec3d facing = player.getRotationVec(1.0F).multiply(1, 0, 1);
                World world = player.getEntityWorld();

                for (int blockOut = 0; blockOut < 8; blockOut++) {
                    origin = origin.add(facing);
                    EvokerFangsEntity fangs = new EvokerFangsEntity(world, origin.x, origin.y, origin.z, player.getYaw(), blockOut * 2, (LivingEntity) player);

                    BlockPos pos = BlockPos.ofFloored(origin);
                    BlockPos below = pos.down();
                    if (world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP) && world.isAir(pos)) {
                        world.spawnEntity(fangs);
                        continue;
                    }

                    BlockPos below2 = pos.down(2);
                    if (world.getBlockState(below2).isSideSolidFullSquare(world, below2, Direction.UP) && world.isAir(below2.up())) {
                        fangs.setPos(fangs.getX(), fangs.getY() - 1, fangs.getZ());
                        world.spawnEntity(fangs);
                        origin = origin.add(0, -1, 0);
                        continue;
                    }

                    BlockPos up = pos.up();
                    if (world.getBlockState(pos).isSideSolidFullSquare(world, up, Direction.UP) && world.isAir(up)) {
                        fangs.setPos(fangs.getX(), fangs.getY() + 1, fangs.getZ());
                        world.spawnEntity(fangs);
                        origin = origin.add(0, 1, 0);
                        continue;
                    }
                    break;
                }
            }
        });

        map.put(Identifier.of("guardian"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                List<PlayerEntity> targets = player.getEntityWorld().getNonSpectatingEntities(PlayerEntity.class, player.getBoundingBox().expand(50.0D));
                for (PlayerEntity target : targets) {
                    if (target != player) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 60, 2));
                    }
                }
            }
        });

        map.put(Identifier.of("llama"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                Vec3d look = player.getRotationVec(1.0F);
                LlamaSpitEntity spit = new LlamaSpitEntity(world, (LlamaEntity) ((EntityAccessor) player).getCurrentIdentity());
                Vec3d spawnPos = player.getEyePos().add(look.multiply(1.0));
                spit.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
                spit.setVelocity(look.x, look.y, look.z, 1.5F, 10.0F);
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_LLAMA_SPIT,
                    player.getSoundCategory(),
                    1.0F,
                    1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F
                );
                world.spawnEntity(spit);
            }
        });

        map.put(Identifier.of("snow_golem"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW,
                    SoundCategory.NEUTRAL,
                    0.5F,
                    0.4F / (world.random.nextFloat() * 0.4F + 0.8F)
                );
                Vec3d look = player.getRotationVec(1.0F);
                Vec3d spawnPos = player.getEyePos().add(look.multiply(0.8));
                for (int i = 0; i < 10; i++) {
                    SnowballEntity snowball = new SnowballEntity(net.minecraft.entity.EntityType.SNOWBALL, world);
                    snowball.setOwner(player);
                    snowball.setItem(new ItemStack(Items.SNOWBALL));
                    float pitchOffset = (float) (player.getPitch() + world.random.nextGaussian() * 5.0);
                    float yawOffset = (float) (player.getYaw() + world.random.nextGaussian() * 5.0);
                    snowball.setVelocity((LivingEntity) player, pitchOffset, yawOffset, 0.0F, 1.5F, 1.0F);
                    snowball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yawOffset, pitchOffset);
                    world.spawnEntity(snowball);
                }
            }
        });

        map.put(Identifier.of("witch"), new IdentityAbility() {
            private final List<RegistryEntry<Potion>> validPotions = List.of(Potions.HARMING, Potions.POISON, Potions.SLOWNESS, Potions.WEAKNESS);

            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                SplashPotionEntity potionEntity = new SplashPotionEntity(net.minecraft.entity.EntityType.SPLASH_POTION, world);
                potionEntity.setOwner(player);
                RegistryEntry<Potion> potion = validPotions.get(world.random.nextInt(validPotions.size()));
                ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
                potionStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));
                potionEntity.setItem(potionStack);
                potionEntity.setPitch(-20.0F);
                Vec3d look = player.getRotationVec(1.0F);
                potionEntity.setVelocity(look.getX(), look.getY(), look.getZ(), 0.75F, 8.0F);
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_WITCH_THROW,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.8F + world.getRandom().nextFloat() * 0.4F
                );
                world.spawnEntity(potionEntity);
            }
        });

        map.put(Identifier.of("wither"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_WITHER_SHOOT,
                    SoundCategory.HOSTILE,
                    1.0F,
                    0.8F + world.random.nextFloat() * 0.4F
                );
                Vec3d look = player.getRotationVec(1.0F);
                Vec3d spawnPos = player.getEyePos().add(look.multiply(2.0));
                WitherSkullEntity skull = new WitherSkullEntity(world, (LivingEntity) player, look);
                skull.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
                skull.setVelocity(look.x, look.y, look.z, 1.5F, 0.0F);
                world.spawnEntity(skull);
            }
        });

        map.put(Identifier.of("ender_dragon"), new IdentityAbility() {
            @Override
            public void execute(Entity player) {
                World world = player.getEntityWorld();
                Vec3d look = player.getRotationVec(1.0F);
                Vec3d velocity = look.multiply(0.5);
                Vec3d spawnPos = player.getEyePos().add(look.multiply(2.0));
                DragonFireballEntity fireball = new DragonFireballEntity(world, (LivingEntity) player, velocity);
                fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
                fireball.setOwner(player);
                world.spawnEntity(fireball);
                world.playSoundFromEntity(null, player, SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.HOSTILE, 3.0F, 1.0F);
            }
        });

        return map;
    }
}
