package net.Gabou.identity2.client.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public final class IdentityVariantDiscovery {
    private static final List<String> CANDIDATE_KEYS = List.of("Color", "Variant", "variant", "Type", "type", "Skin", "skin", "Form", "form");
    private static final int MAX_NUMERIC_VARIANT_VALUE = 31;
    private static final int MAX_CAT_SAMPLES = 32;

    private IdentityVariantDiscovery() {
    }

    public static List<IdentityVariant> discover(EntityType<?> type, ClientLevel world) {
        if (type == null || world == null) {
            return List.of(defaultVariant(type));
        }

        try {
            Identifier typeId = EntityType.getKey(type);
            if (typeId == null) {
                return List.of(defaultVariant(type));
            }

            List<IdentityVariant> known = discoverKnownVariants(type, typeId, world);
            if (!known.isEmpty()) {
                return known;
            }

            List<IdentityVariant> generic = discoverGenericVariants(type, typeId, world);
            if (!generic.isEmpty()) {
                return generic;
            }

            return List.of(defaultVariant(typeId));
        } catch (Throwable ignored) {
            return List.of(defaultVariant(type));
        }
    }

    private static List<IdentityVariant> discoverKnownVariants(EntityType<?> type, Identifier typeId, ClientLevel world) {
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
            List<IdentityVariant> catVariants = discoverCatStringVariants(type, typeId, world);
            if (!catVariants.isEmpty()) {
                return catVariants;
            }
        }

        return List.of();
    }

    private static List<IdentityVariant> discoverCatStringVariants(EntityType<?> type, Identifier typeId, ClientLevel world) {
        Set<String> keys = Set.of("variant", "Variant");
        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (int i = 0; i < MAX_CAT_SAMPLES; i++) {
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
                CompoundTag variant = new CompoundTag();
                variant.putString(key, value);
                out.putIfAbsent(value, new IdentityVariant(typeId, "Cat " + capitalize(value), variant));
            }
        }

        return new ArrayList<>(out.values());
    }

    private static List<IdentityVariant> discoverGenericVariants(EntityType<?> type, Identifier typeId, ClientLevel world) {
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

    private static String buildGenericDisplayName(Identifier typeId, String key, int value) {
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
        Identifier typeId = type == null ? Identifier.parse("minecraft:pig") : EntityType.getKey(type);
        return defaultVariant(typeId);
    }

    private static IdentityVariant defaultVariant(Identifier typeId) {
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
