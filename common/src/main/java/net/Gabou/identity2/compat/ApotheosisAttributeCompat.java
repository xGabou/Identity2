package net.Gabou.identity2.compat;

import dev.architectury.platform.Platform;
import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime bridge for Apotheosis / AttributesLib attribute-map hooks.
 *
 * This class intentionally uses reflection so common code does not hard-link
 * Forge-only classes and remains safe on Fabric.
 */
public final class ApotheosisAttributeCompat {
    private static final boolean ENABLED = Platform.isModLoaded("apotheosis")
        || Platform.isModLoaded("attributeslib")
        || Platform.isModLoaded("apothic_attributes");

    @Nullable
    private static final Class<?> I_ENTITY_OWNED_CLASS = load("dev.shadowsoffire.attributeslib.util.IEntityOwned");
    @Nullable
    private static final Class<?> I_ATTRIBUTE_MANAGER_CLASS = load("dev.shadowsoffire.attributeslib.util.IAttributeManager");
    @Nullable
    private static final Method SET_OWNER_METHOD = find(I_ENTITY_OWNED_CLASS, "setOwner", LivingEntity.class);
    @Nullable
    private static final Method SET_ATTRIBUTES_UPDATING_METHOD = find(I_ATTRIBUTE_MANAGER_CLASS, "setAttributesUpdating", boolean.class);

    private ApotheosisAttributeCompat() {
    }

    public static void beginAttributeUpdate(@Nullable AttributeMap map) {
        setAttributesUpdating(map, true);
    }

    public static void endAttributeUpdate(@Nullable AttributeMap map) {
        setAttributesUpdating(map, false);
    }

    public static void setOwner(@Nullable AttributeMap map, @Nullable LivingEntity owner) {
        if (!ENABLED || map == null || owner == null || I_ENTITY_OWNED_CLASS == null || SET_OWNER_METHOD == null) {
            return;
        }
        if (!I_ENTITY_OWNED_CLASS.isInstance(map)) {
            return;
        }
        try {
            SET_OWNER_METHOD.invoke(map, owner);
        } catch (Throwable ignored) {
        }
    }

    private static void setAttributesUpdating(@Nullable AttributeMap map, boolean updating) {
        if (!ENABLED || map == null || I_ATTRIBUTE_MANAGER_CLASS == null || SET_ATTRIBUTES_UPDATING_METHOD == null) {
            return;
        }
        if (!I_ATTRIBUTE_MANAGER_CLASS.isInstance(map)) {
            return;
        }
        try {
            SET_ATTRIBUTES_UPDATING_METHOD.invoke(map, updating);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static Class<?> load(String name) {
        if (!ENABLED) {
            return null;
        }
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method find(@Nullable Class<?> owner, String methodName, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(methodName, params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
