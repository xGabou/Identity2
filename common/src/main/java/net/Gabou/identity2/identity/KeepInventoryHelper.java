package net.Gabou.identity2.identity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

public final class KeepInventoryHelper {
    private KeepInventoryHelper() {
    }

    public static boolean isKeepInventoryEnabled(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Object gameRules = invokeNoArg(serverLevel, "getGameRules");
        if (gameRules == null) {
            return false;
        }

        Object keepInventoryRule = resolveGameRule("minecraft:keepInventory");
        if (keepInventoryRule == null) {
            keepInventoryRule = resolveGameRule("minecraft:keep_inventory");
        }
        if (keepInventoryRule == null) {
            keepInventoryRule = resolveGameRule("keepInventory");
        }
        if (keepInventoryRule == null) {
            keepInventoryRule = resolveGameRule("keep_inventory");
        }
        if (keepInventoryRule == null) {
            return false;
        }

        Object value = invokeSingleArg(gameRules, "getBoolean", keepInventoryRule);
        if (value instanceof Boolean bool) {
            return bool;
        }

        value = invokeSingleArg(gameRules, "get", keepInventoryRule);
        return value instanceof Boolean bool && bool;
    }

    @Nullable
    private static Object resolveGameRule(String identifierString) {
        try {
            Object registry = BuiltInRegistries.class.getField("GAME_RULE").get(null);
            ResourceLocation identityId = ResourceLocation.parse(identifierString);

            Object value = invokeSingleArg(registry, "get", identityId);
            if (value != null) {
                return unwrapOptional(value);
            }

            value = invokeSingleArg(registry, "getOptional", identityId);
            if (value != null) {
                return unwrapOptional(value);
            }

            value = invokeSingleArg(registry, "byNameOrThrow", identityId);
            if (value != null) {
                return value;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object invokeSingleArg(Object target, String methodName, Object argument) {
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                return method.invoke(target, argument);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
