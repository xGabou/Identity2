package net.Gabou.identity2.client;

import java.util.HashMap;
import java.util.Map;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.mixin.BeeAccessor;
import net.Gabou.identity2.mixin.CamelAccessor;
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
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.EnderMan;
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
import net.minecraft.world.phys.Vec3;

public final class IdentityRenderStateHelper {
    private static final float ENDER_DRAGON_MORPH_FLAP_SPEED = 1.0F / 10.0F;
    private static final int IRON_GOLEM_ATTACK_ANIMATION_TICKS = 10;
    private static final Map<Integer, Integer> IRON_GOLEM_ATTACK_END_TICKS = new HashMap<>();
    private static final Map<Integer, Integer> IRON_GOLEM_LAST_SWING_TIMES = new HashMap<>();
    private static final Map<Integer, Boolean> WOLF_WAS_IN_WATER = new HashMap<>();
    private static final Map<Integer, Float> WOLF_SHAKE_PROGRESS = new HashMap<>();

    private IdentityRenderStateHelper() {
    }

    public static void syncIdentityVisualState(Entity source, Entity identity) {
        syncIdentityVisualState(source, identity, 0.0F);
    }

    public static void syncIdentityVisualState(Entity source, Entity identity, float tickDelta) {
        Vec3 renderPos = (identity instanceof Warden && WardenBurrowManager.isHidden(source))
                ? WardenBurrowManager.resolveVisualBurrowPosition(source)
                : source.position();
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
                    beeAccessor.identity2$setRollAmount(0.0F);
                    beeAccessor.identity2$setRollAmountO(0.0F);
                }
                beeIdentity.setStayOutOfHiveCountdown(0);
                if (!source.onGround() || source.getDeltaMovement().y > 0.0D) {
                    beeIdentity.setRemainingPersistentAngerTime(0);
                }
            }

            if (livingIdentity instanceof Squid squidIdentity) {
                // TODO: The squid/glow squid swim loop is serviceable but still not a perfect vanilla match.
                float swimSpeed = livingSource.walkAnimation.speed();
                livingIdentity.walkAnimation.setSpeed(Math.max(0.15F, swimSpeed));
                if (source.isInWater()) {
                    livingIdentity.setSwimming(true);
                    squidIdentity.tentacleMovement = (float) (source.tickCount * 0.12F);
                    squidIdentity.oldTentacleMovement = (float) ((source.tickCount - 1) * 0.12F);
                    squidIdentity.tentacleAngle = (float) Math.sin(source.tickCount * 0.15F) * 0.25F;
                    squidIdentity.oldTentacleAngle = (float) Math.sin((source.tickCount - 1) * 0.15F) * 0.25F;
                }
            }

            if (livingIdentity instanceof Turtle) {
                LimbAnimatorAccessor targetTurtle = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
                LimbAnimatorAccessor originTurtle = (LimbAnimatorAccessor) livingSource.walkAnimation;
                float turtleScale = 0.12F;
                float slowedPosition = originTurtle.getPosition() * turtleScale;
                targetTurtle.setPrevSpeed(originTurtle.getPrevSpeed() * turtleScale);
                targetTurtle.setPosition(slowedPosition);
                targetTurtle.setPositionScale(originTurtle.getPositionScale() * turtleScale);
                livingIdentity.walkAnimation.setSpeed(livingSource.walkAnimation.speed() * turtleScale);
            }

            if (livingIdentity instanceof Axolotl && !source.isInWater()) {
                livingIdentity.setSwimming(false);
                float horizontalSpeed = (float) source.getDeltaMovement().horizontalDistance();
                float landSpeed = horizontalSpeed > 0.01F ? Math.max(livingSource.walkAnimation.speed(), horizontalSpeed * 6.0F) : 0.0F;
                livingIdentity.walkAnimation.setSpeed(landSpeed);
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
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.MAINHAND, livingSource.getItemBySlot(EquipmentSlot.MAINHAND));
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.OFFHAND, livingSource.getItemBySlot(EquipmentSlot.OFFHAND));
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.HEAD, livingSource.getItemBySlot(EquipmentSlot.HEAD));
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.CHEST, livingSource.getItemBySlot(EquipmentSlot.CHEST));
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.LEGS, livingSource.getItemBySlot(EquipmentSlot.LEGS));
            setItemSlotIfChanged(livingIdentity, EquipmentSlot.FEET, livingSource.getItemBySlot(EquipmentSlot.FEET));
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
        syncWolfWetState(source, identity);
        syncEntityAnimationState(source, identity, tickDelta);
    }

    private static void setItemSlotIfChanged(LivingEntity identity, EquipmentSlot slot, ItemStack stack) {
        ItemStack current = identity.getItemBySlot(slot);
        if (!ItemStack.matches(current, stack)) {
            identity.setItemSlot(slot, stack);
        }
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

    private static void syncWolfWetState(Entity source, Entity identity) {
        if (!(identity instanceof Wolf wolf)) {
            return;
        }

        WolfAccessor accessor = (WolfAccessor) wolf;

        int entityId = source.getId();
        boolean inWater = source.isInWaterOrBubble();
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
            shakeProgress = Math.min(1.0F, shakeProgress + 0.025F);

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
            int golemAttackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (golemAttackTicks > 0) {
                ((IronGolemAccessor) ironGolem).identity2$setAttackAnimationTick(golemAttackTicks);
            }
        } else if (identity instanceof Ravager ravager) {
            int attackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (attackTicks > 0) {
                ((RavagerAccessor) ravager).identity2$setAttackTick(attackTicks);
            }
        } else if (identity instanceof Hoglin hoglin) {
            int attackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (attackTicks > 0) {
                ((HoglinAccessor) hoglin).identity2$setAttackAnimationRemainingTicks(attackTicks);
            }
        } else if (identity instanceof Zoglin zoglin) {
            int attackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (attackTicks > 0) {
                ((ZoglinAccessor) zoglin).identity2$setAttackAnimationRemainingTicks(attackTicks);
            }
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

    public static int resolveIronGolemAttackTicks(Entity source, float tickDelta) {
        if (!(source instanceof LivingEntity livingSource)) {
            return 0;
        }
        int entityId = source.getId();
        int previousSwingTime = IRON_GOLEM_LAST_SWING_TIMES.getOrDefault(entityId, -1);
        int currentEndTick = IRON_GOLEM_ATTACK_END_TICKS.getOrDefault(entityId, 0);
        boolean attacking = livingSource.attackAnim > 0.0F || livingSource.swinging;
        boolean restartedSwing = livingSource.swinging && livingSource.swingTime <= 1 && previousSwingTime > 1;

        if (attacking && (currentEndTick <= source.tickCount || restartedSwing)) {
            currentEndTick = source.tickCount + IRON_GOLEM_ATTACK_ANIMATION_TICKS;
            IRON_GOLEM_ATTACK_END_TICKS.put(entityId, currentEndTick);
        }
        IRON_GOLEM_LAST_SWING_TIMES.put(entityId, livingSource.swingTime);

        float remaining = currentEndTick - (source.tickCount + tickDelta);
        if (remaining <= 0.0F) {
            IRON_GOLEM_ATTACK_END_TICKS.remove(entityId);
            IRON_GOLEM_LAST_SWING_TIMES.remove(entityId);
            return 0;
        }
        return Math.min(IRON_GOLEM_ATTACK_ANIMATION_TICKS, (int) Math.ceil(remaining));
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
            state.startIfStopped(startTick);
        } else {
            state.stop();
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
