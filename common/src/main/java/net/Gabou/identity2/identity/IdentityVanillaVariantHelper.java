package net.Gabou.identity2.identity;

import net.Gabou.identity2.Identity2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class IdentityVanillaVariantHelper {
    private IdentityVanillaVariantHelper() {
    }

    public static List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        if (type == null) {
            return List.of();
        }
        ResourceLocation typeId = EntityType.getKey(type);
        if (typeId == null) {
            return List.of();
        }

        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        if (type == EntityType.SHEEP) {
            addVariants(out, discoverSheepVariants(typeId));
        } else if (type == EntityType.AXOLOTL) {
            addVariants(out, discoverAxolotlVariants(typeId, level));
        } else if (type == EntityType.CAT) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "CAT_VARIANT", "CatVariant", "Cat"));
        } else if (type == EntityType.WOLF) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "WOLF_VARIANT", "WolfVariant", "Wolf"));
        } else if (type == EntityType.FROG) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "FROG_VARIANT", "FrogVariant", "Frog"));
        }

        IdentityVariant babyVariant = discoverBabyVariant(type, typeId, level);
        if (babyVariant != null) {
            addVariant(out, babyVariant);
        }

        if (out.isEmpty()) {
            Identity2.LOGGER.debug("No direct vanilla variants discovered for {}", typeId);
        }
        return new ArrayList<>(out.values());
    }

    public static CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag variant = new CompoundTag();
        if (entity == null) {
            return variant;
        }

        if (entity instanceof AgeableMob ageable) {
            variant.putBoolean("IsBaby", ageable.isBaby());
            int age = ageable.getAge();
            if (age != 0) {
                variant.putInt("Age", age);
            }
            Object ageLocked = readOptionalValue(ageable, "isAgeLocked");
            if (ageLocked == null) {
                ageLocked = readOptionalValue(ageable, "getAgeLocked");
            }
            if (ageLocked instanceof Boolean bool) {
                variant.putBoolean("AgeLocked", bool);
            }
        }

        if (entity.getType() == EntityType.SHEEP) {
            Object color = invokeNoArg(entity, "getColor");
            Integer colorId = resolveDyeColorId(color);
            if (colorId != null) {
                variant.putByte("Color", (byte) Math.max(0, Math.min(15, colorId)));
            }
        }

        if (entity.getType() == EntityType.CAT) {
            Object catVariant = invokeNoArg(entity, "getVariant");
            putRegistryKey(variant, "CatVariant", resolveRegistryKey("CAT_VARIANT", catVariant));
        }

        if (entity.getType() == EntityType.WOLF) {
            Object wolfVariant = invokeNoArg(entity, "getVariant");
            putRegistryKey(variant, "WolfVariant", resolveRegistryKey("WOLF_VARIANT", wolfVariant));
            Object collarColor = invokeNoArg(entity, "getCollarColor");
            Integer colorId = resolveDyeColorId(collarColor);
            if (colorId != null) {
                variant.putInt("CollarColor", Math.max(0, colorId));
            }
        }

        if (entity.getType() == EntityType.FROG) {
            Object frogVariant = invokeNoArg(entity, "getVariant");
            putRegistryKey(variant, "FrogVariant", resolveRegistryKey("FROG_VARIANT", frogVariant));
        }

        if (entity.getType() == EntityType.AXOLOTL) {
            Object axolotlVariant = invokeNoArg(entity, "getVariant");
            Integer variantId = resolveNumericVariantValue(axolotlVariant);
            if (variantId != null) {
                variant.putInt("Variant", variantId);
            }
        }

        return variant;
    }

    public static void applyVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        if (entity instanceof AgeableMob ageable) {
            applyAgeableState(ageable, variantNbt);
        }

        if (entity.getType() == EntityType.SHEEP) {
            applySheepState(entity, variantNbt);
        }

        if (entity.getType() == EntityType.CAT) {
            applyRegistryBackedVariant(entity, variantNbt, "CatVariant", "CAT_VARIANT");
        } else if (entity.getType() == EntityType.WOLF) {
            applyRegistryBackedVariant(entity, variantNbt, "WolfVariant", "WOLF_VARIANT");
            Object color = readOptionalValue(variantNbt, "CollarColor");
            Integer colorId = resolveNumericVariantValue(color);
            if (colorId != null) {
                Object dyeColor = invokeStaticDyeColorById(colorId);
                if (dyeColor != null) {
                    invokeOneArg(entity, "setCollarColor", dyeColor);
                }
            }
        } else if (entity.getType() == EntityType.FROG) {
            applyRegistryBackedVariant(entity, variantNbt, "FrogVariant", "FROG_VARIANT");
        }

        if (entity.getType() == EntityType.AXOLOTL) {
            applyAxolotlState(entity, variantNbt);
        }

        applyVillagerVariantData(entity, variantNbt);
    }

    private static void applyAgeableState(AgeableMob ageable, CompoundTag variantNbt) {
        if (ageable == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        Boolean baby = readBoolean(variantNbt, "IsBaby");
        if (baby == null) {
            baby = readBoolean(variantNbt, "Baby");
        }
        if (baby != null) {
            invokeOneArg(ageable, "setBaby", baby);
        }

        Integer age = readInt(variantNbt, "Age");
        if (age != null) {
            invokeIntArg(ageable, "setAge", age);
            if (age < 0) {
                invokeOneArg(ageable, "setBaby", true);
            }
        }

        Boolean ageLocked = readBoolean(variantNbt, "AgeLocked");
        if (ageLocked != null) {
            invokeOneArg(ageable, "setAgeLocked", ageLocked);
        }
    }

    private static void applySheepState(Entity entity, CompoundTag variantNbt) {
        Integer colorId = readInt(variantNbt, "Color");
        if (colorId == null) {
            return;
        }
        Object dyeColor = invokeStaticDyeColorById(Math.max(0, Math.min(15, colorId)));
        if (dyeColor != null) {
            invokeOneArg(entity, "setColor", dyeColor);
        }
    }

    private static void applyAxolotlState(Entity entity, CompoundTag variantNbt) {
        Integer variantId = readInt(variantNbt, "Variant");
        if (variantId == null) {
            return;
        }
        Object currentVariant = invokeNoArg(entity, "getVariant");
        Object[] values = currentVariant != null && currentVariant.getClass().isEnum()
                ? currentVariant.getClass().getEnumConstants()
                : null;
        if (values == null || values.length == 0) {
            return;
        }
        int index = Math.max(0, Math.min(values.length - 1, variantId));
        invokeOneArg(entity, "setVariant", values[index]);
    }

    private static void applyVillagerVariantData(Entity entity, CompoundTag variantNbt) {
        Object villagerData = invokeNoArg(entity, "getVillagerData");
        if (villagerData == null) {
            return;
        }

        CompoundTag villagerDataTag = readCompound(variantNbt, "VillagerData");
        String professionRaw = readString(variantNbt, "VillagerProfession", "Profession", "profession");
        if ((professionRaw == null || professionRaw.isBlank()) && villagerDataTag != null) {
            professionRaw = readString(villagerDataTag, "profession");
        }
        String typeRaw = readString(variantNbt, "VillagerType", "Type", "type");
        if ((typeRaw == null || typeRaw.isBlank()) && villagerDataTag != null) {
            typeRaw = readString(villagerDataTag, "type");
        }

        ResourceLocation professionId = parseIdentifier(professionRaw);
        ResourceLocation typeId = parseIdentifier(typeRaw);

        if (professionId != null) {
            Object profession = resolveRegistryValue("VILLAGER_PROFESSION", professionId);
            if (profession != null) {
                Object wrapped = wrapAsHolder(getBuiltInRegistryObject("VILLAGER_PROFESSION"), profession);
                Object updated = invokeOneArg(villagerData, "setProfession", wrapped != null ? wrapped : profession);
                if (updated != null) {
                    villagerData = updated;
                }
            }
        }

        if (typeId != null) {
            Object villagerType = resolveRegistryValue("VILLAGER_TYPE", typeId);
            if (villagerType != null) {
                Object wrapped = wrapAsHolder(getBuiltInRegistryObject("VILLAGER_TYPE"), villagerType);
                Object updated = invokeOneArg(villagerData, "setType", wrapped != null ? wrapped : villagerType);
                if (updated != null) {
                    villagerData = updated;
                }
            }
        }

        Integer level = readInt(variantNbt, "VillagerLevel");
        if (level == null && villagerDataTag != null) {
            level = readInt(villagerDataTag, "level");
        }
        if (level != null && level > 0) {
            Object updated = invokeIntArg(villagerData, "setLevel", Math.max(1, level));
            if (updated != null) {
                villagerData = updated;
            }
        }

        if (invokeOneArg(entity, "setVillagerData", villagerData) != null) {
            clearVillagerOffers(entity);
        }
    }

    private static void clearVillagerOffers(Object villager) {
        if (villager == null) {
            return;
        }
        try {
            Class<?> offersClass = Class.forName("net.minecraft.world.item.trading.MerchantOffers");
            Object offers = offersClass.getConstructor().newInstance();
            if (invokeOneArg(villager, "setOffers", offers) != null) {
                return;
            }
        } catch (Throwable ignored) {
        }
        invokeNoArg(villager, "resetOffers");
    }

    private static void applyRegistryBackedVariant(Entity entity, CompoundTag variantNbt, String nbtKey, String registryField) {
        String raw = readString(variantNbt, nbtKey, "variant", "Variant");
        ResourceLocation variantId = parseIdentifier(raw);
        if (variantId == null) {
            return;
        }
        Object variant = resolveRegistryValue(registryField, variantId);
        if (variant == null) {
            return;
        }
        Object registry = getBuiltInRegistryObject(registryField);
        Object wrapped = wrapAsHolder(registry, variant);
        if (invokeOneArg(entity, "setVariant", variant) != null) {
            return;
        }
        if (wrapped != null && invokeOneArg(entity, "setVariant", wrapped) != null) {
            return;
        }
        if (invokeOneArg(entity, "setType", variant) == null && wrapped != null) {
            invokeOneArg(entity, "setType", wrapped);
        }
    }

    private static List<IdentityVariant> discoverSheepVariants(ResourceLocation typeId) {
        List<IdentityVariant> variants = new ArrayList<>(16);
        for (int i = 0; i < 16; i++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Color", i);
            DyeColor color = DyeColor.byId(i);
            variants.add(new IdentityVariant(typeId, "Sheep " + capitalize(color.getName()), nbt));
        }
        return variants;
    }

    private static List<IdentityVariant> discoverAxolotlVariants(ResourceLocation typeId, Level level) {
        List<IdentityVariant> variants = new ArrayList<>();
        Object sample = null;
        try {
            sample = EntityType.AXOLOTL.create(level);
        } catch (Throwable ignored) {
        }
        Object[] values = sample == null ? null : invokeNoArg(sample, "getVariant") instanceof Enum<?> e ? e.getClass().getEnumConstants() : null;
        if (values == null || values.length == 0) {
            return variants;
        }
        for (int i = 0; i < values.length; i++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Variant", i);
            variants.add(new IdentityVariant(typeId, "Axolotl " + capitalize(values[i].toString()), nbt));
        }
        return variants;
    }

    private static IdentityVariant discoverBabyVariant(EntityType<?> type, ResourceLocation typeId, Level level) {
        if (type == null || typeId == null || level == null) {
            return null;
        }
        Entity baseline = createEntity(type, level);
        Entity babyProbe = createEntity(type, level);
        if (baseline == null || babyProbe == null || !(babyProbe instanceof AgeableMob)) {
            return null;
        }

        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baseline);
        if (baselineData == null || baselineData.isEmpty()) {
            return null;
        }

        invokeOneArg(babyProbe, "setBaby", true);
        invokeIntArg(babyProbe, "setAge", -24000);

        CompoundTag babyData = IdentityVariantNbtHelper.writeEntityData(babyProbe);
        if (babyData == null || babyData.isEmpty()) {
            return null;
        }

        CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, babyData);
        if (diff.isEmpty()) {
            diff.putBoolean("IsBaby", true);
        }
        return diff.isEmpty() ? null : new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", diff);
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

    private static void addVariants(Map<String, IdentityVariant> out, List<IdentityVariant> variants) {
        if (out == null || variants == null || variants.isEmpty()) {
            return;
        }
        for (IdentityVariant variant : variants) {
            addVariant(out, variant);
        }
    }

    private static void addVariant(Map<String, IdentityVariant> out, IdentityVariant variant) {
        if (out == null || variant == null) {
            return;
        }
        CompoundTag normalized = normalizeVariant(variant.variantNbt());
        if (normalized == null || normalized.isEmpty()) {
            return;
        }
        IdentityVariant normalizedVariant = new IdentityVariant(variant.entityTypeId(), variant.displayName(), normalized);
        String token = IdentityProgression.toVariantUnlockToken(normalizedVariant.variantNbt());
        out.putIfAbsent(token, normalizedVariant);
    }

    private static CompoundTag normalizeVariant(CompoundTag source) {
        return source == null ? new CompoundTag() : source.copy();
    }

    private static void putRegistryKey(CompoundTag variant, String key, @Nullable ResourceLocation id) {
        if (variant != null && key != null && !key.isBlank() && id != null) {
            variant.putString(key, id.toString());
        }
    }

    @Nullable
    private static ResourceLocation parseIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(raw);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
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

    @Nullable
    private static ResourceLocation resolveRegistryKey(String fieldName, @Nullable Object value) {
        Registry<?> registry = resolveRegistry(fieldName);
        if (registry == null || value == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Registry<Object> cast = (Registry<Object>) registry;
        try {
            ResourceLocation id = cast.getKey(value);
            if (id != null) {
                return id;
            }
        } catch (Throwable ignored) {
        }
        Object unwrapped = unwrapHolderValue(value);
        if (unwrapped != value) {
            try {
                return cast.getKey(unwrapped);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Object resolveRegistryValue(String registryField, @Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Object registry = getBuiltInRegistryObject(registryField);
        if (registry instanceof Registry<?> rawRegistry) {
            @SuppressWarnings("unchecked")
            Registry<Object> cast = (Registry<Object>) rawRegistry;
            return cast.get(id);
        }
        return null;
    }

    @Nullable
    private static Object getBuiltInRegistryObject(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            return BuiltInRegistries.class.getField(fieldName).get(null);
        } catch (Throwable ignored) {
        }
        try {
            Object registryKeyObj = Registries.class.getField(fieldName).get(null);
            if (!(registryKeyObj instanceof net.minecraft.resources.ResourceKey<?> registryKey)) {
                return null;
            }
            ResourceLocation location = resolveResourceKeyLocation(registryKey);
            if (location == null || BuiltInRegistries.REGISTRY == null) {
                return null;
            }
            return BuiltInRegistries.REGISTRY.get(location);
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ResourceLocation resolveResourceKeyLocation(Object resourceKey) {
        Object direct = invokeNoArg(resourceKey, "location");
        if (direct instanceof ResourceLocation id) {
            return id;
        }
        Object identifier = invokeNoArg(resourceKey, "identifier");
        if (identifier instanceof ResourceLocation id) {
            return id;
        }
        return null;
    }

    @Nullable
    private static Object wrapAsHolder(@Nullable Object registry, @Nullable Object value) {
        if (registry == null || value == null) {
            return null;
        }
        for (Method method : registry.getClass().getMethods()) {
            if (!method.getName().equals("wrapAsHolder") || method.getParameterCount() != 1) {
                continue;
            }
            try {
                return method.invoke(registry, value);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Entity createEntity(EntityType<?> type, Level level) {
        try {
            return type.create(level);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static String readString(CompoundTag tag, String... keys) {
        if (tag == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Object value = readOptionalValue(tag, key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    @Nullable
    private static Boolean readBoolean(CompoundTag tag, String key) {
        Object value = readOptionalValue(tag, key);
        return value instanceof Boolean bool ? bool : null;
    }

    @Nullable
    private static Integer readInt(CompoundTag tag, String key) {
        Object value = readOptionalValue(tag, key);
        return value instanceof Number number ? number.intValue() : null;
    }

    @Nullable
    private static CompoundTag readCompound(CompoundTag tag, String key) {
        Object value = readOptionalValue(tag, key);
        return value instanceof CompoundTag compound ? compound : null;
    }

    @Nullable
    private static Object readOptionalValue(Object target, String key) {
        if (target == null || key == null || key.isBlank()) {
            return null;
        }
        for (String methodName : List.of("getBoolean", "getInt", "getString", "getCompound")) {
            try {
                Method method = target.getClass().getMethod(methodName, String.class);
                Object result = method.invoke(target, key);
                if (result instanceof java.util.Optional<?> optional) {
                    return optional.orElse(null);
                }
                return result;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
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

    @Nullable
    private static Object invokeOneArg(Object target, String methodName, Object value) {
        if (target == null || methodName == null || methodName.isBlank()) {
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

    @Nullable
    private static Object invokeIntArg(Object target, String methodName, int value) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param != int.class && param != Integer.class) {
                continue;
            }
            try {
                return method.invoke(target, value);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Object unwrapHolderValue(@Nullable Object value) {
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

    @Nullable
    private static Integer resolveDyeColorId(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        Object id = invokeNoArg(value, "getId");
        if (id instanceof Number number) {
            return number.intValue();
        }
        Object ordinal = invokeNoArg(value, "ordinal");
        if (ordinal instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    @Nullable
    private static Integer resolveNumericVariantValue(@Nullable Object value) {
        return resolveDyeColorId(value);
    }

    @Nullable
    private static Integer resolveNumericValue(@Nullable Object value) {
        return resolveDyeColorId(value);
    }

    @Nullable
    private static Object invokeStaticDyeColorById(int id) {
        try {
            return DyeColor.byId(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        String[] parts = normalized.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }
}

