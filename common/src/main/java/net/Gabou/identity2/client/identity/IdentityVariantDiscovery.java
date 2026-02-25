package net.Gabou.identity2.client.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.core.Registry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public final class IdentityVariantDiscovery {
    private static final List<String> CAT_VARIANT_FALLBACK_IDS = List.of(
        "minecraft:tabby",
        "minecraft:black",
        "minecraft:red",
        "minecraft:siamese",
        "minecraft:british_shorthair",
        "minecraft:calico",
        "minecraft:persian",
        "minecraft:ragdoll",
        "minecraft:white",
        "minecraft:jellie",
        "minecraft:all_black"
    );
    private static final List<String> WOLF_VARIANT_FALLBACK_IDS = List.of(
        "minecraft:pale",
        "minecraft:spotted",
        "minecraft:snowy",
        "minecraft:black",
        "minecraft:ashen",
        "minecraft:rusty",
        "minecraft:woods",
        "minecraft:chestnut",
        "minecraft:striped"
    );
    private static final List<String> FROG_VARIANT_FALLBACK_IDS = List.of(
        "minecraft:temperate",
        "minecraft:warm",
        "minecraft:cold"
    );
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
        "Age",
        "IsBaby",
        "Baby",
        "VillagerLevel"
    );
    private static final int MAX_NUMERIC_VARIANT_VALUE = 31;
    private static final int MAX_VARIANT_SAMPLE_COUNT = 32;

    private IdentityVariantDiscovery() {
    }

    public static List<IdentityVariant> discover(EntityType<?> type, ClientLevel world) {
        if (type == null || world == null) {
            return List.of(defaultVariant(type));
        }

        try {
            ResourceLocation typeId = EntityType.getKey(type);
            if (typeId == null) {
                return List.of(defaultVariant(type));
            }

            List<IdentityVariant> variants = discoverKnownVariants(type, typeId, world);
            if (variants.isEmpty()) {
                variants = discoverGenericVariants(type, typeId, world);
            }
            if (variants.isEmpty()) {
                variants = List.of(defaultVariant(typeId));
            }
            return withBabyVariants(type, typeId, world, variants);
        } catch (Throwable ignored) {
            return List.of(defaultVariant(type));
        }
    }

    private static List<IdentityVariant> discoverKnownVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        if (type == EntityType.SHEEP) {
            List<IdentityVariant> variants = new ArrayList<>(16);
            for (int i = 0; i < 16; i++) {
                CompoundTag nbt = new CompoundTag();
                nbt.putByte("Color", (byte) i);
                DyeColor color = DyeColor.byId(i);
                variants.add(new IdentityVariant(typeId, "Sheep " + capitalize(color.getName()), nbt));
            }
            return variants;
        }

        if (type == EntityType.AXOLOTL) {
            List<IdentityVariant> variants = new ArrayList<>(5);
            String[] names = {"Lucy", "Wild", "Gold", "Cyan", "Blue"};
            for (int i = 0; i < names.length; i++) {
                CompoundTag nbt = new CompoundTag();
                nbt.putInt("Variant", i);
                variants.add(new IdentityVariant(typeId, "Axolotl " + names[i], nbt));
            }
            return variants;
        }

        if (type == EntityType.CAT) {
            List<IdentityVariant> catVariants = discoverRegistryVariants(typeId, "CAT_VARIANT", "CatVariant", "Cat");
            if (!catVariants.isEmpty()) {
                return catVariants;
            }
            catVariants = discoverStringVariantsFromSamples(type, typeId, world, Set.of("CatVariant", "variant", "Variant"), "Cat", "CatVariant");
            if (!catVariants.isEmpty()) {
                return catVariants;
            }
            return buildFallbackStringVariants(typeId, "Cat", "CatVariant", CAT_VARIANT_FALLBACK_IDS);
        }

        if (type == EntityType.WOLF) {
            List<IdentityVariant> wolfVariants = discoverRegistryVariants(typeId, "WOLF_VARIANT", "WolfVariant", "Wolf");
            if (!wolfVariants.isEmpty()) {
                return wolfVariants;
            }
            wolfVariants = discoverStringVariantsFromSamples(type, typeId, world, Set.of("WolfVariant", "variant", "Variant"), "Wolf", "WolfVariant");
            if (!wolfVariants.isEmpty()) {
                return wolfVariants;
            }
            return buildFallbackStringVariants(typeId, "Wolf", "WolfVariant", WOLF_VARIANT_FALLBACK_IDS);
        }

        if (type == EntityType.FROG) {
            List<IdentityVariant> frogVariants = discoverRegistryVariants(typeId, "FROG_VARIANT", "FrogVariant", "Frog");
            if (!frogVariants.isEmpty()) {
                return frogVariants;
            }
            frogVariants = discoverStringVariantsFromSamples(type, typeId, world, Set.of("FrogVariant", "variant", "Variant"), "Frog", "FrogVariant");
            if (!frogVariants.isEmpty()) {
                return frogVariants;
            }
            return buildFallbackStringVariants(typeId, "Frog", "FrogVariant", FROG_VARIANT_FALLBACK_IDS);
        }

        return List.of();
    }

    private static List<IdentityVariant> discoverGenericVariants(EntityType<?> type, ResourceLocation typeId, ClientLevel world) {
        Entity baseEntity = createEntity(type, world);
        if (baseEntity == null) {
            return List.of();
        }

        CompoundTag baseline = writeEntityData(baseEntity, world);
        if (baseline == null || baseline.isEmpty()) {
            return List.of();
        }

        String key = findCandidateKey(baseline);
        if (key == null) {
            return List.of();
        }

        NumericKind numericKind = detectNumericKind(baseline, key);
        if (numericKind == null) {
            // String variants are not guessed dynamically by design.
            return List.of();
        }

        List<IdentityVariant> variants = new ArrayList<>();
        for (int value = 0; value <= MAX_NUMERIC_VARIANT_VALUE; value++) {
            CompoundTag probe = baseline.copy();
            putNumeric(probe, key, numericKind, value);

            Entity candidate = createEntity(type, world);
            if (candidate == null) {
                continue;
            }
            if (!readEntityData(candidate, world, probe)) {
                continue;
            }

            CompoundTag roundTrip = writeEntityData(candidate, world);
            if (roundTrip == null || !matchesNumeric(roundTrip, key, numericKind, value)) {
                continue;
            }

            CompoundTag variant = new CompoundTag();
            putNumeric(variant, key, numericKind, value);
            String displayName = buildGenericDisplayName(typeId, key, value);
            variants.add(new IdentityVariant(typeId, displayName, variant));
        }

        return variants;
    }

    private static String findCandidateKey(CompoundTag baseline) {
        for (String key : CANDIDATE_KEYS) {
            if (baseline.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private static String buildGenericDisplayName(ResourceLocation typeId, String key, int value) {
        String entityName = capitalize(typeId.getPath().replace('_', ' '));
        if ("Color".equals(key) && typeId.equals(EntityType.getKey(EntityType.SHEEP))) {
            DyeColor color = DyeColor.byId(value % 16);
            return "Sheep " + capitalize(color.getName());
        }
        return entityName + " " + key + " " + value;
    }

    private static Entity createEntity(EntityType<?> type, ClientLevel world) {
        try {
            return type.create(world, EntitySpawnReason.COMMAND);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<IdentityVariant> discoverRegistryVariants(
        ResourceLocation typeId,
        String registryField,
        String nbtKey,
        String prefix
    ) {
        Registry<?> registry = getBuiltInRegistry(registryField);
        if (registry == null) {
            return List.of();
        }

        Map<String, IdentityVariant> variants = new LinkedHashMap<>();
        for (Object value : registry) {
            ResourceLocation variantId = resolveRegistryKey(registry, value);
            if (variantId == null) {
                continue;
            }
            CompoundTag nbt = createVariantStringNbt(nbtKey, variantId.toString());
            String display = prefix + " " + capitalize(variantId.getPath().replace('_', ' '));
            variants.putIfAbsent(variantId.toString(), new IdentityVariant(typeId, display, nbt));
        }
        return new ArrayList<>(variants.values());
    }

    private static List<IdentityVariant> discoverStringVariantsFromSamples(
        EntityType<?> type,
        ResourceLocation typeId,
        ClientLevel world,
        Set<String> keys,
        String prefix,
        String preferredKey
    ) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (int i = 0; i < MAX_VARIANT_SAMPLE_COUNT; i++) {
            Entity entity = createEntity(type, world);
            if (entity == null) {
                continue;
            }

            CompoundTag nbt = writeEntityData(entity, world);
            if (nbt == null) {
                continue;
            }

            for (String key : keys) {
                String value = nbt.getStringOr(key, "");
                if (value.isBlank()) {
                    continue;
                }
                CompoundTag variant = createVariantStringNbt(preferredKey, value);
                if (!key.equals(preferredKey)) {
                    variant.putString(key, value);
                }
                String display = prefix + " " + capitalize(value.replace('_', ' '));
                out.putIfAbsent(key + "=" + value, new IdentityVariant(typeId, display, variant));
            }
        }
        return new ArrayList<>(out.values());
    }

    private static List<IdentityVariant> buildFallbackStringVariants(
        ResourceLocation typeId,
        String prefix,
        String preferredKey,
        List<String> fallbackIds
    ) {
        if (typeId == null || prefix == null || preferredKey == null || fallbackIds == null || fallbackIds.isEmpty()) {
            return List.of();
        }
        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (String raw : fallbackIds) {
            ResourceLocation variantId = parseResourceLocation(raw);
            if (variantId == null) {
                continue;
            }
            String value = variantId.toString();
            CompoundTag variant = createVariantStringNbt(preferredKey, value);
            String display = prefix + " " + capitalize(variantId.getPath().replace('_', ' '));
            out.putIfAbsent(value, new IdentityVariant(typeId, display, variant));
        }
        return new ArrayList<>(out.values());
    }

    private static CompoundTag createVariantStringNbt(String preferredKey, String value) {
        CompoundTag variant = new CompoundTag();
        if (value == null || value.isBlank()) {
            return variant;
        }
        if (preferredKey != null && !preferredKey.isBlank()) {
            variant.putString(preferredKey, value);
        }
        variant.putString("variant", value);
        variant.putString("Variant", value);
        return variant;
    }

    private static ResourceLocation parseResourceLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.contains(":")) {
                return ResourceLocation.parse(raw);
            }
            return ResourceLocation.fromNamespaceAndPath("minecraft", raw);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<IdentityVariant> withBabyVariants(
        EntityType<?> type,
        ResourceLocation typeId,
        ClientLevel world,
        List<IdentityVariant> baseVariants
    ) {
        if (!supportsBabyVariants(type, world)) {
            return baseVariants;
        }

        List<IdentityVariant> out = new ArrayList<>(Math.max(2, baseVariants.size() * 2));
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (IdentityVariant variant : baseVariants) {
            if (variant == null) {
                continue;
            }
            String baseKey = variant.variantNbt().toString();
            if (keys.add(baseKey)) {
                out.add(variant);
            }

            CompoundTag babyNbt = variant.variantNbt().copy();
            babyNbt.putBoolean("IsBaby", true);
            babyNbt.putBoolean("Baby", true);
            int age = babyNbt.getInt("Age").isPresent() ? babyNbt.getInt("Age").get() : -24000;
            if (age >= 0) {
                age = -24000;
            }
            babyNbt.putInt("Age", age);

            String babyKey = babyNbt.toString();
            if (!keys.add(babyKey)) {
                continue;
            }
            out.add(new IdentityVariant(typeId, variant.displayName() + " (Baby)", babyNbt));
        }
        return out.isEmpty() ? baseVariants : out;
    }

    private static boolean supportsBabyVariants(EntityType<?> type, ClientLevel world) {
        Entity entity = createEntity(type, world);
        if (entity == null) {
            return false;
        }

        CompoundTag nbt = writeEntityData(entity, world);
        if (nbt != null && (nbt.contains("Age") || nbt.contains("IsBaby") || nbt.contains("Baby"))) {
            return true;
        }
        return hasBooleanSetter(entity.getClass(), "setBaby") || hasIntSetter(entity.getClass(), "setAge");
    }

    private static boolean hasBooleanSetter(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param == boolean.class || param == Boolean.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasIntSetter(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param == int.class || param == Integer.class) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Registry<?> getBuiltInRegistry(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Object value = net.minecraft.core.registries.BuiltInRegistries.class.getField(fieldName).get(null);
            if (value instanceof Registry<?> registry) {
                return registry;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object registryKeyObj = net.minecraft.core.registries.Registries.class.getField(fieldName).get(null);
            if (!(registryKeyObj instanceof net.minecraft.resources.ResourceKey<?> registryKey)) {
                return null;
            }
            ResourceLocation location = registryKey.location();
            if (location == null || net.minecraft.core.registries.BuiltInRegistries.REGISTRY == null) {
                return null;
            }
            Object registry = net.minecraft.core.registries.BuiltInRegistries.REGISTRY.getValue(location);
            if (registry instanceof Registry<?> typedRegistry) {
                return typedRegistry;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceLocation resolveRegistryKey(Registry<?> registry, Object value) {
        if (registry == null || value == null) {
            return null;
        }
        try {
            Registry<Object> cast = (Registry<Object>) registry;
            return cast.getKey(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CompoundTag writeEntityData(Entity entity, ClientLevel world) {
        try {
            TagValueOutput writeView = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
            entity.saveWithoutId(writeView);
            return writeView.buildResult();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean readEntityData(Entity entity, ClientLevel world, CompoundTag nbt) {
        try {
            ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), nbt);
            entity.load(readView);
            return true;
        } catch (Throwable ignored) {
            return false;
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
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
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

    private static NumericKind detectNumericKind(CompoundTag nbt, String key) {
        if (nbt.getByte(key).isPresent()) {
            return NumericKind.BYTE;
        }
        if (nbt.getShort(key).isPresent()) {
            return NumericKind.SHORT;
        }
        if (nbt.getInt(key).isPresent()) {
            return NumericKind.INT;
        }
        if (nbt.getLong(key).isPresent()) {
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
            case BYTE -> nbt.getByte(key).isPresent() && nbt.getByte(key).get() == (byte) expected;
            case SHORT -> nbt.getShort(key).isPresent() && nbt.getShort(key).get() == (short) expected;
            case INT -> nbt.getInt(key).isPresent() && nbt.getInt(key).get() == expected;
            case LONG -> nbt.getLong(key).isPresent() && nbt.getLong(key).get() == expected;
        };
    }

    private enum NumericKind {
        BYTE,
        SHORT,
        INT,
        LONG
    }
}
