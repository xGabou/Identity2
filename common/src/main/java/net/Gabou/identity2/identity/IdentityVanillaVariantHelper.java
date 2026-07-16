package net.Gabou.identity2.identity;

import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.mixin.GoatAccessor;
import net.Gabou.identity2.mixin.HorseAccessor;
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
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.axolotl.Axolotl.Variant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
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
        } else if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            addVariants(out, discoverSlimeSizeVariants(typeId));
        } else if (type == EntityType.VILLAGER) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "VILLAGER_TYPE", "VillagerType", "Villager"));
        } else if (type == EntityType.AXOLOTL) {
            addVariants(out, discoverAxolotlVariants(typeId));
        } else if (type == EntityType.CAT) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "CAT_VARIANT", "CatVariant", "Cat"));
        } else if (type == EntityType.HORSE) {
            addVariants(out, discoverHorseVariants(typeId));
        } else if (type == EntityType.WOLF) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "WOLF_VARIANT", "WolfVariant", "Wolf"));
        } else if (type == EntityType.FROG) {
            addVariants(out, discoverRegistryBackedVariants(typeId, "FROG_VARIANT", "FrogVariant", "Frog"));
        }

        IdentityVariant babyVariant = discoverBabyVariant(type, typeId, level);
        if (babyVariant != null) {
            addVariant(out, babyVariant);
            if (type == EntityType.VILLAGER || type == EntityType.AXOLOTL || type == EntityType.CAT || type == EntityType.HORSE) {
                addVariants(out, discoverBabyCopies(new ArrayList<>(out.values()), " Baby"));
            }
        } else if (type == EntityType.AXOLOTL) {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("IsBaby", true);
            nbt.putInt("Age", -24000);
            addVariant(out, new IdentityVariant(typeId, "Axolotl Baby", nbt));
            addVariants(out, discoverBabyCopies(new ArrayList<>(out.values()), " Baby"));
        } else if (type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.HUSK || type == EntityType.DROWNED) {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("IsBaby", true);
            addVariant(out, new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", nbt));
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
            putRegistryKey(variant, "CatVariant", BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant()));
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

        if (entity instanceof Horse horse) {
            variant.putInt("Variant", packHorseVariant(horse.getVariant(), horse.getMarkings()));
        }

        if (entity instanceof Goat goat) {
            variant.putBoolean("HasLeftHorn", goat.hasLeftHorn());
            variant.putBoolean("HasRightHorn", goat.hasRightHorn());
            variant.putBoolean("IsScreamingGoat", goat.isScreamingGoat());
        }

        if (entity instanceof Villager villager) {
            VillagerData data = villager.getVillagerData();
            putRegistryKey(variant, "VillagerType", BuiltInRegistries.VILLAGER_TYPE.getKey(data.getType()));
            putRegistryKey(variant, "VillagerProfession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession()));
            variant.putInt("VillagerLevel", data.getLevel());
        }

        return variant;
    }

    public static void applyVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null || variantNbt == null) {
            return;
        }

        if (entity instanceof AgeableMob ageable) {
            applyAgeableState(ageable, variantNbt);
        }

        if (entity instanceof Sheep sheep) {
            applySheepState(sheep, variantNbt);
        }

        if (entity instanceof Cat cat) {
            applyCatVariant(cat, variantNbt);
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
            applyFrogVariant(frog, variantNbt);
        }

        if (entity instanceof Axolotl axolotl) {
            applyAxolotlState(axolotl, variantNbt);
        }

        if (entity instanceof Horse horse) {
            applyHorseState(horse, variantNbt);
        }

        if (entity instanceof Goat goat) {
            applyGoatState(goat, variantNbt);
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

    private static void applyHorseState(Horse horse, CompoundTag variantNbt) {
        if (horse == null || variantNbt == null || !variantNbt.contains("Variant", Tag.TAG_ANY_NUMERIC)) {
            return;
        }
        int packed = variantNbt.getInt("Variant");
        net.minecraft.world.entity.animal.horse.Variant coat = net.minecraft.world.entity.animal.horse.Variant.byId(packed & 255);
        Markings markings = Markings.byId((packed >> 8) & 255);
        ((HorseAccessor) horse).identity2$setVariantAndMarkings(coat, markings);
    }

    private static void applyGoatState(Goat goat, CompoundTag variantNbt) {
        if (goat == null || variantNbt == null) {
            return;
        }
        boolean hasExplicitHornState = variantNbt.contains("HasLeftHorn", Tag.TAG_BYTE)
                || variantNbt.contains("HasRightHorn", Tag.TAG_BYTE);
        boolean leftHorn = hasExplicitHornState
                ? NbtCompat.getBooleanOr(variantNbt, "HasLeftHorn", true)
                : true;
        boolean rightHorn = hasExplicitHornState
                ? NbtCompat.getBooleanOr(variantNbt, "HasRightHorn", true)
                : true;
        goat.getEntityData().set(GoatAccessor.identity2$getHasLeftHornData(), leftHorn);
        goat.getEntityData().set(GoatAccessor.identity2$getHasRightHornData(), rightHorn);
        if (variantNbt.contains("IsScreamingGoat", Tag.TAG_BYTE)) {
            goat.setScreamingGoat(NbtCompat.getBooleanOr(variantNbt, "IsScreamingGoat", false));
        }
    }

    private static void applyVillagerVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        if (entity instanceof Villager villager) {
            applyDirectVillagerVariantData(villager, variantNbt);
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

    private static void applyDirectVillagerVariantData(Villager villager, CompoundTag variantNbt) {
        VillagerData data = villager.getVillagerData();
        CompoundTag nested = NbtCompat.getCompoundOrNull(variantNbt, "VillagerData");

        String professionRaw = readVariantString(variantNbt, "VillagerProfession", "Profession", "profession");
        if ((professionRaw == null || professionRaw.isBlank()) && nested != null) {
            professionRaw = NbtCompat.getStringOr(nested, "profession", "");
        }
        ResourceLocation professionId = parseResourceLocation(professionRaw);
        if (professionId != null && BuiltInRegistries.VILLAGER_PROFESSION.containsKey(professionId)) {
            data = data.setProfession(BuiltInRegistries.VILLAGER_PROFESSION.get(professionId));
        }

        String typeRaw = readVariantString(variantNbt, "VillagerType", "Type", "type");
        if ((typeRaw == null || typeRaw.isBlank()) && nested != null) {
            typeRaw = NbtCompat.getStringOr(nested, "type", "");
        }
        ResourceLocation typeId = parseResourceLocation(typeRaw);
        if (typeId != null && BuiltInRegistries.VILLAGER_TYPE.containsKey(typeId)) {
            data = data.setType(BuiltInRegistries.VILLAGER_TYPE.get(typeId));
        }

        int level = variantNbt.contains("VillagerLevel", Tag.TAG_ANY_NUMERIC)
                ? variantNbt.getInt("VillagerLevel")
                : nested != null ? nested.getInt("level") : data.getLevel();
        data = data.setLevel(Math.max(1, level));
        villager.setVillagerData(data);
        villager.setOffers(new net.minecraft.world.item.trading.MerchantOffers());
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

    private static void applyCatVariant(Cat cat, CompoundTag variantNbt) {
        String raw = readVariantString(variantNbt, "CatVariant", "variant", "Variant");
        ResourceLocation variantId = parseResourceLocation(raw);
        if (variantId == null) {
            return;
        }
        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(variantId);
        if (variant != null) {
            cat.setVariant(variant);
        }
    }

    private static void applyFrogVariant(Frog frog, CompoundTag variantNbt) {
        String raw = readVariantString(variantNbt, "FrogVariant", "variant", "Variant");
        ResourceLocation variantId = parseResourceLocation(raw);
        if (variantId == null) {
            return;
        }
        FrogVariant variant = BuiltInRegistries.FROG_VARIANT.get(variantId);
        if (variant != null) {
            frog.setVariant(variant);
        }
    }

    private static boolean isSupportedVanillaVariantType(EntityType<?> type) {
        return type == EntityType.SHEEP
                || type == EntityType.SLIME
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.VILLAGER
                || type == EntityType.AXOLOTL
                || type == EntityType.CAT
                || type == EntityType.HORSE
                || type == EntityType.GOAT
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

    private static List<IdentityVariant> discoverSlimeSizeVariants(ResourceLocation typeId) {
        String prefix = capitalize(typeId.getPath().replace('_', ' '));
        List<IdentityVariant> variants = new ArrayList<>();
        // Slime NBT stores actual size minus one.
        int[] sizes = new int[] {0, 1, 3};
        String[] labels = new String[] {"Small", "Medium", "Large"};
        for (int i = 0; i < sizes.length; i++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Size", sizes[i]);
            variants.add(new IdentityVariant(typeId, prefix + " " + labels[i], nbt));
        }
        return variants;
    }

    private static List<IdentityVariant> discoverHorseVariants(ResourceLocation typeId) {
        List<IdentityVariant> variants = new ArrayList<>(
                net.minecraft.world.entity.animal.horse.Variant.values().length * Markings.values().length
        );
        for (net.minecraft.world.entity.animal.horse.Variant coat : net.minecraft.world.entity.animal.horse.Variant.values()) {
            for (Markings markings : Markings.values()) {
                CompoundTag nbt = new CompoundTag();
                nbt.putInt("Variant", packHorseVariant(coat, markings));
                String label = "Horse " + capitalize(coat.getSerializedName().replace('_', ' '));
                if (markings != Markings.NONE) {
                    label += " " + capitalize(markings.name().replace('_', ' '));
                }
                variants.add(new IdentityVariant(typeId, label, nbt));
            }
        }
        return variants;
    }

    private static int packHorseVariant(net.minecraft.world.entity.animal.horse.Variant coat, Markings markings) {
        return (coat.getId() & 255) | ((markings.getId() << 8) & 65280);
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

    private static IdentityVariant discoverBabyVariant(EntityType<?> type, ResourceLocation typeId, Level level) {
        if (type == null || typeId == null || level == null || IdentityApi.isBabyVariantBlocked(type)) {
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
        diff.putBoolean("IsBaby", true);
        diff.putInt("Age", -24000);
        if (diff.isEmpty()) {
            return null;
        }
        return new IdentityVariant(typeId, capitalize(typeId.getPath().replace('_', ' ')) + " Baby", diff);
    }

    private static List<IdentityVariant> discoverBabyCopies(List<IdentityVariant> variants, String suffix) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        List<IdentityVariant> out = new ArrayList<>();
        for (IdentityVariant variant : variants) {
            if (variant == null || variant.variantNbt() == null || variant.variantNbt().isEmpty()
                    || variant.variantNbt().contains("IsBaby")) {
                continue;
            }
            CompoundTag nbt = variant.variantNbt().copy();
            nbt.putBoolean("IsBaby", true);
            nbt.putInt("Age", -24000);
            out.add(new IdentityVariant(variant.entityTypeId(), variant.displayName() + suffix, nbt));
        }
        return out;
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
