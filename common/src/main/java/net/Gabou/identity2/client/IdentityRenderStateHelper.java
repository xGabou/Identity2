package net.Gabou.identity2.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

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
            if (((LivingEntityAccessor) livingIdentity).identity2$isJumping() != ((LivingEntityAccessor) livingSource).identity2$isJumping()) {
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

    private static void syncLivingHealthForRender(LivingEntity source, LivingEntity identity) {
        float sourceMaxHealth = source.getMaxHealth();
        float identityMaxHealth = identity.getMaxHealth();
        if (sourceMaxHealth <= 0.0F || identityMaxHealth <= 0.0F) {
            return;
        }
        float scaledHealth = source.getHealth() * (identityMaxHealth / sourceMaxHealth);
        identity.setHealth(Math.max(0.0F, Math.min(identityMaxHealth, scaledHealth)));
    }

    public static void syncEntityAnimationState(Entity source, Entity identity, float tickDelta) {
        if (source == null || identity == null) {
            return;
        }

        int abilityAttackTicks = Math.max(
            PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY),
            PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY)
        );

        if (identity instanceof IronGolem) {
            int golemAttackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (golemAttackTicks > 0) {
                setIntFieldExact(identity, "attackAnimationTick", golemAttackTicks);
            }
        } else if (identity instanceof Ravager) {
            int ravagerAttackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (ravagerAttackTicks > 0) {
                setIntFieldExact(identity, "attackAnimationTick", ravagerAttackTicks);
                setIntFieldExact(identity, "attackTick", ravagerAttackTicks);
                setIntFieldExact(identity, "attackAnimationRemainingTicks", ravagerAttackTicks);
                setFloatFieldMax(identity, "attackTicksRemaining", ravagerAttackTicks);
            }
        } else if (identity instanceof HoglinBase) {
            int hoglinAttackTicks = Math.max(resolveIronGolemAttackTicks(source, tickDelta), abilityAttackTicks);
            if (hoglinAttackTicks > 0) {
                setIntFieldExact(identity, "attackAnimationRemainingTicks", hoglinAttackTicks);
                setIntFieldExact(identity, "attackAnimationTick", hoglinAttackTicks);
                setIntFieldExact(identity, "attackTick", hoglinAttackTicks);
                setFloatFieldMax(identity, "attackTicksRemaining", hoglinAttackTicks);
            }
        } else if (abilityAttackTicks > 0) {
            ensureVanillaAttackAnimation(identity);
            setIntFieldMax(identity, "attackAnimationTick", abilityAttackTicks);
            setIntFieldMax(identity, "attackTick", abilityAttackTicks);
            setIntFieldMax(identity, "attackAnimationRemainingTicks", abilityAttackTicks);
            setFloatFieldMax(identity, "attackTicksRemaining", abilityAttackTicks);
        }

        if (identity instanceof Warden) {
            int beamStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY);
            int attackStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY);
            syncAnimationStateField(identity, "sonicBoomAnimationState", PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY), beamStart);
            syncAnimationStateField(identity, "attackAnimationState", PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY), attackStart);
        }

        if (identity instanceof Pufferfish) {
            int puffState = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.PUFFER_PUFF_TICKS_KEY) ? 2 : 0;
            invokeOneArg(identity, "setPuffState", puffState);
        }
    }

    private static void syncAnimationStateField(Object target, String fieldName, boolean active, int startTick) {
        Object state = getFieldValue(target, fieldName);
        if (state == null) {
            return;
        }
        if (active) {
            invokeOneArg(state, "startIfStopped", startTick);
        } else {
            invokeNoArg(state, "stop");
        }
    }

    private static void setIntFieldExact(Object target, String fieldName, int value) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != int.class && field.getType() != Integer.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                field.setInt(target, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void setIntFieldMax(Object target, String fieldName, int minValue) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != int.class && field.getType() != Integer.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                int currentValue = field.getInt(target);
                if (currentValue < minValue) {
                    field.setInt(target, minValue);
                }
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void setFloatFieldMax(Object target, String fieldName, float minValue) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != float.class && field.getType() != Float.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                float currentValue = field.getFloat(target);
                if (currentValue < minValue) {
                    field.setFloat(target, minValue);
                }
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean ensureVanillaAttackAnimation(Entity identity) {
        if (!(identity instanceof IronGolem || identity instanceof Ravager || identity instanceof HoglinBase)) {
            return false;
        }
        if (hasActiveAttackAnimation(identity)) {
            return false;
        }
        return invokeHandleEntityEvent(identity, (byte) 4);
    }

    private static boolean hasActiveAttackAnimation(Object target) {
        return getIntFieldValue(target, "attackAnimationTick") > 0
            || getIntFieldValue(target, "attackTick") > 0
            || getIntFieldValue(target, "attackAnimationRemainingTicks") > 0
            || getFloatFieldValue(target, "attackTicksRemaining") > 0;
    }

    private static int getIntFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return 0;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != int.class && field.getType() != Integer.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                return field.getInt(target);
            } catch (Throwable ignored) {
            }
        }
        Object viaGetter = invokeNoArg(target, getterNameForField(fieldName));
        if (viaGetter instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static float getFloatFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return 0.0F;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != float.class && field.getType() != Float.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                return field.getFloat(target);
            } catch (Throwable ignored) {
            }
        }
        Object viaGetter = invokeNoArg(target, getterNameForField(fieldName));
        if (viaGetter instanceof Number number) {
            return number.floatValue();
        }
        return 0.0F;
    }

    private static String getterNameForField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return "";
        }
        return switch (fieldName) {
            case "attackAnimationTick" -> "getAttackAnimationTick";
            case "attackTick" -> "getAttackTick";
            case "attackAnimationRemainingTicks" -> "getAttackAnimationRemainingTicks";
            case "attackTicksRemaining" -> "getAttackTicksRemaining";
            default -> "";
        };
    }

    private static boolean invokeHandleEntityEvent(Object target, byte eventId) {
        if (target == null) {
            return false;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals("handleEntityEvent") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> param = method.getParameterTypes()[0];
                if (param != byte.class && param != Byte.class) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    method.invoke(target, eventId);
                    return true;
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static Object getFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                return field.get(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
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
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> param = method.getParameterTypes()[0];
                if (arg != null && !(param.isAssignableFrom(arg.getClass()) || (param == int.class && arg instanceof Integer))) {
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
        }
        return null;
    }
}
