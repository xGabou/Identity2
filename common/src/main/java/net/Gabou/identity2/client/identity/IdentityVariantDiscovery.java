package net.Gabou.identity2.client.identity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.Gabou.identity2.identity.IdentityVanillaVariantHelper;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;

public final class IdentityVariantDiscovery {
    private static final List<String> CANDIDATE_KEYS = List.of(
            "Color",
            "Variant",
            "variant",
            "Type",
            "type",
            "Skin",
            "skin",
            "Form",
            "form",
            "IsBaby",
            "Baby",
            "VillagerLevel"
    );
    private static final Set<String> VARIANT_NAME_HINTS = Set.of(
            "variant",
            "type",
            "color",
            "skin",
            "form",
            "pattern",
            "phase",
            "breed",
            "style",
            "coat",
            "mark"
    );
    private static final Set<String> AXIS_NAME_EXCLUDE = Set.of(
            "id",
            "uuid",
            "position",
            "x",
            "y",
            "z",
            "health",
            "maxhealth",
            "air",
            "team",
            "name",
            "customname",
            "target",
            "owner",
            "soundvariant",
            "age",
            "pose"
    );
    private static final int MAX_NUMERIC_VARIANT_VALUE = 31;
    private static final int MAX_REFLECTIVE_VARIANTS = 256;
    private static final int MAX_DYNAMIC_SAMPLE_ENTITIES = 48;
    private static final int MAX_DYNAMIC_REGISTRY_VALUES = 128;
    private static final Set<String> NON_VARIANT_ROOT_KEYS = Set.of("Age", "AgeLocked", "EggLayTime");
    private static final Set<String> BLACKLISTED_VARIANT_EXACT = Set.of(
            "block_activate",
            "block_attach",
            "block_change",
            "block_close",
            "block_deactivate",
            "block_destroy",
            "block_detach",
            "block_open",
            "block_place",
            "container_close",
            "container_open",
            "drink",
            "eat",
            "elytra_glide",
            "entity_action",
            "entity_damage",
            "entity_die",
            "entity_dismount",
            "entity_interact",
            "entity_mount",
            "entity_place",
            "equip",
            "explode",
            "flap",
            "fluid_pickup",
            "fluid_place",
            "hit_ground",
            "instrument_play",
            "item_interact_finish",
            "item_interact_start",
            "jukebox_play",
            "jukebox_stop_play",
            "lightning_strike",
            "note_block_play",
            "prime_fuse",
            "projectile_land",
            "projectile_shoot",
            "sculk_sensor_tendrils_clicking",
            "shear",
            "shriek",
            "splash",
            "swim",
            "teleport",
            "unequip"
    );
    private static final Set<String> BLACKLISTED_VARIANT_PREFIXES = Set.of(
            "resonate_",
            "block_",
            "entity_",
            "projectile_",
            "item_interact_",
            "container_",
            "fluid_",
            "jukebox_",
            "note_block_",
            "sculk_"
    );

    private IdentityVariantDiscovery() {
    }

    public static List<IdentityVariant> discover(EntityType<?> type, ClientLevel world) {
        if (type == null || world == null) {
            Identity2.LOGGER.info("[VariantDebug] discover skipped: type={} world={}", type, world);
            return List.of(defaultVariant(type));
        }

        try {
            ResourceLocation typeId = EntityType.getKey(type);
            if (typeId == null) {
                Identity2.LOGGER.info("[VariantDebug] discover missing type id for {}", type);
                return List.of(defaultVariant(type));
            }

            Map<String, IdentityVariant> out = new LinkedHashMap<>();
            List<IdentityVariant> adapterVariants = IdentityApi.discoverVariants(type, world);
            for (IdentityVariant variant : adapterVariants) {
                addVariant(out, variant);
            }

            List<IdentityVariant> known = discoverKnownVariants(type, typeId, world);
            for (IdentityVariant variant : known) {
                addVariant(out, variant);
            }

            // If a custom adapter provides explicit variants for this entity type,
            // treat it as authoritative to avoid adapter + heuristic duplicate entries.
            if (!adapterVariants.isEmpty()) {
                ensureDefaultVariantWhenBabyPresent(typeId, out);
                if (out.isEmpty()) {
                    return List.of(defaultVariant(typeId));
                }
                return new ArrayList<>(out.values());
            }

            for (IdentityVariant variant : discoverSampledVariants(type, typeId, world)) {
                addVariant(out, variant);
            }
            for (IdentityVariant variant : discoverGenericVariants(type, typeId, world)) {
                addVariant(out, variant);
            }
            List<IdentityVariant> current = new ArrayList<>(out.values());
            for (IdentityVariant variant : discoverBabyVariantsFromExisting(type, typeId, world, current)) {
                addVariant(out, variant);
            }
            for (IdentityVariant variant : discoverBabyVariants(type, typeId, world)) {
                addVariant(out, variant);
            }
            ensureDefaultVariantWhenBabyPresent(typeId, out);

            Identity2.LOGGER.info(
                    "[VariantDebug] discovered {} variants for {} (adapter={}, world={})",
                    out.size(),
                    typeId,
                    adapterVariants.size(),
                    world.dimension().location()
            );
            if (out.isEmpty()) {
                return List.of(defaultVariant(typeId));
            }
            return new ArrayList<>(out.values());
        } catch (Throwable ignored) {
            Identity2.LOGGER.warn("[VariantDebug] discover failed for {}", type, ignored);
            return List.of(defaultVariant(type));
        }
    }

