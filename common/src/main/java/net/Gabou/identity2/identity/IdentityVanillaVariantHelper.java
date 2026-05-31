package net.Gabou.identity2.identity;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.axolotl.Axolotl.Variant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.DyeColor;
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

    public static List<IdentityVariant> discoverVariants(EntityType<?> type, ClientLevel level) {
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
            addVariants(out, discoverAxolotlVariants(typeId));
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

        if (out.isEmpty() && isSupportedVanillaVariantType(type)) {
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
            if (ageable.getAge() != 0) {
                variant.putInt("Age", ageable.getAge());
            }
            Object ageLocked = invokeNoArg(ageable, "isAgeLocked");
            if (!(ageLocked instanceof Boolean)) {
                ageLocked = invokeNoArg(ageable, "getAgeLocked");
            }
            if (ageLocked instanceof Boolean bool) {
                variant.putBoolean("AgeLocked", bool);
            }
        }

        if (entity instanceof Sheep sheep) {
            variant.putByte("Color", (byte) clampDyeId(sheep.getColor()));
        }

        if (entity instanceof Cat cat) {
            putRegistryKey(variant, "CatVariant", resolveRegistryKey("CAT_VARIANT", cat.getVariant()));
        }

        if (entity instanceof Wolf wolf) {
            Object wolfVariant = invokeNoArg(wolf, "getVariant");
            putRegistryKey(variant, "WolfVariant", resolveRegistryKey("WOLF_VARIANT", wolfVariant));
            variant.putInt("CollarColor", clampDyeId(wolf.getCollarColor()));
        }

        if (entity instanceof Frog frog) {
            putRegistryKey(variant, "FrogVariant", BuiltInRegistries.FROG_VARIANT.getKey(frog.getVariant()));
        }

        if (entity instanceof Axolotl axolotl) {
            Variant axolotlVariant = axolotl.getVariant();
            if (axolotlVariant != null) {
                variant.putInt("Variant", axolotlVariant.ordinal());
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

        if (entity instanceof Sheep sheep) {
            applySheepState(sheep, variantNbt);
        }

        if (entity instanceof Cat cat) {
            applyRegistryBackedVariant(cat, variantNbt, "CatVariant", "CAT_VARIANT");
        }

        if (entity instanceof Wolf wolf) {
            applyRegistryBackedVariant(wolf, variantNbt, "WolfVariant", "WOLF_VARIANT");
            if (variantNbt.contains("CollarColor", Tag.TAG_ANY_NUMERIC)) {
                DyeColor color = DyeColor.byId(variantNbt.getInt("CollarColor"));
                if (color != null) {
                    wolf.setCollarColor(color);
                }
            }
        }

        if (entity instanceof Frog frog) {
            applyRegistryBackedVariant(frog, variantNbt, "FrogVariant", "FROG_VARIANT");
        }

        if (entity instanceof Axolotl axolotl) {
            applyAxolotlState(axolotl, variantNbt);
        }

        applyVillagerVariantData(entity, variantNbt);
    }

    private static void applyAgeableState(AgeableMob ageable, CompoundTag variantNbt) {
        if (ageable == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        boolean hasBabyFlag = variantNbt.contains("IsBaby", Tag.TAG_BYTE) || variantNbt.contains("Baby", Tag.TAG_BYTE);
        if (hasBabyFlag) {
            boolean baby = NbtCompat.getBooleanOr(variantNbt, "IsBaby", NbtCompat.getBooleanOr(variantNbt, "Baby", false));
            ageable.setBaby(baby);
        }

        if (variantNbt.contains("Age", Tag.TAG_ANY_NUMERIC)) {
            int age = variantNbt.getInt("Age");
            ageable.setAge(age);
            if (age < 0) {
                ageable.setBaby(true);
            }
        }

        if (variantNbt.contains("AgeLocked", Tag.TAG_BYTE)) {
            invokeOneArg(ageable, "setAgeLocked", NbtCompat.getBooleanOr(variantNbt, "AgeLocked", false));
        }
    }

    private static void applySheepState(Sheep sheep, CompoundTag variantNbt) {
        if (sheep == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        if (!variantNbt.contains("Color", Tag.TAG_ANY_NUMERIC)) {
            return;
        }
        int colorId = Math.max(0, Math.min(15, variantNbt.getInt("Color")));
        DyeColor color = DyeColor.byId(colorId);
        if (color != null) {
            sheep.setColor(color);
        }
    }

    private static void applyAxolotlState(Axolotl axolotl, CompoundTag variantNbt) {
        if (axolotl == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        if (!variantNbt.contains("Variant", Tag.TAG_ANY_NUMERIC)) {
            return;
        }
        Variant[] values = Variant.values();
        if (values.length == 0) {
            return;
        }
        int index = Math.max(0, Math.min(values.length - 1, variantNbt.getInt("Variant")));
        axolotl.setVariant(values[index]);
    }

    private static void applyVillagerVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        Object villagerData = invokeNoArg(entity, "getVillagerData");
        if (villagerData == null) {
            return;
        }

        CompoundTag villagerDataTag = NbtCompat.getCompoundOrNull(variantNbt, "VillagerData");
        String professionRaw = readVariantString(variantNbt, "VillagerProfession", "Profession", "profession");
        if ((professionRaw == null || professionRaw.isBlank()) && villagerDataTag != null) {
            professionRaw = NbtCompat.getStringOr(villagerDataTag, "profession", "");
        }
        String typeRaw = readVariantString(variantNbt, "VillagerType", "Type", "type");
        if ((typeRaw == null || typeRaw.isBlank()) && villagerDataTag != null) {
            typeRaw = NbtCompat.getStringOr(villagerDataTag, "type", "");
        }

        ResourceLocation professionId = parseResourceLocation(professionRaw);
        ResourceLocation typeId = parseResourceLocation(typeRaw);

        if (professionId != null) {
            Object profession = resolveRegistryValue("VILLAGER_PROFESSION", professionId);
            if (profession != null) {
                Object wrapped = wrapAsHolder(getBuiltInRegistryObject("VILLAGER_PROFESSION"), profession);
                Object professionArg = wrapped != null ? wrapped : profession;
                Object updatedVillagerData = invokeOneArg(villagerData, "setProfession", professionArg);
                if (updatedVillagerData != null) {
                    villagerData = updatedVillagerData;
                }
            }
        }

        if (typeId != null) {
            Object villagerType = resolveRegistryValue("VILLAGER_TYPE", typeId);
            if (villagerType != null) {
                Object wrapped = wrapAsHolder(getBuiltInRegistryObject("VILLAGER_TYPE"), villagerType);
                Object typeArg = wrapped != null ? wrapped : villagerType;
                Object updatedVillagerData = invokeOneArg(villagerData, "setType", typeArg);
                if (updatedVillagerData != null) {
                    villagerData = updatedVillagerData;
                }
            }
        }

        int level = 0;
        if (variantNbt.contains("VillagerLevel", Tag.TAG_ANY_NUMERIC)) {
            level = variantNbt.getInt("VillagerLevel");
        } else if (villagerDataTag != null && villagerDataTag.contains("level", Tag.TAG_ANY_NUMERIC)) {
            level = villagerDataTag.getInt("level");
        }
        if (level > 0) {
            Object updatedVillagerData = invokeIntArg(villagerData, "setLevel", Math.max(1, level));
            if (updatedVillagerData != null) {
                villagerData = updatedVillagerData;
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
        String raw = readVariantString(variantNbt, nbtKey, "variant", "Variant");
        ResourceLocation variantId = parseResourceLocation(raw);
        if (variantId == null) {
            return;
        }
        Object variant = resolveRegistryValue(registryField, variantId);
        if (variant == null) {
            Identity2.LOGGER.debug("Unable to resolve vanilla variant {} for {}", variantId, registryField);
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

    private static boolean isSupportedVanillaVariantType(EntityType<?> type) {
        return type == EntityType.SHEEP
                || type == EntityType.AXOLOTL
                || type == EntityType.CAT
                || type == EntityType.WOLF
                || type == EntityType.FROG;
    }

    private static List<IdentityVariant> discoverSheepVariants(ResourceLocation typeId) {
        List<IdentityVariant> variants = new ArrayList<>(16);
        for (int i = 0; i < 16; i++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putByte("Color", (byte) i);
            DyeColor color = DyeColor.byId(i);
            variants.add(new IdentityVariant(typeId, "Sheep " + capitalize(color.getName()), nbt));
        }
        return variants;
    }

    private static List<IdentityVariant> discoverAxolotlVariants(ResourceLocation typeId) {
        List<IdentityVariant> variants = new ArrayList<>();
        Variant[] values = Variant.values();
        for (int i = 0; i < values.length; i++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Variant", i);
            variants.add(new IdentityVariant(typeId, "Axolotl " + capitalize(values[i].name()), nbt));
        }
        return variants;
    }

    private static IdentityVariant discoverBabyVariant(EntityType<?> type, ResourceLocation typeId, ClientLevel level) {
        if (type == null || typeId == null || level == null) {
            return null;
        }
        Entity baseline;
        Entity babyProbe;
        try {
            baseline = type.create(level);
            babyProbe = type.create(level);
        } catch (Throwable ignored) {
            baseline = null;
            babyProbe = null;
        }
        if (baseline == null || babyProbe == null) {
            return null;
        }
        if (!(babyProbe instanceof AgeableMob ageable)) {
            return null;
        }

        CompoundTag baselineData = IdentityVariantNbtHelper.writeEntityData(baseline);
        if (baselineData == null || baselineData.isEmpty()) {
            return null;
        }

        ageable.setBaby(true);
        ageable.setAge(-24000);
        CompoundTag babyData = IdentityVariantNbtHelper.writeEntityData(babyProbe);
        if (babyData == null || babyData.isEmpty()) {
            return null;
        }

        CompoundTag diff = IdentityVariantNbtHelper.computeVariantDiff(baselineData, babyData);
        if (diff.isEmpty()) {
            diff.putBoolean("IsBaby", true);
        }
        if (diff.isEmpty()) {
            return null;
        }
        return new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", diff);
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
        CompoundTag sanitized = IdentityProgression.normalizeVariantForUnlock(variant.variantNbt());
        if (sanitized == null || sanitized.isEmpty()) {
            return;
        }
        IdentityVariant normalized = new IdentityVariant(variant.entityTypeId(), variant.displayName(), sanitized);
        String token = IdentityProgression.toVariantUnlockToken(normalized.variantNbt());
        out.putIfAbsent(token, normalized);
    }

    private static void putRegistryKey(CompoundTag variant, String key, @Nullable ResourceLocation id) {
        if (variant == null || key == null || key.isBlank() || id == null) {
            return;
        }
        variant.putString(key, id.toString());
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
            return cast.getKey(value);
        } catch (Throwable ignored) {
            Object unwrapped = unwrapHolderValue(value);
            if (unwrapped != value) {
                try {
                    return cast.getKey(unwrapped);
                } catch (Throwable ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
    }

    private static int clampDyeId(@Nullable DyeColor color) {
        if (color == null) {
            return 0;
        }
        return Math.max(0, color.getId());
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
                ResourceLocation location = resourceKey.location();
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
            ResourceLocation location = registryKey.location();
            if (location == null || BuiltInRegistries.REGISTRY == null) {
                return null;
            }
            return BuiltInRegistries.REGISTRY.get(location);
        } catch (Throwable ignored) {
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

    @Nullable
    private static String readVariantString(CompoundTag variantNbt, String... keys) {
        if (variantNbt == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!variantNbt.contains(key, Tag.TAG_STRING)) {
                continue;
            }
            String value = NbtCompat.getStringOr(variantNbt, key, "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.contains(":")) {
                return new ResourceLocation(raw);
            }
            return new ResourceLocation("minecraft", raw);
        } catch (Exception ignored) {
            return null;
        }
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
