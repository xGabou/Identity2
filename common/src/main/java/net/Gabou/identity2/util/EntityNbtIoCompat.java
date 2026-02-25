package net.Gabou.identity2.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class EntityNbtIoCompat {
    private static final String VALUE_OUTPUT_CLASS = "net.minecraft.world.level.storage.ValueOutput";
    private static final String VALUE_INPUT_CLASS = "net.minecraft.world.level.storage.ValueInput";

    private EntityNbtIoCompat() {
    }

    public static CompoundTag saveWithoutId(@Nullable Entity entity) {
        if (entity == null) {
            return new CompoundTag();
        }

        CompoundTag direct = trySaveWithCompoundTag(entity);
        if (direct != null) {
            return direct;
        }

        CompoundTag fromValueOutput = trySaveWithValueOutput(entity);
        return fromValueOutput == null ? new CompoundTag() : fromValueOutput;
    }

    public static boolean load(@Nullable Entity entity, @Nullable CompoundTag nbt, @Nullable Object registryAccess) {
        if (entity == null || nbt == null) {
            return false;
        }

        if (tryLoadWithCompoundTag(entity, nbt)) {
            return true;
        }
        return tryLoadWithValueInput(entity, nbt, registryAccess);
    }

    private static CompoundTag trySaveWithCompoundTag(Entity entity) {
        Method method = findMethod(entity.getClass(), "saveWithoutId", CompoundTag.class);
        if (method == null) {
            return null;
        }
        try {
            CompoundTag out = new CompoundTag();
            Object result = method.invoke(entity, out);
            if (result instanceof CompoundTag tag) {
                return tag;
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CompoundTag trySaveWithValueOutput(Entity entity) {
        Method saveMethod = findSingleArgMethod(entity.getClass(), "saveWithoutId", VALUE_OUTPUT_CLASS);
        if (saveMethod == null) {
            return null;
        }
        Object registryAccess = extractRegistryAccess(entity);
        Object valueOutput = createTagValueOutput(registryAccess);
        if (valueOutput == null) {
            return null;
        }
        try {
            saveMethod.invoke(entity, valueOutput);
            return extractBuiltCompound(valueOutput);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean tryLoadWithCompoundTag(Entity entity, CompoundTag nbt) {
        Method loadMethod = findMethod(entity.getClass(), "load", CompoundTag.class);
        if (loadMethod == null) {
            return false;
        }
        try {
            loadMethod.invoke(entity, nbt);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryLoadWithValueInput(Entity entity, CompoundTag nbt, @Nullable Object registryAccess) {
        Method loadMethod = findSingleArgMethod(entity.getClass(), "load", VALUE_INPUT_CLASS);
        if (loadMethod == null) {
            return false;
        }
        Object valueInput = createTagValueInput(registryAccess, nbt);
        if (valueInput == null) {
            return false;
        }
        try {
            loadMethod.invoke(entity, valueInput);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Object extractRegistryAccess(Entity entity) {
        try {
            Object level = entity.level();
            if (level == null) {
                return null;
            }
            Method registryAccess = level.getClass().getMethod("registryAccess");
            return registryAccess.invoke(level);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object createTagValueOutput(@Nullable Object registryAccess) {
        try {
            Class<?> outputClass = Class.forName("net.minecraft.world.level.storage.TagValueOutput");
            Object reporter = resolveDiscardingProblemReporter();
            if (reporter == null || registryAccess == null) {
                return null;
            }
            for (Method method : outputClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!method.getName().equals("createWithContext") || method.getParameterCount() != 2) {
                    continue;
                }
                return method.invoke(null, reporter, registryAccess);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static Object createTagValueInput(@Nullable Object registryAccess, CompoundTag nbt) {
        try {
            Class<?> inputClass = Class.forName("net.minecraft.world.level.storage.TagValueInput");
            Object reporter = resolveDiscardingProblemReporter();
            if (reporter == null || registryAccess == null) {
                return null;
            }
            for (Method method : inputClass.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!method.getName().equals("create") || method.getParameterCount() != 3) {
                    continue;
                }
                return method.invoke(null, reporter, registryAccess, nbt);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static Object resolveDiscardingProblemReporter() {
        try {
            Class<?> reporterClass = Class.forName("net.minecraft.util.ProblemReporter");
            Field discardingField = reporterClass.getField("DISCARDING");
            return discardingField.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static CompoundTag extractBuiltCompound(Object valueOutput) {
        if (valueOutput == null) {
            return null;
        }
        try {
            Method buildResult = valueOutput.getClass().getMethod("buildResult");
            Object built = buildResult.invoke(valueOutput);
            if (built instanceof CompoundTag tag) {
                return tag;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?> paramType) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, paramType);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Method findSingleArgMethod(Class<?> type, String name, String paramTypeName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].getName().equals(paramTypeName)) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}