    private static void addVariant(Map<String, IdentityVariant> out, IdentityVariant variant) {
        if (out == null || variant == null) {
            return;
        }
        CompoundTag sanitized = sanitizeVariantNbt(variant.variantNbt(), true);
        if (sanitized == null || sanitized.isEmpty()) {
            return;
        }
        IdentityVariant normalized = new IdentityVariant(variant.entityTypeId(), variant.displayName(), sanitized);
        String token = IdentityProgression.toVariantUnlockToken(normalized.variantNbt());
        out.putIfAbsent(token, normalized);
    }

    private static void ensureDefaultVariantWhenBabyPresent(ResourceLocation typeId, Map<String, IdentityVariant> out) {
        if (typeId == null || out == null || out.isEmpty()) {
            return;
        }
        if (out.containsKey("-")) {
            return;
        }
        for (IdentityVariant variant : out.values()) {
            if (variant == null) {
                continue;
            }
            CompoundTag nbt = variant.variantNbt();
            if (nbt == null || nbt.isEmpty()) {
                continue;
            }
            boolean babyByFlag = NbtCompat.getBooleanOr(nbt, "IsBaby", false);
            boolean babyByAlias = NbtCompat.getBooleanOr(nbt, "Baby", false);
            boolean babyByAge = nbt.contains("Age", Tag.TAG_ANY_NUMERIC) && nbt.getInt("Age") < 0;
            if (babyByFlag || babyByAlias || babyByAge) {
                out.put("-", defaultVariant(typeId));
                return;
            }
        }
    }

