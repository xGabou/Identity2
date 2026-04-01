package net.Gabou.identity2.compat;

import dev.architectury.platform.Platform;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.api.variant.IdentityVariantAdapter;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class UntamedWildsCompat {
    private static final String MOD_ID = "untamedwilds";
    private static final Set<EntityType<?>> REGISTERED_TYPES = ConcurrentHashMap.newKeySet();
    private static final Class<?> COMPLEX_MOB_CLASS = findClass("untamedwilds.entity.ComplexMob");
    private static final Method GET_ENTITY_DATA_METHOD = findMethod(COMPLEX_MOB_CLASS, "getEntityData", EntityType.class);
    private static final Method GET_VARIANT_METHOD = findMethod(COMPLEX_MOB_CLASS, "getVariant");
    private static final Method GET_SKIN_METHOD = findMethod(COMPLEX_MOB_CLASS, "getSkin");
    private static final Method GET_SIZE_METHOD = findMethod(COMPLEX_MOB_CLASS, "getMobSize");
    private static final Method GET_GENDER_METHOD = findMethod(COMPLEX_MOB_CLASS, "getGender");
    private static final Method SET_VARIANT_METHOD = findMethod(COMPLEX_MOB_CLASS, "setVariant", int.class);
    private static final Method SET_SKIN_METHOD = findMethod(COMPLEX_MOB_CLASS, "setSkin", int.class);
    private static final Method SET_SIZE_METHOD = findMethod(COMPLEX_MOB_CLASS, "setMobSize", float.class);
    private static final Method SET_GENDER_METHOD = findMethod(COMPLEX_MOB_CLASS, "setGender", int.class);
    private static final Method UPDATE_ATTRIBUTES_METHOD = findMethod(findClass("untamedwilds.entity.INeedsPostUpdate"), "updateAttributes");
    private static final Method HOLDER_GET_SPECIES_DATA_METHOD = findMethod(findClass("untamedwilds.util.EntityDataHolder"), "getSpeciesData");
    private static final Method HOLDER_GET_NAME_METHOD = findMethod(findClass("untamedwilds.util.EntityDataHolder"), "getName", int.class);
    private static final Method HOLDER_GET_FLAGS_METHOD = findMethod(findClass("untamedwilds.util.EntityDataHolder"), "getFlags", int.class, String.class);
    private static final Method HOLDER_GET_SKINS_METHOD = findMethod(findClass("untamedwilds.util.EntityDataHolder"), "getSkins", int.class);

    private static final IdentityVariantAdapter VARIANT_ADAPTER = new IdentityVariantAdapter() {
        @Override
        public CompoundTag extractVariantData(LivingEntity entity) {
            return extractComplexMobVariant(entity);
        }

        @Override
        public void applyVariantData(Entity entity, CompoundTag variantNbt) {
            applyComplexMobVariant(entity, variantNbt);
        }

        @Override
        public List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
            return discoverComplexMobVariants(type);
        }
    };

    private UntamedWildsCompat() {
    }

    public static boolean isSupportedIdentityType(EntityType<?> type) {
        if (!Platform.isModLoaded(MOD_ID) || type == null) {
            return false;
        }
        ResourceLocation id = EntityType.getKey(type);
        if (id == null || !MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        return resolveEntityDataHolder(type) != null || hasEntityDataResource(id);
    }

    public static void ensureVariantAdapterRegistered(EntityType<?> type) {
        if (!isSupportedIdentityType(type)) {
            return;
        }
        if (!REGISTERED_TYPES.add(type)) {
            return;
        }
        IdentityApi.registerVariantAdapter(type, VARIANT_ADAPTER);
    }

    private static CompoundTag extractComplexMobVariant(LivingEntity entity) {
        CompoundTag out = new CompoundTag();
        if (!isComplexMobInstance(entity)) {
            return out;
        }
        Integer variant = invokeInt(GET_VARIANT_METHOD, entity);
        Integer skin = invokeInt(GET_SKIN_METHOD, entity);
        Float size = invokeFloat(GET_SIZE_METHOD, entity);
        Integer gender = invokeInt(GET_GENDER_METHOD, entity);
        if (variant != null) {
            out.putInt("Variant", variant);
        }
        if (skin != null) {
            out.putInt("Skin", skin);
        }
        if (size != null) {
            out.putFloat("Size", size);
        }
        if (gender != null) {
            out.putInt("Gender", gender);
        }
        return out;
    }

    private static void applyComplexMobVariant(Entity entity, CompoundTag variantNbt) {
        if (!isComplexMobInstance(entity) || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        if (variantNbt.contains("Variant")) {
            invoke(SET_VARIANT_METHOD, entity, variantNbt.getInt("Variant"));
        }
        if (variantNbt.contains("Skin")) {
            invoke(SET_SKIN_METHOD, entity, variantNbt.getInt("Skin"));
        }
        if (variantNbt.contains("Size")) {
            invoke(SET_SIZE_METHOD, entity, variantNbt.getFloat("Size"));
        }
        if (variantNbt.contains("Gender")) {
            invoke(SET_GENDER_METHOD, entity, variantNbt.getInt("Gender"));
        }
        invoke(UPDATE_ATTRIBUTES_METHOD, entity);
    }

    private static List<IdentityVariant> discoverComplexMobVariants(EntityType<?> type) {
        if (!isSupportedIdentityType(type) || HOLDER_GET_NAME_METHOD == null) {
            return List.of();
        }
        ResourceLocation id = EntityType.getKey(type);
        if (id == null) {
            return List.of();
        }
        Object holder = resolveEntityDataHolder(type);
        int speciesCount = getSpeciesCount(holder);
        if (speciesCount <= 0) {
            return List.of();
        }
        List<IdentityVariant> variants = new ArrayList<>(speciesCount);
        for (int variantIndex = 0; variantIndex < speciesCount; variantIndex++) {
            String speciesName = invokeString(HOLDER_GET_NAME_METHOD, holder, variantIndex);
            String displayName = formatSpeciesName(speciesName, variantIndex);
            int skinCount = getSkinCount(holder, variantIndex);
            boolean dimorphic = getFlagValue(holder, variantIndex, "dimorphism") != 0;
            if (dimorphic) {
                variants.add(new IdentityVariant(id, displayName + " Male", createVariantNbt(variantIndex, skinCount, 0)));
                variants.add(new IdentityVariant(id, displayName + " Female", createVariantNbt(variantIndex, skinCount, 1)));
                continue;
            }
            variants.add(new IdentityVariant(id, displayName, createVariantNbt(variantIndex, skinCount, null)));
        }
        return variants;
    }

    private static int getSpeciesCount(Object holder) {
        if (holder == null || HOLDER_GET_SPECIES_DATA_METHOD == null) {
            return 0;
        }
        Object value = invoke(HOLDER_GET_SPECIES_DATA_METHOD, holder);
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private static int getFlagValue(Object holder, int variantIndex, String flagName) {
        Integer value = invokeInt(HOLDER_GET_FLAGS_METHOD, holder, variantIndex, flagName);
        return value == null ? 0 : value;
    }

    private static int getSkinCount(Object holder, int variantIndex) {
        Integer value = invokeInt(HOLDER_GET_SKINS_METHOD, holder, variantIndex);
        return value == null ? 0 : value;
    }

    private static CompoundTag createVariantNbt(int variantIndex, int skinCount, Integer gender) {
        CompoundTag variantNbt = new CompoundTag();
        variantNbt.putInt("Variant", variantIndex);
        if (skinCount > 0) {
            variantNbt.putInt("Skin", 0);
        }
        if (gender != null) {
            variantNbt.putInt("Gender", gender);
        }
        return variantNbt;
    }

    private static boolean isComplexMobInstance(Object entity) {
        return entity != null && COMPLEX_MOB_CLASS != null && COMPLEX_MOB_CLASS.isInstance(entity);
    }

    private static Object resolveEntityDataHolder(EntityType<?> type) {
        if (type == null || GET_ENTITY_DATA_METHOD == null) {
            return null;
        }
        return invoke(GET_ENTITY_DATA_METHOD, null, type);
    }

    private static boolean hasEntityDataResource(ResourceLocation id) {
        if (id == null || !MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        String resourcePath = "data/" + MOD_ID + "/entities/" + id.getPath() + ".json";
        ClassLoader loader = UntamedWildsCompat.class.getClassLoader();
        if (loader != null && loader.getResource(resourcePath) != null) {
            return true;
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null && contextLoader.getResource(resourcePath) != null;
    }

    private static String formatSpeciesName(String speciesName, int fallbackIndex) {
        if (speciesName == null || speciesName.isBlank()) {
            return "Variant " + fallbackIndex;
        }
        String normalized = speciesName.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "Variant " + fallbackIndex;
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? "Variant " + fallbackIndex : builder.toString();
    }

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer invokeInt(Method method, Object target, Object... args) {
        Object result = invoke(method, target, args);
        return result instanceof Number number ? number.intValue() : null;
    }

    private static Float invokeFloat(Method method, Object target, Object... args) {
        Object result = invoke(method, target, args);
        return result instanceof Number number ? number.floatValue() : null;
    }

    private static String invokeString(Method method, Object target, Object... args) {
        Object result = invoke(method, target, args);
        return result instanceof String string ? string : null;
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            Method method = owner.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
