package net.Gabou.identity2.client.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.DyeColor;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;

public final class IdentityVariantDiscovery {
    private static final List<String> CANDIDATE_KEYS = List.of("Color", "Variant", "variant", "Type", "type", "Skin", "skin", "Form", "form");
    private static final int MAX_NUMERIC_VARIANT_VALUE = 31;
    private static final int MAX_CAT_SAMPLES = 32;

    private IdentityVariantDiscovery() {
    }

    public static List<IdentityVariant> discover(EntityType<?> type, ClientWorld world) {
        if (type == null || world == null) {
            return List.of(defaultVariant(type));
        }

        try {
            Identifier typeId = EntityType.getId(type);
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

    private static List<IdentityVariant> discoverKnownVariants(EntityType<?> type, Identifier typeId, ClientWorld world) {
        if (type == EntityType.SHEEP) {
            List<IdentityVariant> variants = new ArrayList<>(16);
            for (int i = 0; i < 16; i++) {
                NbtCompound nbt = new NbtCompound();
                nbt.putByte("Color", (byte) i);
                DyeColor color = DyeColor.byIndex(i);
                variants.add(new IdentityVariant(typeId, "Sheep " + capitalize(color.getId()), nbt));
            }
            return variants;
        }

        if (type == EntityType.AXOLOTL) {
            List<IdentityVariant> variants = new ArrayList<>(5);
            String[] names = {"Lucy", "Wild", "Gold", "Cyan", "Blue"};
            for (int i = 0; i < names.length; i++) {
                NbtCompound nbt = new NbtCompound();
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

    private static List<IdentityVariant> discoverCatStringVariants(EntityType<?> type, Identifier typeId, ClientWorld world) {
        Set<String> keys = Set.of("variant", "Variant");
        Map<String, IdentityVariant> out = new LinkedHashMap<>();
        for (int i = 0; i < MAX_CAT_SAMPLES; i++) {
            Entity entity = createEntity(type, world);
            if (entity == null) {
                continue;
            }

            NbtCompound nbt = writeEntityData(entity, world);
            if (nbt == null) {
                continue;
            }

            for (String key : keys) {
                String value = nbt.getString(key, "");
                if (value.isBlank()) {
                    continue;
                }
                NbtCompound variant = new NbtCompound();
                variant.putString(key, value);
                out.putIfAbsent(value, new IdentityVariant(typeId, "Cat " + capitalize(value), variant));
            }
        }

        return new ArrayList<>(out.values());
    }

    private static List<IdentityVariant> discoverGenericVariants(EntityType<?> type, Identifier typeId, ClientWorld world) {
        Entity baseEntity = createEntity(type, world);
        if (baseEntity == null) {
            return List.of();
        }

        NbtCompound baseline = writeEntityData(baseEntity, world);
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
            NbtCompound probe = baseline.copy();
            putNumeric(probe, key, numericKind, value);

            Entity candidate = createEntity(type, world);
            if (candidate == null) {
                continue;
            }
            if (!readEntityData(candidate, world, probe)) {
                continue;
            }

            NbtCompound roundTrip = writeEntityData(candidate, world);
            if (roundTrip == null || !matchesNumeric(roundTrip, key, numericKind, value)) {
                continue;
            }

            NbtCompound variant = new NbtCompound();
            putNumeric(variant, key, numericKind, value);
            String displayName = buildGenericDisplayName(typeId, key, value);
            variants.add(new IdentityVariant(typeId, displayName, variant));
        }

        return variants;
    }

    private static String findCandidateKey(NbtCompound baseline) {
        for (String key : CANDIDATE_KEYS) {
            if (baseline.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private static String buildGenericDisplayName(Identifier typeId, String key, int value) {
        String entityName = capitalize(typeId.getPath().replace('_', ' '));
        if ("Color".equals(key) && typeId.equals(EntityType.getId(EntityType.SHEEP))) {
            DyeColor color = DyeColor.byIndex(value % 16);
            return "Sheep " + capitalize(color.getId());
        }
        return entityName + " " + key + " " + value;
    }

    private static Entity createEntity(EntityType<?> type, ClientWorld world) {
        try {
            return type.create(world, SpawnReason.COMMAND);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static NbtCompound writeEntityData(Entity entity, ClientWorld world) {
        try {
            NbtWriteView writeView = NbtWriteView.create(ErrorReporter.EMPTY, world.getRegistryManager());
            entity.writeData(writeView);
            return writeView.getNbt();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean readEntityData(Entity entity, ClientWorld world, NbtCompound nbt) {
        try {
            ReadView readView = NbtReadView.create(ErrorReporter.EMPTY, world.getRegistryManager(), nbt);
            entity.readData(readView);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static IdentityVariant defaultVariant(EntityType<?> type) {
        Identifier typeId = type == null ? Identifier.of("minecraft:pig") : EntityType.getId(type);
        return defaultVariant(typeId);
    }

    private static IdentityVariant defaultVariant(Identifier typeId) {
        return new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')), new NbtCompound());
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

    private static NumericKind detectNumericKind(NbtCompound nbt, String key) {
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

    private static void putNumeric(NbtCompound nbt, String key, NumericKind kind, int value) {
        switch (kind) {
            case BYTE -> nbt.putByte(key, (byte) value);
            case SHORT -> nbt.putShort(key, (short) value);
            case INT -> nbt.putInt(key, value);
            case LONG -> nbt.putLong(key, value);
        }
    }

    private static boolean matchesNumeric(NbtCompound nbt, String key, NumericKind kind, int expected) {
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