    private static List<IdentityVariant> discoverKnownVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        if (type == null || typeId == null) {
            return List.of();
        }
        return IdentityVanillaVariantHelper.discoverVariants(type, world);
    }

    private static List<IdentityVariant> discoverDataDrivenRegistryVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        Entity baselineEntity = createEntity(type, world);
        if (baselineEntity == null) {
            return List.of();
        }
        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baselineEntity);
        if (baselineData == null || baselineData.isEmpty()) {
            return List.of();
        }

        List<Method> methods = getAllMethods(baselineEntity.getClass());
        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (Method getter : methods) {
            if (getter.getParameterCount() != 0 || getter.getReturnType() == void.class) {
                continue;
            }
            String suffix = extractGetterSuffix(getter.getName());
            if (suffix == null || suffix.isBlank()) {
                continue;
            }
            String lowered = suffix.toLowerCase(Locale.ROOT);
            if (!lowered.contains("variant")) {
                continue;
            }
            if (AXIS_NAME_EXCLUDE.contains(lowered)) {
                continue;
            }

            Method setter = findSetter(methods, suffix);
            if (setter == null) {
                continue;
            }

            Object rawBaseline = invokeNoArg(baselineEntity, getter);
            if (!(rawBaseline instanceof Holder.Reference<?> ref)) {
                continue;
            }
            ResourceKey<?> valueKey = ref.key();
            ResourceKey<? extends Registry<?>> registryKey = ResourceKey.createRegistryKey(valueKey.registry());
            net.minecraft.core.Registry<?> registry = world.registryAccess().registryOrThrow(registryKey);
            List<net.minecraft.resources.ResourceKey<?>> keys = new ArrayList<>(registry.registryKeySet());
            keys.sort(Comparator.comparing(k -> k.location().toString(), String.CASE_INSENSITIVE_ORDER));
            Object baselineValue = unwrapHolderValue(rawBaseline);
            VariantAxis axis = new VariantAxis(suffix, getter, setter, baselineValue, registry);
            int count = 0;
            for (ResourceKey<?> tp : keys) {
                if (count >= MAX_DYNAMIC_REGISTRY_VALUES) {
                    break;
                }
                ResourceLocation key = tp.location();
                Object candidate = getRegistryValue(registry, key);
                if (candidate == null) {
                    continue;
                }
                if (areEquivalent(axis.baselineValue(), candidate, registry)) {
                    continue;
                }

                Entity candidateEntity = createEntity(type, world);
                if (candidateEntity == null || !applyAxisCandidate(axis, candidateEntity, candidate)) {
                    continue;
                }
                CompoundTag fullData = IdentityVariantNbtHelper.writeEntityData(candidateEntity);
                if (fullData == null || fullData.isEmpty()) {
                    continue;
                }

                CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, fullData);
                if (diff.isEmpty()) {
                    continue;
                }

                String label = capitalize(typeId.getPath().replace('_', ' ')) + " " + capitalize(key.getPath().replace('_', ' '));
                addVariant(out, new IdentityVariant(typeId, label, diff));
                count++;
            }
        }

        return new ArrayList<>(out.values());
    }

    private static List<IdentityVariant> discoverRegistryBackedVariants(
            ResourceLocation typeId,
            String registryField,
            String variantKey,
            String labelPrefix
    ) {
        Registry<?> registry = resolveRegistry(registryField);
        if (registry == null || registry.keySet().isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> keys = new ArrayList<>(registry.keySet());
        keys.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        List<IdentityVariant> variants = new ArrayList<>(keys.size());
        for (ResourceLocation variantId : keys) {
            if (variantId == null) {
                continue;
            }
            CompoundTag nbt = new CompoundTag();
            nbt.putString(variantKey, variantId.toString());
            variants.add(new IdentityVariant(typeId, labelPrefix + " " + capitalize(variantId.getPath().replace('_', ' ')), nbt));
        }
        return variants;
    }

    private static Registry<?> resolveRegistry(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Object direct = BuiltInRegistries.class.getField(fieldName).get(null);
            if (direct instanceof Registry<?> registry) {
                return registry;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object key = Registries.class.getField(fieldName).get(null);
            if (key instanceof net.minecraft.resources.ResourceKey<?> resourceKey) {
                ResourceLocation location = resolveResourceKeyLocation(resourceKey);
                if (location != null) {
                    Object value = BuiltInRegistries.REGISTRY.get(location);
                    if (value instanceof Registry<?> registry) {
                        return registry;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation resolveResourceKeyLocation(Object resourceKey) {
        Object direct = invokeNoArg(resourceKey, "location");
        if (direct instanceof ResourceLocation id) {
            return id;
        }
        Object byResourceLocation = invokeNoArg(resourceKey, "identifier");
        if (byResourceLocation instanceof ResourceLocation id) {
            return id;
        }
        Object byRegistry = invokeNoArg(resourceKey, "registry");
        if (byRegistry instanceof ResourceLocation id) {
            return id;
        }
        return null;
    }

    private static List<IdentityVariant> discoverReflectiveVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        Entity baselineEntity = createEntity(type, world);
        if (baselineEntity == null) {
            return List.of();
        }
        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baselineEntity);
        if (baselineData == null || baselineData.isEmpty()) {
            return List.of();
        }

        List<VariantAxis> axes = findVariantAxes(baselineEntity);
        if (axes.isEmpty()) {
            return List.of();
        }

        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (VariantAxis axis : axes) {
            for (Object candidate : collectAxisCandidates(axis, type, world)) {
                if (areEquivalent(axis.baselineValue(), candidate, axis.registry())) {
                    continue;
                }
                Entity candidateEntity = createEntity(type, world);
                if (candidateEntity == null || !applyAxisCandidate(axis, candidateEntity, candidate)) {
                    continue;
                }
                CompoundTag fullData = IdentityVariantNbtHelper.writeEntityData(candidateEntity);
                if (fullData == null || fullData.isEmpty()) {
                    continue;
                }
                CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, fullData);
                if (diff.isEmpty()) {
                    continue;
                }

                addVariant(out, new IdentityVariant(typeId, buildAxisLabel(typeId, axis, candidate), diff));
                if (out.size() >= MAX_REFLECTIVE_VARIANTS) {
                    return new ArrayList<>(out.values());
                }
            }
        }

        return new ArrayList<>(out.values());
    }

    private static List<VariantAxis> findVariantAxes(Entity entity) {
        if (entity == null) {
            return List.of();
        }
        List<VariantAxis> axes = new ArrayList<>();
        List<Method> methods = getAllMethods(entity.getClass());
        Set<String> seen = new LinkedHashSet<>();
        for (Method getter : methods) {
            if (getter.getParameterCount() != 0 || getter.getReturnType() == void.class) {
                continue;
            }
            String suffix = extractGetterSuffix(getter.getName());
            if (suffix == null || suffix.isBlank()) {
                continue;
            }

            String lowered = suffix.toLowerCase(Locale.ROOT);
            if (AXIS_NAME_EXCLUDE.contains(lowered)) {
                continue;
            }
            boolean likelyByName = isLikelyVariantName(lowered);
            boolean likelyByType = getter.getReturnType().isEnum();
            if (!likelyByName && !likelyByType) {
                continue;
            }

            Method setter = findSetter(methods, suffix);
            if (setter == null) {
                continue;
            }

            Object rawBaseline = invokeNoArg(entity, getter);
            Object baseline = unwrapHolderValue(rawBaseline);
            Registry<?> registry = resolveRegistryForAxis(rawBaseline, baseline, getter, setter);
            if (baseline == null && registry == null) {
                continue;
            }
            if (!isSupportedAxisType(setter.getParameterTypes()[0], baseline, registry)) {
                continue;
            }

            if (seen.add(suffix)) {
                axes.add(new VariantAxis(suffix, getter, setter, baseline, registry));
            }
        }
        return axes;
    }

    private static boolean isSupportedAxisType(Class<?> setterParam, Object baseline, Registry<?> registry) {
        if (setterParam == null) {
            return false;
        }
        if (baseline == null) {
            return registry != null;
        }
        if (isAssignable(setterParam, baseline.getClass())) {
            return true;
        }
        if (baseline instanceof Number) {
            return setterParam.isPrimitive() || Number.class.isAssignableFrom(setterParam);
        }
        if (baseline instanceof Boolean) {
            return setterParam == boolean.class || setterParam == Boolean.class;
        }
        if (baseline instanceof String) {
            return setterParam == String.class;
        }
        if (baseline.getClass().isEnum()) {
            return setterParam.isEnum();
        }
        return registry != null;
    }

    private static List<Object> collectAxisCandidates(VariantAxis axis, EntityType<?> type, ClientLevel world) {
        Object baseline = axis.baselineValue();
        List<Object> out = new ArrayList<>();
        if (baseline instanceof Boolean) {
            out.add(Boolean.TRUE);
            out.add(Boolean.FALSE);
            return out;
        }
        if (baseline instanceof Number number) {
            for (int i = 0; i <= MAX_NUMERIC_VARIANT_VALUE; i++) {
                out.add(i);
            }
            if (number.intValue() < 0) {
                out.add(number.intValue());
            }
            return out;
        }
        if (baseline.getClass().isEnum()) {
            Object[] constants = baseline.getClass().getEnumConstants();
            if (constants != null) {
                Collections.addAll(out, constants);
            }
            return out;
        }
        if (axis.registry() != null) {
            List<ResourceLocation> keys = new ArrayList<>(axis.registry().keySet());
            keys.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
            int count = 0;
            for (ResourceLocation key : keys) {
                if (count >= MAX_DYNAMIC_REGISTRY_VALUES) {
                    break;
                }
                Object value = getRegistryValue(axis.registry(), key);
                if (value != null) {
                    out.add(value);
                    count++;
                }
            }
            return out;
        }
        if (baseline instanceof String) {
            return sampleGetterValues(axis.getter(), type, world);
        }
        return out;
    }

    private static List<Object> sampleGetterValues(Method getter, EntityType<?> type, ClientLevel world) {
        Set<String> seen = new LinkedHashSet<>();
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_SAMPLE_ENTITIES; i++) {
            Entity sample = createEntity(type, world);
            if (sample == null) {
                continue;
            }
            Object value = unwrapHolderValue(invokeNoArg(sample, getter));
            if (value == null) {
                continue;
            }
            if (seen.add(value.toString())) {
                out.add(value);
            }
        }
        return out;
    }

    private static boolean applyAxisCandidate(VariantAxis axis, Entity entity, Object candidate) {
        if (axis == null || entity == null) return false;

        Object converted = convertCandidateForSetter(axis, candidate);
        if (converted == null) return false;

        Method setter = axis.setter();

        try {
            if (!setter.canAccess(entity)) setter.setAccessible(true);
            setter.invoke(entity, converted);
            return true;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();

            System.out.println("Identity2 axis apply failed");
            System.out.println("Entity class: " + entity.getClass().getName());
            System.out.println("Setter: " + setter.getName());
            System.out.println("Param type: " + setter.getParameterTypes()[0].getName());
            System.out.println("Candidate class: " + (candidate == null ? "null" : candidate.getClass().getName()));
            System.out.println("Converted class: " + (converted == null ? "null" : converted.getClass().getName()));
            System.out.println("Converted value: " + converted);
            if (cause != null) {
                System.out.println("Cause: " + cause.getClass().getName() + " " + cause.getMessage());
                cause.printStackTrace();
            } else {
                e.printStackTrace();
            }
            return false;
        } catch (Throwable t) {
            System.out.println("Identity2 axis apply failed with " + t.getClass().getName() + " " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    private static Object convertCandidateForSetter(VariantAxis axis, Object candidate) {
        if (axis == null || candidate == null) {
            return null;
        }

        Class<?> param = axis.setter().getParameterTypes()[0];

        // Some Mojang setters are typed as Object but internally cast to Holder.
        // So if we have a registry, prefer passing a Holder first.
        if (axis.registry() != null && !(candidate instanceof net.minecraft.core.Holder<?>)) {
            Object wrapped = wrapAsHolder(axis.registry(), candidate, Object.class);
            if (wrapped != null) {
                return wrapped;
            }
        }

        if (isAssignable(param, candidate.getClass())) {
            return candidate;
        }

        if (candidate instanceof Number number) {
            if (param == int.class || param == Integer.class) return number.intValue();
            if (param == long.class || param == Long.class) return number.longValue();
            if (param == short.class || param == Short.class) return number.shortValue();
            if (param == byte.class || param == Byte.class) return number.byteValue();
            if (param == float.class || param == Float.class) return number.floatValue();
            if (param == double.class || param == Double.class) return number.doubleValue();
        }

        if (candidate instanceof Boolean bool) {
            if (param == boolean.class || param == Boolean.class) return bool;
        }

        if (param.isEnum()) {
            if (candidate instanceof Enum<?> e) return parseEnumValue(param, e.name());
            if (candidate instanceof String text) return parseEnumValue(param, text);
        }

        if (axis.registry() != null) {
            Object wrapped = wrapAsHolder(axis.registry(), candidate, param);
            return wrapped;
        }

        return null;
    }

    private static Object parseEnumValue(Class<?> enumClass, String text) {
        if (enumClass == null || !enumClass.isEnum() || text == null || text.isBlank()) {
            return null;
        }
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        return null;
    }

    private static Object wrapAsHolder(Registry<?> registry, Object value, Class<?> paramType) {
        if (registry == null || value == null || paramType == null) {
            return null;
        }
        for (Method method : registry.getClass().getMethods()) {
            if (!method.getName().equals("wrapAsHolder") || method.getParameterCount() != 1) {
                continue;
            }
            if (!isAssignable(method.getParameterTypes()[0], value.getClass())) {
                continue;
            }
            try {
                Object wrapped = method.invoke(registry, value);
                if (wrapped != null && isAssignable(paramType, wrapped.getClass())) {
                    return wrapped;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String buildAxisLabel(ResourceLocation typeId, VariantAxis axis, Object candidate) {
        String entityName = capitalize(typeId.getPath().replace('_', ' '));
        String axisName = capitalize(splitCamel(axis.suffix()));
        String valueName = formatCandidateName(candidate, axis.registry());
        return entityName + " " + axisName + " " + valueName;
    }

    private static String formatCandidateName(Object candidate, Registry<?> registry) {
        if (candidate == null) {
            return "Unknown";
        }
        if (candidate instanceof Enum<?> enumValue) {
            return capitalize(enumValue.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        if (candidate instanceof Number number) {
            return String.valueOf(number.intValue());
        }
        if (candidate instanceof String text) {
            return capitalize(text.replace('_', ' '));
        }
        ResourceLocation id = registry == null ? null : getRegistryKey(registry, candidate);
        if (id != null) {
            return capitalize(id.getPath().replace('_', ' '));
        }
        return capitalize(candidate.toString().replace('_', ' '));
    }

    private static List<IdentityVariant> discoverSampledVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        Entity baselineEntity = createEntity(type, world);
        if (baselineEntity == null) {
            return List.of();
        }
        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baselineEntity);
        if (baselineData == null || baselineData.isEmpty()) {
            return List.of();
        }

        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        int index = 1;
        for (int i = 0; i < MAX_DYNAMIC_SAMPLE_ENTITIES; i++) {
            Entity sample = createEntity(type, world);
            if (sample == null) {
                continue;
            }
            CompoundTag data = IdentityVariantNbtHelper.writeEntityData(sample);
            if (data == null || data.isEmpty()) {
                continue;
            }
            CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, data);
            if (diff.isEmpty()) {
                continue;
            }
            if (!isLikelyVariantDiff(diff)) {
                continue;
            }

            IdentityVariant variant = new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Variant " + index, diff);
            String token = IdentityProgression.toVariantUnlockToken(variant.variantNbt());
            if (out.putIfAbsent(token, variant) == null) {
                index++;
            }
        }
        return new ArrayList<>(out.values());
    }

    private static List<IdentityVariant> discoverGenericVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        Entity baseEntity = createEntity(type, world);
        if (baseEntity == null) {
            return List.of();
        }

        CompoundTag baseline = IdentityVariantNbtHelper.writeEntityData(baseEntity);
        if (baseline == null || baseline.isEmpty()) {
            return List.of();
        }

        List<IdentityVariant> variants = new ArrayList<>();
        for (String key : CANDIDATE_KEYS) {
            if (!baseline.contains(key)) {
                continue;
            }
            NumericKind numericKind = detectNumericKind(baseline, key);
            if (numericKind == null) {
                continue;
            }

            for (int value = 0; value <= MAX_NUMERIC_VARIANT_VALUE; value++) {
                CompoundTag probe = baseline.copy();
                putNumeric(probe, key, numericKind, value);

                Entity candidate = createEntity(type, world);
                if (!IdentityVariantNbtHelper.loadEntityData(candidate, probe)) {
                    continue;
                }

                CompoundTag roundTrip = IdentityVariantNbtHelper.writeEntityData(candidate);
                if (roundTrip == null || !matchesNumeric(roundTrip, key, numericKind, value)) {
                    continue;
                }

                CompoundTag variant = new CompoundTag();
                putNumeric(variant, key, numericKind, value);
                variants.add(new IdentityVariant(typeId, buildGenericDisplayName(typeId, key, value), variant));
            }
        }

        return variants;
    }


    private static List<IdentityVariant> discoverBabyVariantsFromExisting(
            EntityType<?> type,
            ResourceLocation typeId,
            ClientLevel world,
            List<IdentityVariant> existing
    ) {
        if (IdentityApi.isBabyVariantBlocked(type)) {
            return List.of();
        }
        if (type == null || typeId == null || world == null || existing == null || existing.isEmpty()) {
            return List.of();
        }

        Entity probe = createEntity(type, world);
        if (probe == null) {
            Identity2.LOGGER.info("[VariantDebug] baby discovery probe creation failed for {}", typeId);
            return List.of();
        }

        logObjectStructure("[VariantDebug] baby discovery probe", probe);

        if (!(probe instanceof AgeableMob babyProbe)) {
            Identity2.LOGGER.info(
                    "[VariantDebug] baby discovery probe is not ageable for {} class={}",
                    typeId,
                    probe.getClass().getName()
            );
            return List.of();
        }

        List<IdentityVariant> out = new ArrayList<>();

        for (IdentityVariant baseVariant : existing) {
            if (baseVariant == null) continue;

            CompoundTag baseNbt = baseVariant.variantNbt();
            if (baseNbt == null) continue;

            boolean alreadyBabyByFlag = NbtCompat.getBooleanOr(baseNbt, "IsBaby", false);
            boolean alreadyBabyByAlias = NbtCompat.getBooleanOr(baseNbt, "Baby", false);
            boolean alreadyBabyByAge = NbtCompat.getIntOr(baseNbt, "Age", 0) < 0;
            if (alreadyBabyByFlag || alreadyBabyByAlias || alreadyBabyByAge) {
                continue;
            }

            CompoundTag combined = baseNbt.copy();

            if (!combined.contains("IsBaby") && !combined.contains("Baby")) {
                combined.putBoolean("IsBaby", true);
            }
            if (!combined.contains("Age")) {
                combined.putInt("Age", -24000);
            }

            Entity babyEntity = createEntity(type, world);
            if (!IdentityVariantNbtHelper.loadEntityData(babyEntity, combined)) {
                continue;
            }
            if (!(babyEntity instanceof AgeableMob babyAgeable)) {
                continue;
            }
            babyAgeable.setBaby(true);
            babyAgeable.setAge(-24000);
            if (!babyAgeable.isBaby()) {
                continue;
            }

            String name = baseVariant.displayName() + " Baby";
            out.add(new IdentityVariant(typeId, name, combined));
        }

        return out;
    }

    private static List<IdentityVariant> discoverBabyVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        if (IdentityApi.isBabyVariantBlocked(type)) {
            return List.of();
        }
        Entity baselineEntity = createEntity(type, world);
        if (baselineEntity == null) {
            Identity2.LOGGER.info("[VariantDebug] baby baseline creation failed for {}", typeId);
            return List.of();
        }
        logObjectStructure("[VariantDebug] baby baseline entity", baselineEntity);
        if (!(baselineEntity instanceof AgeableMob)) {
            Identity2.LOGGER.info(
                    "[VariantDebug] baby discovery baseline entity is not ageable for {} class={}",
                    typeId,
                    baselineEntity.getClass().getName()
            );
            return List.of();
        }

        Entity babyProbe = createEntity(type, world);
        if (babyProbe == null) {
            return List.of();
        }
        if (!(babyProbe instanceof AgeableMob babyAgeable)) {
            return List.of();
        }
        babyAgeable.setBaby(true);
        babyAgeable.setAge(-24000);
        if (!babyAgeable.isBaby()) {
            return List.of();
        }

        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baselineEntity);
        CompoundTag babyData = IdentityVariantNbtHelper.writeEntityData(babyProbe);
        if (baselineData == null || babyData == null) {
            return List.of();
        }

        CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, babyData);
        CompoundTag variant = diff.copy();
        if (!variant.contains("IsBaby", Tag.TAG_BYTE) && !variant.contains("Baby", Tag.TAG_BYTE)) {
            variant.putBoolean("IsBaby", true);
        }
        if (variant.isEmpty()) {
            return List.of();
        }

        return List.of(new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", variant));
    }

    private static String buildGenericDisplayName(ResourceLocation typeId, String key, int value) {
        String entityName = capitalize(typeId.getPath().replace('_', ' '));
        if ("Color".equals(key) && typeId.equals(EntityType.getKey(EntityType.SHEEP))) {
            DyeColor color = DyeColor.byId(value % 16);
            return "Sheep " + capitalize(color.getName());
        }
        return entityName + " " + key + " " + value;
    }

    private static String splitCamel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(value.charAt(i - 1))) {
                out.append(' ');
            }
            out.append(c);
        }
        return out.toString().replace('_', ' ');
    }

    private static boolean isLikelyVariantName(String loweredSuffix) {
        if (loweredSuffix == null || loweredSuffix.isBlank()) {
            return false;
        }
        for (String hint : VARIANT_NAME_HINTS) {
            if (loweredSuffix.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static String extractGetterSuffix(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodName.substring(3);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodName.substring(2);
        }
        return null;
    }

    private static Method findSetter(List<Method> methods, String suffix) {
        if (methods == null || suffix == null || suffix.isBlank()) {
            return null;
        }
        String setterName = "set" + suffix;
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private static Method findZeroArgBooleanMethod(List<Method> methods, String methodName) {
        if (methods == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findBooleanSetter(List<Method> methods, String methodName) {
        if (methods == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param == boolean.class || param == Boolean.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findIntSetter(List<Method> methods, String methodName) {
        if (methods == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param == int.class || param == Integer.class) {
                return method;
            }
        }
        return null;
    }

    private static boolean invokeBooleanSetter(Object target, Method setter, boolean value) {
        if (target == null || setter == null) {
            return false;
        }
        try {
            if (!setter.canAccess(target)) {
                setter.setAccessible(true);
            }
            setter.invoke(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeIntSetter(Object target, Method setter, int value) {
        if (target == null || setter == null) {
            return false;
        }
        try {
            if (!setter.canAccess(target)) {
                setter.setAccessible(true);
            }
            setter.invoke(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeNoArg(Object target, Method method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : getAllMethods(target.getClass())) {
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
        return null;
    }

    private static List<Method> getAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                for (Class<?> param : method.getParameterTypes()) {
                    signature += ":" + param.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static boolean areEquivalent(Object a, Object b, Registry<?> registry) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        ResourceLocation aId = registry == null ? null : getRegistryKey(registry, a);
        ResourceLocation bId = registry == null ? null : getRegistryKey(registry, b);
        if (aId != null || bId != null) {
            return aId != null && aId.equals(bId);
        }
        return a.equals(b);
    }

    private static Object unwrapHolderValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("value");
            if (method.getParameterCount() == 0) {
                return method.invoke(value);
            }
        } catch (Throwable ignored) {
        }
        return value;
    }

    private static Registry<?> resolveRegistryFromHolderValue(Object value) {
        if (value == null) {
            return null;
        }
        Object optionalKey = invokeNoArg(value, "unwrapKey");
        if (!(optionalKey instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
            return null;
        }
        Object resourceKey = optional.get();
        ResourceLocation registryLocation = null;
        Object registryLocationObject = invokeNoArg(resourceKey, "registry");
        if (registryLocationObject instanceof ResourceLocation id) {
            registryLocation = id;
        } else {
            Object registryKey = invokeNoArg(resourceKey, "registryKey");
            registryLocation = resolveResourceKeyLocation(registryKey);
        }
        if (registryLocation == null) {
            return null;
        }
        Object registryValue = BuiltInRegistries.REGISTRY.get(registryLocation);
        if (registryValue instanceof Registry<?> registry) {
            return registry;
        }
        return null;
    }

    private static Registry<?> resolveRegistryForAxis(Object rawBaseline, Object baselineValue, Method getter, Method setter) {
        Registry<?> registry = resolveRegistryFromHolderValue(rawBaseline);
        if (registry != null) {
            return registry;
        }
        registry = findRegistryForValue(baselineValue);
        if (registry != null) {
            return registry;
        }
        return resolveRegistryFromMethodSignature(getter, setter);
    }

    private static Registry<?> resolveRegistryFromMethodSignature(Method getter, Method setter) {
        Class<?> valueType = resolveValueTypeFromMethodSignatures(getter, setter);
        if (valueType == null) {
            return null;
        }
        Registry<?> byField = resolveRegistry(toUpperSnake(valueType.getSimpleName()));
        if (byField != null) {
            return byField;
        }
        return findRegistryByValueType(valueType);
    }

    private static Class<?> resolveValueTypeFromMethodSignatures(Method getter, Method setter) {
        if (getter != null) {
            Class<?> fromGetter = extractValueType(getter.getGenericReturnType());
            if (fromGetter != null) {
                return fromGetter;
            }
            Class<?> getterType = getter.getReturnType();
            if (getterType != null && getterType != Object.class && !isHolderLikeClass(getterType)) {
                return getterType;
            }
        }
        if (setter != null && setter.getParameterCount() == 1) {
            Type[] genericTypes = setter.getGenericParameterTypes();
            if (genericTypes.length == 1) {
                Class<?> fromSetter = extractValueType(genericTypes[0]);
                if (fromSetter != null) {
                    return fromSetter;
                }
            }
            Class<?> setterType = setter.getParameterTypes()[0];
            if (setterType != null && setterType != Object.class && !isHolderLikeClass(setterType)) {
                return setterType;
            }
        }
        return null;
    }

    private static Class<?> extractValueType(Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class<?> clazz) {
            if (clazz == Object.class || isHolderLikeClass(clazz)) {
                return null;
            }
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass) {
                String rawName = rawClass.getName();
                if (rawName.equals("net.minecraft.core.Holder")
                        || rawName.equals("net.minecraft.core.Holder$Reference")
                        || rawName.equals("net.minecraft.core.Holder$Direct")) {
                    Type[] args = parameterizedType.getActualTypeArguments();
                    if (args.length == 1) {
                        return extractValueType(args[0]);
                    }
                }
            }
            for (Type arg : parameterizedType.getActualTypeArguments()) {
                Class<?> extracted = extractValueType(arg);
                if (extracted != null) {
                    return extracted;
                }
            }
            return extractValueType(rawType);
        }
        if (type instanceof WildcardType wildcardType) {
            for (Type upperBound : wildcardType.getUpperBounds()) {
                Class<?> extracted = extractValueType(upperBound);
                if (extracted != null) {
                    return extracted;
                }
            }
        }
        return null;
    }

    private static boolean isHolderLikeClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        String name = clazz.getName();
        return name.equals("net.minecraft.core.Holder")
                || name.equals("net.minecraft.core.Holder$Reference")
                || name.equals("net.minecraft.core.Holder$Direct");
    }

    private static Registry<?> findRegistryByValueType(Class<?> valueType) {
        if (valueType == null || valueType == Object.class) {
            return null;
        }
        try {
            for (ResourceLocation registryId : BuiltInRegistries.REGISTRY.keySet()) {
                Object value = BuiltInRegistries.REGISTRY.get(registryId);
                if (!(value instanceof Registry<?> registry) || registry.keySet().isEmpty()) {
                    continue;
                }
                if (registryContainsValueType(registry, valueType)) {
                    return registry;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean registryContainsValueType(Registry<?> registry, Class<?> valueType) {
        if (registry == null || valueType == null) {
            return false;
        }
        int inspected = 0;
        for (ResourceLocation key : registry.keySet()) {
            if (inspected++ >= 16) {
                break;
            }
            Object value = getRegistryValue(registry, key);
            Object unwrapped = unwrapHolderValue(value);
            if (unwrapped == null) {
                continue;
            }
            Class<?> actual = unwrapped.getClass();
            if (valueType.isAssignableFrom(actual) || actual.isAssignableFrom(valueType)) {
                return true;
            }
        }
        return false;
    }

    private static String toUpperSnake(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(value.charAt(i - 1))) {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static Registry<?> findRegistryForValue(Object value) {
        if (value == null) {
            return null;
        }
        Object unwrapped = unwrapHolderValue(value);
        for (Field field : BuiltInRegistries.class.getFields()) {
            try {
                Object candidate = field.get(null);
                if (!(candidate instanceof Registry<?> registry)) {
                    continue;
                }
                if (getRegistryKey(registry, unwrapped) != null) {
                    return registry;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation getRegistryKey(Registry<?> registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        try {
            return ((Registry<Object>) registry).getKey(value);
        } catch (Throwable ignored) {
            Object unwrapped = unwrapHolderValue(value);
            if (unwrapped != value) {
                try {
                    return ((Registry<Object>) registry).getKey(unwrapped);
                } catch (Throwable ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getRegistryValue(Registry<?> registry, ResourceLocation id) {
        if (registry == null || id == null) {
            return null;
        }
        try {
            return ((Registry<Object>) registry).get(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType == int.class && argType == Integer.class) {
            return true;
        }
        if (paramType == boolean.class && argType == Boolean.class) {
            return true;
        }
        if (paramType == byte.class && argType == Byte.class) {
            return true;
        }
        if (paramType == short.class && argType == Short.class) {
            return true;
        }
        if (paramType == long.class && argType == Long.class) {
            return true;
        }
        if (paramType == float.class && argType == Float.class) {
            return true;
        }
        return paramType == double.class && argType == Double.class;
    }

    private static boolean isLikelyVariantDiff(CompoundTag diff) {
        if (diff == null || diff.isEmpty()) {
            return false;
        }
        for (String key : diff.getAllKeys()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if ("IsBaby".equals(key) || "Baby".equals(key)) {
                return true;
            }
            String lowered = key.toLowerCase(Locale.ROOT);
            for (String hint : VARIANT_NAME_HINTS) {
                if (lowered.contains(hint)) {
                    return true;
                }
            }
            if ("color".equals(lowered) || "villagerdata".equals(lowered) || "villagerlevel".equals(lowered)) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag sanitizeVariantNbt(CompoundTag source, boolean root) {
        if (source == null || source.isEmpty()) {
            return new CompoundTag();
        }
        CompoundTag out = new CompoundTag();
        for (String key : source.getAllKeys()) {
            if (root && NON_VARIANT_ROOT_KEYS.contains(key)) {
                continue;
            }
            Tag tag = source.get(key);
            if (tag == null) {
                continue;
            }
            if (tag instanceof CompoundTag compoundTag) {
                CompoundTag nested = sanitizeVariantNbt(compoundTag, false);
                if (!nested.isEmpty()) {
                    out.put(key, nested);
                }
                continue;
            }
            if (isBlacklistedStringTag(source, key)) {
                return new CompoundTag();
            }
            out.put(key, tag.copy());
        }
        return out;
    }

    private static boolean isBlacklistedStringTag(CompoundTag source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return false;
        }
        if (!source.contains(key, Tag.TAG_STRING)) {
            return false;
        }
        String raw = NbtCompat.getStringOr(source, key, "").trim();
        if (raw.isBlank()) {
            return false;
        }
        String normalized = normalizeVariantValue(raw);
        if (BLACKLISTED_VARIANT_EXACT.contains(normalized)) {
            return true;
        }
        for (String prefix : BLACKLISTED_VARIANT_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeVariantValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        int separator = normalized.indexOf(':');
        if (separator >= 0 && separator < normalized.length() - 1) {
            return normalized.substring(separator + 1);
        }
        return normalized;
    }

    private static NumericKind detectNumericKind(CompoundTag nbt, String key) {
        if (nbt.contains(key, Tag.TAG_BYTE)) {
            return NumericKind.BYTE;
        }
        if (nbt.contains(key, Tag.TAG_SHORT)) {
            return NumericKind.SHORT;
        }
        if (nbt.contains(key, Tag.TAG_INT)) {
            return NumericKind.INT;
        }
        if (nbt.contains(key, Tag.TAG_LONG)) {
            return NumericKind.LONG;
        }
        return null;
    }

    private static void putNumeric(CompoundTag nbt, String key, NumericKind kind, int value) {
        switch (kind) {
            case BYTE -> nbt.putByte(key, (byte) value);
            case SHORT -> nbt.putShort(key, (short) value);
            case INT -> nbt.putInt(key, value);
            case LONG -> nbt.putLong(key, value);
        }
    }

    private static boolean matchesNumeric(CompoundTag nbt, String key, NumericKind kind, int expected) {
        return switch (kind) {
            case BYTE -> nbt.contains(key, Tag.TAG_BYTE) && nbt.getByte(key) == (byte) expected;
            case SHORT -> nbt.contains(key, Tag.TAG_SHORT) && nbt.getShort(key) == (short) expected;
            case INT -> nbt.contains(key, Tag.TAG_INT) && nbt.getInt(key) == expected;
            case LONG -> nbt.contains(key, Tag.TAG_LONG) && nbt.getLong(key) == (long) expected;
        };
    }

    private record VariantAxis(String suffix, Method getter, Method setter, Object baselineValue, Registry<?> registry) {
    }

    private enum NumericKind {
        BYTE,
        SHORT,
        INT,
        LONG
    }

    private static Entity createEntity(EntityType<?> type, ClientLevel world) {
        try {
            return type.create(world);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IdentityVariant defaultVariant(EntityType<?> type) {
        ResourceLocation typeId = type == null ? ResourceLocation.parse("minecraft:pig") : EntityType.getKey(type);
        return defaultVariant(typeId);
    }

    private static IdentityVariant defaultVariant(ResourceLocation typeId) {
        return new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')), new CompoundTag());
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        String[] parts = normalized.split(" ");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                out.append(p.substring(1));
            }
        }
        return out.toString();
    }

    private static void logObjectStructure(String label, Object obj) {
        if (obj == null) {
            Identity2.LOGGER.info("{}: <null>", label);
            return;
        }

        Class<?> clazz = obj.getClass();
        String methods = Arrays.stream(clazz.getDeclaredMethods())
                .map(IdentityVariantDiscovery::formatMethodSignature)
                .sorted()
                .limit(80)
                .collect(java.util.stream.Collectors.joining(", "));
        String fields = Arrays.stream(clazz.getDeclaredFields())
                .map(field -> field.getName() + ":" + field.getType().getSimpleName())
                .sorted()
                .limit(80)
                .collect(java.util.stream.Collectors.joining(", "));

        Identity2.LOGGER.info(
                "{} class={} super={} methods=[{}] fields=[{}]",
                label,
                clazz.getName(),
                clazz.getSuperclass() == null ? "<none>" : clazz.getSuperclass().getName(),
                methods,
                fields
        );
    }

    private static String formatMethodSignature(Method method) {
        if (method == null) {
            return "<null>";
        }
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(java.util.stream.Collectors.joining(","));
        return method.getName() + "(" + params + "):" + method.getReturnType().getSimpleName();
    }
}
