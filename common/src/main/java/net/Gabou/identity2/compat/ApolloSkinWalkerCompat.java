package net.Gabou.identity2.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ApolloSkinWalkerCompat {
    private static final ResourceLocation SKIN_WALKER_ID = ResourceLocation.parse("apolloskinwalker:skin_walker");
    private static final ConcurrentMap<Class<?>, Accessors> ACCESSORS = new ConcurrentHashMap<>();

    private ApolloSkinWalkerCompat() {
    }

    public static Entity syncSkinWalkerVisualState(Entity identity, Entity source) {
        if (identity == null || source == null) {
            return identity;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(identity.getType());
        if (!SKIN_WALKER_ID.equals(id)) {
            return identity;
        }

        Accessors accessors = ACCESSORS.computeIfAbsent(identity.getClass(), ApolloSkinWalkerCompat::buildAccessors);
        if (accessors == null) {
            return identity;
        }

        try {
            boolean attacking = source instanceof LivingEntity livingSource && (livingSource.swinging || livingSource.attackAnim > 0.0F);
            double horizontalSpeed = source.getDeltaMovement().horizontalDistance();
            boolean moving = horizontalSpeed > 1.0E-6D;
            boolean sprinting = source.isSprinting();
            int age = source.tickCount;

            Object idleState = accessors.idleAnimationState().get(identity);
            Object walkState = accessors.walkAnimationState().get(identity);
            Object runState = accessors.runAnimationState().get(identity);
            Object attackState = accessors.attackAnimationState().get(identity);

            if (attacking) {
                start(accessors.startIfStopped(), attackState, age);
                stop(accessors.stop(), idleState);
                stop(accessors.stop(), walkState);
                stop(accessors.stop(), runState);
                return identity;
            }

            if (moving && sprinting) {
                start(accessors.startIfStopped(), runState, age);
                stop(accessors.stop(), idleState);
                stop(accessors.stop(), walkState);
                stop(accessors.stop(), attackState);
                return identity;
            }

            if (moving) {
                start(accessors.startIfStopped(), walkState, age);
                stop(accessors.stop(), idleState);
                stop(accessors.stop(), runState);
                stop(accessors.stop(), attackState);
                return identity;
            }

            start(accessors.startIfStopped(), idleState, age);
            stop(accessors.stop(), walkState);
            stop(accessors.stop(), runState);
            stop(accessors.stop(), attackState);
        } catch (ReflectiveOperationException ignored) {
        }

        return identity;
    }

    private static Accessors buildAccessors(Class<?> clazz) {
        Field idleAnimationState = findField(clazz, "idleAnimationState");
        Field walkAnimationState = findField(clazz, "walkAnimationState");
        Field runAnimationState = findField(clazz, "runAnimationState");
        Field attackAnimationState = findField(clazz, "attackAnimationState");
        if (idleAnimationState == null || walkAnimationState == null || runAnimationState == null || attackAnimationState == null) {
            return null;
        }

        Class<?> animationStateType = idleAnimationState.getType();
        Method startIfStopped = findMethod(animationStateType, "startIfStopped", "method_41324", int.class);
        Method stop = findMethod(animationStateType, "stop", "method_41325");
        if (startIfStopped == null || stop == null) {
            return null;
        }

        return new Accessors(idleAnimationState, walkAnimationState, runAnimationState, attackAnimationState, startIfStopped, stop);
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, null, parameterTypes);
    }

    private static Method findMethod(Class<?> clazz, String primaryName, String fallbackName, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(primaryName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException ignored) {
                if (fallbackName != null) {
                    try {
                        Method method = current.getDeclaredMethod(fallbackName, parameterTypes);
                        method.setAccessible(true);
                        return method;
                    } catch (ReflectiveOperationException ignoredToo) {
                        current = current.getSuperclass();
                        continue;
                    }
                }
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void start(Method method, Object state, int age) throws ReflectiveOperationException {
        if (method != null && state != null) {
            method.invoke(state, age);
        }
    }

    private static void stop(Method method, Object state) throws ReflectiveOperationException {
        if (method != null && state != null) {
            method.invoke(state);
        }
    }

    private record Accessors(
        Field idleAnimationState,
        Field walkAnimationState,
        Field runAnimationState,
        Field attackAnimationState,
        Method startIfStopped,
        Method stop
    ) {
    }
}
