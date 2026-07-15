package net.Gabou.identity2.client;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.mixin.BeeAccessor;
import net.Gabou.identity2.mixin.CamelAccessor;
import net.Gabou.identity2.mixin.CreeperAccessor;
import net.Gabou.identity2.mixin.EnderManAccessor;
import net.Gabou.identity2.mixin.HoglinAccessor;
import net.Gabou.identity2.mixin.IronGolemAccessor;
import net.Gabou.identity2.mixin.RavagerAccessor;
import net.Gabou.identity2.mixin.WolfAccessor;
import net.Gabou.identity2.mixin.ZoglinAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class IdentityRenderStateHelper {
    private static final float ENDER_DRAGON_MORPH_FLAP_SPEED = 1.0F / 10.0F;
    private static final Map<Integer, Boolean> WOLF_WAS_IN_WATER = new HashMap<>();
    private static final Map<Integer, Float> WOLF_SHAKE_PROGRESS = new HashMap<>();
    private static final Map<AnimationState, Integer> ANIMATION_START_TICKS = new IdentityHashMap<>();

    private IdentityRenderStateHelper() {
    }

    public static void syncIdentityVisualState(Entity source, Entity identity) {
        syncIdentityVisualState(source, identity, 0.0F);
    }

    public static void syncIdentityVisualState(Entity source, Entity identity, float tickDelta) {
        net.minecraft.world.phys.Vec3 renderPos = source.position();
        identity.setPosRaw(renderPos.x, renderPos.y, renderPos.z);
        if (identity instanceof EnderDragon) {
            identity.setYRot(source.getYRot() + 180.0F);
            syncEnderDragonFlapAnimation(source, (EnderDragon) identity);
        } else {
            identity.setYRot(source.getYRot());
        }
        ((EntityAccessor) identity).setLastPosition(new Vec3(source.xOld, source.yOld, source.zOld));

        boolean preserveNativeAttackState = isHoglinFamily(identity) || identity instanceof IronGolem || identity instanceof Ravager;

        if (identity instanceof LivingEntity livingIdentity && source instanceof LivingEntity livingSource) {
            syncLivingHealthForRender(livingSource, livingIdentity);
            if (((LivingEntityAccessor) livingIdentity).identity2$isJumping()
                    != ((LivingEntityAccessor) livingSource).identity2$isJumping()) {
                livingIdentity.setJumping(((LivingEntityAccessor) livingSource).identity2$isJumping());
            }

            LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
            LimbAnimatorAccessor origin = (LimbAnimatorAccessor) livingSource.walkAnimation;
            target.setPrevSpeed(origin.getPrevSpeed());
            target.setPosition(origin.getPosition());
            target.setPositionScale(origin.getPositionScale());
            livingIdentity.walkAnimation.setSpeed(livingSource.walkAnimation.speed());

            if (!preserveNativeAttackState) {
                livingIdentity.swinging = livingSource.swinging;
                livingIdentity.swingTime = livingSource.swingTime;
                livingIdentity.oAttackAnim = livingSource.oAttackAnim;
                livingIdentity.attackAnim = livingSource.attackAnim;
            }
            livingIdentity.hurtTime = livingSource.hurtTime;
            livingIdentity.hurtDuration = livingSource.hurtDuration;
            livingIdentity.deathTime = livingSource.deathTime;
            livingIdentity.invulnerableTime = livingSource.invulnerableTime;
            livingIdentity.setInvisible(livingSource.isInvisible() || livingSource.hasEffect(MobEffects.INVISIBILITY));
            if (!(livingIdentity instanceof Shulker)) {
                livingIdentity.yBodyRot = livingSource.yBodyRot;
                livingIdentity.yBodyRotO = livingSource.yBodyRotO;
            }
            livingIdentity.yHeadRot = livingSource.yHeadRot;
            livingIdentity.yHeadRotO = livingSource.yHeadRotO;
            syncSpeciesHeadRotation(livingSource, livingIdentity);
            livingIdentity.swingingArm = livingSource.swingingArm;
            if (livingSource.isUsingItem()) {
                livingIdentity.startUsingItem(livingSource.getUsedItemHand());
            } else {
                livingIdentity.stopUsingItem();
            }

            if (livingIdentity instanceof Bat batIdentity) {
                batIdentity.setResting(false);
            }

            if (livingIdentity instanceof Bee beeIdentity) {
                boolean rolling = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY)
                        || PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ROLL_TICKS_KEY);
                BeeAccessor beeAccessor = (BeeAccessor) beeIdentity;
                beeAccessor.identity2$setRolling(rolling);
                if (!rolling) {
                    float currentRoll = beeAccessor.identity2$getRollAmount();
                    float recoveredRoll = recoverBeeRoll(currentRoll);
                    if (Math.abs(recoveredRoll) < 0.01F) {
                        recoveredRoll = 0.0F;
                    }
                    beeAccessor.identity2$setRollAmountO(currentRoll);
                    beeAccessor.identity2$setRollAmount(recoveredRoll);
                }
                beeIdentity.setStayOutOfHiveCountdown(0);
                if (!source.onGround() || source.getDeltaMovement().y > 0.0D) {
                    beeIdentity.setRemainingPersistentAngerTime(0);
                }
            }

            if (livingIdentity instanceof Creeper creeperIdentity) {
                syncCreeperHissState(source, creeperIdentity);
            }

            if (livingIdentity instanceof Squid squidIdentity) {
                // Tentacle motion is animated by vanilla Squid.aiStep, which runs because
                // ClientWorldMixin ticks the morph entity every client tick. Do not write
                // tentacleMovement/tentacleAngle here: deriving the phase from
                // tickCount * rate makes the phase jump whenever the swim speed changes,
                // which showed up as jerky squid/glow squid tentacles.
                float swimSpeed = livingSource.walkAnimation.speed();
                livingIdentity.walkAnimation.setSpeed(Math.max(0.15F, swimSpeed));
                if (source.isInWater()) {
                    squidIdentity.setSwimming(true);
                }
            }

            if (livingIdentity instanceof Turtle) {
                scaleWalkAnimation(livingSource, livingIdentity, 0.12F);
            }

            if (livingIdentity instanceof Armadillo) {
                scaleWalkAnimation(livingSource, livingIdentity, 0.30F);
            }

            if (livingIdentity instanceof Axolotl && !source.isInWater()) {
                livingIdentity.setSwimming(false);
                float horizontalSpeed = (float) source.getDeltaMovement().horizontalDistance();
                float landSpeed = horizontalSpeed > 0.01F ? Math.max(livingSource.walkAnimation.speed(), horizontalSpeed * 6.0F) : 0.0F;
                livingIdentity.walkAnimation.setSpeed(landSpeed);
            }

            if (livingIdentity instanceof Rabbit) {
                applyRabbitMovementAnimation(livingIdentity, livingSource);
            }
        }

        identity.tickCount = source.tickCount;
        identity.setOnGround(source.onGround());
        identity.setDeltaMovement(source.getDeltaMovement());
        identity.setShiftKeyDown(source.isShiftKeyDown());
        identity.setSprinting(source.isSprinting());
        identity.setSwimming(source.isSwimming());
        identity.setPose(source.getPose());

        ((EntityAccessor) identity).setVehicle(source.getVehicle());
        ((EntityAccessor) identity).setTouchingWater(source.isInWater());

        if (identity instanceof Phantom) {
            identity.setXRot(-source.getXRot());
            identity.xRotO = -source.xRotO;
        } else if (identity instanceof Goat goatIdentity) {
            // TODO: Goat head anchoring is clamped here, but it still is not a perfect vanilla head/body binding.
            float bodyYaw = source instanceof LivingEntity livingSource ? livingSource.yBodyRot : source.getYRot();
            float headYaw = bodyYaw + net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(source.getYRot() - bodyYaw), -28.0F, 28.0F);
            goatIdentity.yBodyRot = bodyYaw;
            goatIdentity.yBodyRotO = bodyYaw;
            goatIdentity.yHeadRot = headYaw;
            goatIdentity.yHeadRotO = headYaw;
            goatIdentity.setXRot(net.minecraft.util.Mth.clamp(source.getXRot(), -25.0F, 25.0F));
            goatIdentity.xRotO = net.minecraft.util.Mth.clamp(source.xRotO, -25.0F, 25.0F);
        } else if (!(identity instanceof Shulker)) {
            identity.setXRot(source.getXRot());
            identity.xRotO = source.xRotO;
        }

        if (source instanceof LivingEntity livingSource && identity instanceof LivingEntity livingIdentity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                setItemSlotIfChanged(livingIdentity, slot, visibleMorphEquipment(livingSource, slot));
            }
        }

        if (source instanceof LivingEntity livingSource && identity instanceof Mob mobIdentity) {
            if (!preserveNativeAttackState) {
                mobIdentity.setAggressive(livingSource.isUsingItem());
            }
        }

        if (source.isInWater()) {
            identity.clearFire();
            identity.setSharedFlagOnFire(false);
        } else {
            identity.setSharedFlagOnFire(identity.isOnFire());
        }

        syncEndermanCarriedBlock(source, identity);
        syncEndermanAngryState(source, identity);
        syncParrotDanceState(source, identity);
        syncWolfWetState(source, identity);
        syncEntityAnimationState(source, identity, tickDelta);
    }

    private static void setItemSlotIfChanged(LivingEntity identity, EquipmentSlot slot, ItemStack stack) {
        ItemStack current = identity.getItemBySlot(slot);
        if (!ItemStack.matches(current, stack)) {
            identity.setItemSlot(slot, stack);
        }
    }

    private static void scaleWalkAnimation(LivingEntity source, LivingEntity identity, float scale) {
        LimbAnimatorAccessor target = (LimbAnimatorAccessor) identity.walkAnimation;
        LimbAnimatorAccessor origin = (LimbAnimatorAccessor) source.walkAnimation;
        target.setPrevSpeed(origin.getPrevSpeed() * scale);
        target.setPosition(origin.getPosition() * scale);
        target.setPositionScale(origin.getPositionScale() * scale);
        identity.walkAnimation.setSpeed(source.walkAnimation.speed() * scale);
    }

    private static ItemStack visibleMorphEquipment(LivingEntity source, EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HAND && !net.Gabou.identity2.IdentitySettings.identitiesEquipItems) {
            return ItemStack.EMPTY;
        }
        if (slot.getType() != EquipmentSlot.Type.HAND && !net.Gabou.identity2.IdentitySettings.identitiesEquipArmor) {
            return ItemStack.EMPTY;
        }
        return source.getItemBySlot(slot);
    }

    private static void syncEndermanCarriedBlock(Entity source, Entity identity) {
        if (!(identity instanceof EnderMan enderMan) || !(source instanceof LivingEntity livingSource)) {
            return;
        }
        ItemStack stack = livingSource.getMainHandItem();
        BlockState carried = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
        enderMan.setCarriedBlock(carried);
    }

    private static void syncEndermanAngryState(Entity source, Entity identity) {
        if (!(identity instanceof EnderMan enderMan)) {
            return;
        }

        boolean angry = net.Gabou.identity2.util.NbtCompat.getBooleanOr(
                ((EntityAccessor) source).getCustomData(),
                PredefIdentityAbilities.ENDERMAN_ANGRY_STATE_KEY,
                false
        );

        enderMan.getEntityData().set(EnderManAccessor.identity2$getDataCreepy(), angry);
        enderMan.getEntityData().set(EnderManAccessor.identity2$getDataStaredAt(), angry);
    }

    private static void syncCreeperHissState(Entity source, Creeper creeper) {
        CreeperAccessor accessor = (CreeperAccessor) creeper;

        int fuseRemaining = PredefIdentityAbilities.getSyncedTicksRemaining(
                source,
                PredefIdentityAbilities.CREEPER_FUSE_TICKS_KEY
        );

        if (fuseRemaining <= 0) {
            creeper.setSwellDir(-1);
            accessor.identity2$setOldSwell(0);
            accessor.identity2$setSwell(0);
            return;
        }

        int duration = PredefIdentityAbilities.CREEPER_FUSE_DURATION_TICKS;

        int elapsed = Mth.clamp(
                duration - fuseRemaining,
                0,
                duration
        );

        creeper.setSwellDir(1);
        accessor.identity2$setOldSwell(Math.max(0, elapsed - 1));
        accessor.identity2$setSwell(elapsed);
    }

    private static void syncParrotDanceState(Entity source, Entity identity) {
        if (!(identity instanceof Parrot parrot) || source.level() == null) {
            return;
        }
        net.minecraft.core.BlockPos origin = source.blockPosition();
        net.minecraft.core.BlockPos playingJukebox = null;
        outer:
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    net.minecraft.core.BlockPos candidate = origin.offset(x, y, z);
                    if (source.level().getBlockEntity(candidate) instanceof JukeboxBlockEntity jukebox
                            && jukebox.getSongPlayer().isPlaying()) {
                        playingJukebox = candidate;
                        break outer;
                    }
                }
            }
        }
        parrot.setRecordPlayingNearby(playingJukebox == null ? origin : playingJukebox, playingJukebox != null);
    }

    private static void syncWolfWetState(Entity source, Entity identity) {
        if (!(identity instanceof Wolf wolf)) {
            return;
        }

        WolfAccessor accessor = (WolfAccessor) wolf;

        int entityId = source.getId();
        boolean inWater = isWolfMorphWet(source);
        boolean wasInWater = WOLF_WAS_IN_WATER.getOrDefault(entityId, inWater);
        float shakeProgress = WOLF_SHAKE_PROGRESS.getOrDefault(entityId, 0.0F);

        if (inWater) {
            accessor.identity2$setWet(true);
            accessor.identity2$setShaking(false);
            accessor.identity2$setShakeAnim(0.0F);
            accessor.identity2$setShakeAnimO(0.0F);

            WOLF_SHAKE_PROGRESS.put(entityId, 0.0F);
            WOLF_WAS_IN_WATER.put(entityId, true);
            return;
        }

        if (wasInWater && shakeProgress <= 0.0F) {
            accessor.identity2$setWet(true);
            accessor.identity2$setShaking(true);
            shakeProgress = 0.01F;
        }

        if (shakeProgress > 0.0F) {
            float previous = shakeProgress;
            shakeProgress = Math.min(1.0F, shakeProgress + 0.010F);

            accessor.identity2$setShakeAnimO(previous);
            accessor.identity2$setShakeAnim(shakeProgress);
            accessor.identity2$setShaking(true);
            accessor.identity2$setWet(true);

            if (shakeProgress >= 1.0F) {
                accessor.identity2$setShaking(false);
                accessor.identity2$setWet(false);
                accessor.identity2$setShakeAnim(0.0F);
                accessor.identity2$setShakeAnimO(0.0F);
                shakeProgress = 0.0F;
            }
        } else {
            accessor.identity2$setShaking(false);
            accessor.identity2$setWet(false);
            accessor.identity2$setShakeAnim(0.0F);
            accessor.identity2$setShakeAnimO(0.0F);
        }

        WOLF_SHAKE_PROGRESS.put(entityId, shakeProgress);
        WOLF_WAS_IN_WATER.put(entityId, false);
    }

    private static void syncLivingHealthForRender(LivingEntity source, LivingEntity identity) {
        float sourceMaxHealth = source.getMaxHealth();
        float identityMaxHealth = identity.getMaxHealth();
        if (sourceMaxHealth <= 0.0F || identityMaxHealth <= 0.0F) {
            return;
        }
        float scaledHealth = source.getHealth() * (identityMaxHealth / sourceMaxHealth);
        identity.setHealth(Math.max(0.0F, Math.min(identityMaxHealth, scaledHealth)));
    }

    /**
     * Smoothly recovers bee roll after ability use.
     *
     * @param currentRoll the current roll value
     * @return the smoothed roll value
     */
    private static float recoverBeeRoll(float currentRoll) {
        return net.minecraft.util.Mth.lerp(0.18F, currentRoll, 0.0F);
    }

    /**
     * Returns true when a wolf morph should be considered wet.
     *
     * @param wolf the wolf morph entity
     * @return true if wet from water or rain
     */
    private static boolean isWolfMorphWet(Entity wolf) {
        return wolf.isInWaterRainOrBubble();
    }

    /**
     * Applies rabbit movement animation state.
     *
     * <p>The rabbit hop animation is driven by the private jumpTicks/jumpDuration
     * cycle that vanilla starts via entity event 1 and advances in Rabbit.aiStep
     * (which runs because ClientWorldMixin ticks the morph entity). Calling
     * setJumping(true) does not start that cycle — and Rabbit.setJumping(true)
     * also plays the jump sound on every call — so we trigger the entity event
     * whenever the previous hop cycle has finished.</p>
     *
     * @param renderState the rabbit render state
     * @param source the morphed player
     */
    private static void applyRabbitMovementAnimation(LivingEntity renderState, LivingEntity source) {
        if (!(renderState instanceof Rabbit rabbit)) {
            return;
        }
        boolean hopping = source.getDeltaMovement().horizontalDistanceSqr() > 0.0025D && source.onGround();
        if (!hopping) {
            return;
        }
        renderState.walkAnimation.setSpeed(Math.max(0.35F, source.walkAnimation.speed()));
        if (((net.Gabou.identity2.mixin.RabbitAccessor) rabbit).identity2$getJumpDuration() == 0) {
            rabbit.handleEntityEvent((byte) 1);
        }
    }

    // TODO(tuning): Ravager/hoglin/zoglin head anchoring still sits slightly off the
    // body (reported as "head positions need adjustment"). Fixing this needs in-game
    // visual iteration on the yaw/pitch copy below plus possibly a model pivot offset;
    // do not guess values here without checking first- and third-person renders.
    private static void syncSpeciesHeadRotation(LivingEntity source, LivingEntity renderState) {
        if (renderState instanceof Hoglin || renderState instanceof Zoglin) {
            fixHoglinLikeHeadRotation(renderState, source);
        } else if (renderState instanceof Ravager) {
            fixRavagerHeadRotation(renderState, source);
        } else if (renderState instanceof AbstractHorse) {
            fixHorseHeadRotation(renderState, source);
        } else if (renderState instanceof SnowGolem) {
            fixSnowGolemRotation(renderState, source);
        }
    }

    private static void fixSnowGolemRotation(LivingEntity renderState, LivingEntity source) {
        float facing = source.getYRot();
        copyBodyYaw(renderState, facing, source.yRotO);
        copyHeadYaw(renderState, facing, source.yRotO);
        copyPitch(renderState, source.getXRot(), source.xRotO);
    }

    /**
     * Applies hoglin and zoglin head rotation correction.
     *
     * @param renderState the copied render state
     * @param source the source entity
     */
    private static void fixHoglinLikeHeadRotation(LivingEntity renderState, LivingEntity source) {
        copyBodyYaw(renderState, source.yBodyRot, source.yBodyRotO);
        copyHeadYaw(renderState, source.getYHeadRot(), source.yHeadRotO);
        copyPitch(renderState, source.getXRot(), source.xRotO);
    }

    /**
     * Applies ravager specific head rotation sync.
     *
     * @param renderState the ravager render state
     * @param source the morphed player
     */
    private static void fixRavagerHeadRotation(LivingEntity renderState, LivingEntity source) {
        copyHeadYaw(renderState, source.getYHeadRot(), source.yHeadRotO);
        copyPitch(renderState, source.getXRot(), source.xRotO);
    }

    /**
     * Applies horse family head and neck rotation sync.
     *
     * @param renderState the horse render state
     * @param source the morphed player
     */
    private static void fixHorseHeadRotation(LivingEntity renderState, LivingEntity source) {
        copyBodyYaw(renderState, source.yBodyRot, source.yBodyRotO);
        copyHeadYaw(renderState, source.getYHeadRot(), source.yHeadRotO);
        copyPitch(renderState, source.getXRot(), source.xRotO);
    }

    private static void copyBodyYaw(LivingEntity renderState, float yaw, float previousYaw) {
        renderState.yBodyRot = yaw;
        renderState.yBodyRotO = previousYaw;
    }

    private static void copyHeadYaw(LivingEntity renderState, float yaw, float previousYaw) {
        renderState.yHeadRot = yaw;
        renderState.yHeadRotO = previousYaw;
    }

    private static void copyPitch(LivingEntity renderState, float pitch, float previousPitch) {
        renderState.setXRot(pitch);
        renderState.xRotO = previousPitch;
    }

    private static void syncEnderDragonFlapAnimation(Entity source, EnderDragon dragonIdentity) {
        float flapTime = source.tickCount * ENDER_DRAGON_MORPH_FLAP_SPEED;
        dragonIdentity.oFlapTime = (source.tickCount - 1) * ENDER_DRAGON_MORPH_FLAP_SPEED;
        dragonIdentity.flapTime = flapTime;
    }

    public static void syncEntityAnimationState(Entity source, Entity identity, float tickDelta) {
        if (source == null || identity == null) {
            return;
        }

        int abilityAttackTicks = Math.max(
                PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY),
                PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY)
        );

        if (identity instanceof IronGolem ironGolem) {
            // TODO(tuning): swing timing vs. damage moment ("attack arm swing timing")
            // may still need an offset once verified in game. The countdown timebase
            // was moved to game time, which already removes the client/server drift
            // that made the swing start late for remote viewers.
            int golemAttackTicks = PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY);
            ((IronGolemAccessor) ironGolem).identity2$setAttackAnimationTick(golemAttackTicks);
        } else if (identity instanceof Ravager ravager) {
            int attackTicks = abilityAttackTicks;
            ((RavagerAccessor) ravager).identity2$setAttackTick(attackTicks);
        } else if (identity instanceof Hoglin hoglin) {
            int attackTicks = abilityAttackTicks;
            ((HoglinAccessor) hoglin).identity2$setAttackAnimationRemainingTicks(attackTicks);
        } else if (identity instanceof Zoglin zoglin) {
            int attackTicks = abilityAttackTicks;
            ((ZoglinAccessor) zoglin).identity2$setAttackAnimationRemainingTicks(attackTicks);
        } else if (abilityAttackTicks > 0) {
            ensureVanillaAttackAnimation(identity);
        }

        if (identity instanceof Warden warden) {
            int beamStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY);
            int attackStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY);
            syncAnimationState(warden.sonicBoomAnimationState, beamStart);
            syncAnimationState(warden.attackAnimationState, attackStart);
        }

        if (identity instanceof Camel camel) {
            boolean sitting = net.Gabou.identity2.util.NbtCompat.getBooleanOr(
                    ((EntityAccessor) source).getCustomData(),
                    PredefIdentityAbilities.CAMEL_SITTING_STATE_KEY,
                    false
            );
            if (sitting && camel.getPose() != Pose.SITTING) {
                camel.sitDown();
            } else if (!sitting && camel.getPose() == Pose.SITTING) {
                camel.standUp();
            }
            int jumpStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_JUMP_TICKS_KEY);
            syncAnimationState(camel.dashAnimationState, jumpStart);
            camel.setDashing(jumpStart > 0);
            if (jumpStart > 0) {
                CamelAccessor accessor = (CamelAccessor) camel;
                if (accessor.identity2$getDashCooldown() < 24) {
                    accessor.identity2$setDashCooldown(24);
                }
            }
        }

        if (identity instanceof Pufferfish pufferfish) {
            pufferfish.setPuffState(PredefIdentityAbilities.isSyncedAnimationActive(
                    source,
                    PredefIdentityAbilities.PUFFER_PUFF_TICKS_KEY
            ) ? 2 : 0);
        }
    }

    private static boolean ensureVanillaAttackAnimation(Entity identity) {
        if (!(identity instanceof IronGolem || identity instanceof Ravager || isHoglinFamily(identity))) {
            return false;
        }
        if (hasActiveAttackAnimation(identity)) {
            return false;
        }
        identity.handleEntityEvent((byte) 4);
        return true;
    }

    private static void syncAnimationState(AnimationState state, int startTick) {
        if (startTick > 0) {
            Integer previousStartTick = ANIMATION_START_TICKS.get(state);
            if (previousStartTick == null || previousStartTick != startTick) {
                state.stop();
                state.start(startTick);
                ANIMATION_START_TICKS.put(state, startTick);
            }
        } else {
            state.stop();
            ANIMATION_START_TICKS.remove(state);
        }
    }

    private static boolean hasActiveAttackAnimation(Entity identity) {
        if (identity instanceof IronGolem ironGolem) {
            return ((IronGolemAccessor) ironGolem).identity2$getAttackAnimationTick() > 0;
        }
        if (identity instanceof Ravager ravager) {
            return ((RavagerAccessor) ravager).identity2$getAttackTick() > 0;
        }
        if (identity instanceof Hoglin hoglin) {
            return ((HoglinAccessor) hoglin).identity2$getAttackAnimationRemainingTicks() > 0;
        }
        if (identity instanceof Zoglin zoglin) {
            return ((ZoglinAccessor) zoglin).identity2$getAttackAnimationRemainingTicks() > 0;
        }
        return false;
    }

    private static boolean isHoglinFamily(Entity identity) {
        return identity instanceof Hoglin || identity instanceof Zoglin;
    }
}
