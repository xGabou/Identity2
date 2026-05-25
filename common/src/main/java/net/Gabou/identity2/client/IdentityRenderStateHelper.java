package net.Gabou.identity2.client;

import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class IdentityRenderStateHelper {
    private static final int IRON_GOLEM_ATTACK_ANIMATION_TICKS = 10;
    private static final Map<Integer, Integer> IRON_GOLEM_ATTACK_END_TICKS = new HashMap<>();
    private static final Map<Integer, Integer> IRON_GOLEM_LAST_SWING_TIMES = new HashMap<>();

    private IdentityRenderStateHelper() {
    }

    public static void syncIdentityVisualState(Entity source, Entity identity) {
        syncIdentityVisualState(source, identity, 0.0F);
    }

    public static void syncIdentityVisualState(Entity source, Entity identity, float tickDelta) {
        identity.setPosRaw(source.position().x, source.position().y, source.position().z);
        if (identity instanceof EnderDragon) {
            identity.setYRot(source.getYRot() + 180.0F);
        } else {
            identity.setYRot(source.getYRot());
        }
        ((EntityAccessor) identity).setLastPosition(new Vec3(source.xOld, source.yOld, source.zOld));

        if (identity instanceof LivingEntity livingIdentity && source instanceof LivingEntity livingSource) {
            syncLivingHealthForRender(livingSource, livingIdentity);
            if (((LivingEntityAccessor) livingIdentity).identity2$isJumping()
                    != ((LivingEntityAccessor) livingSource).identity2$isJumping()) {
                livingIdentity.setJumping(((LivingEntityAccessor) livingSource).identity2$isJumping());
            }

            LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
            LimbAnimatorAccessor origin = (LimbAnimatorAccessor) livingSource.walkAnimation;
            target.setPrevSpeed(origin.getPrevSpeed());
            target.setSpeed(origin.getSpeed());
            target.setPosition(origin.getPosition());
            target.setPositionScale(origin.getPositionScale());

            livingIdentity.swinging = livingSource.swinging;
            livingIdentity.swingTime = livingSource.swingTime;
            livingIdentity.oAttackAnim = livingSource.oAttackAnim;
            livingIdentity.attackAnim = livingSource.attackAnim;
            livingIdentity.hurtTime = livingSource.hurtTime;
            livingIdentity.hurtDuration = livingSource.hurtDuration;
            livingIdentity.deathTime = livingSource.deathTime;
            livingIdentity.invulnerableTime = livingSource.invulnerableTime;
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
        } else if (!(identity instanceof Shulker)) {
            identity.setXRot(source.getXRot());
            identity.xRotO = source.xRotO;
        }

        if (source instanceof LivingEntity livingSource && identity instanceof LivingEntity livingIdentity) {
            livingIdentity.setItemSlot(EquipmentSlot.MAINHAND, livingSource.getItemBySlot(EquipmentSlot.MAINHAND));
            livingIdentity.setItemSlot(EquipmentSlot.OFFHAND, livingSource.getItemBySlot(EquipmentSlot.OFFHAND));
            livingIdentity.setItemSlot(EquipmentSlot.HEAD, livingSource.getItemBySlot(EquipmentSlot.HEAD));
            livingIdentity.setItemSlot(EquipmentSlot.CHEST, livingSource.getItemBySlot(EquipmentSlot.CHEST));
            livingIdentity.setItemSlot(EquipmentSlot.LEGS, livingSource.getItemBySlot(EquipmentSlot.LEGS));
            livingIdentity.setItemSlot(EquipmentSlot.FEET, livingSource.getItemBySlot(EquipmentSlot.FEET));
        }

        if (source instanceof LivingEntity livingSource && identity instanceof Mob mobIdentity) {
            mobIdentity.setAggressive(livingSource.isUsingItem());
        }

        if (source.isInWater()) {
            identity.clearFire();
            identity.setSharedFlagOnFire(false);
        } else {
            identity.setSharedFlagOnFire(identity.isOnFire());
        }
        syncEntityAnimationState(source, identity, tickDelta);
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

    private static void syncEntityAnimationState(Entity source, Entity identity, float tickDelta) {
        if (source == null || identity == null) {
            return;
        }
        if (identity instanceof IronGolem) {
            setIntFieldExact(identity, "attackAnimationTick", Math.max(
                    resolveIronGolemAttackTicks(source, tickDelta),
                    PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY)
            ));
        }

        int attackTicks = Math.max(
                PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY),
                PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY)
        );
        if (identity instanceof Ravager) {
            setIntFieldExact(identity, "attackTick", attackTicks);
        }
        if (identity instanceof Hoglin) {
            setIntFieldExact(identity, "attackAnimationRemainingTicks", attackTicks);
        }
        if (identity instanceof Warden) {
            int beamStart = (int) Math.round(PredefIdentityAbilities.getSyncedAnimationStartTick(
                    source,
                    PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY
            ));
            int attackStart = (int) Math.round(PredefIdentityAbilities.getSyncedAnimationStartTick(
                    source,
                    PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY
            ));
            syncAnimationStateField(identity, "sonicBoomAnimationState", "sonicBoomAnimation", beamStart);
            syncAnimationStateField(identity, "attackAnimationState", "attackAnimation", attackStart);
        }
        if (identity instanceof Pufferfish) {
            invokeOneArg(identity, "setPuffState", PredefIdentityAbilities.isSyncedAnimationActive(
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

    private static void syncAnimationStateField(Object owner, String fieldName, String fallbackFieldName, int startTick) {
        Object state = getFieldValue(owner, fieldName);
        if (state == null && fallbackFieldName != null) {
            state = getFieldValue(owner, fallbackFieldName);
        }
        if (state == null) {
            return;
        }
        if (startTick > 0) {
            invokeOneArg(state, "start", startTick);
        } else {
            invokeNoArg(state, "stop");
        }
    }

    private static Object getFieldValue(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static void setIntFieldExact(Object target, String fieldName, int value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (field.getType() != int.class) {
                    return;
                }
                field.setAccessible(true);
                field.setInt(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static Object invokeOneArg(Object target, String methodName, Object value) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            try {
                return method.invoke(target, value);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }
            try {
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
