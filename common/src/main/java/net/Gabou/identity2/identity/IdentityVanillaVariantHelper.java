package net.Gabou.identity2.identity;

import java.util.List;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.mixin.GoatAccessor;
import net.Gabou.identity2.mixin.HorseAccessor;
import net.Gabou.identity2.mixin.WolfAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Typed, remap-safe extraction and application for vanilla variants. */
public final class IdentityVanillaVariantHelper {
    private IdentityVanillaVariantHelper() {
    }

    public static List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        return type == null || level == null ? List.of() : IdentityApi.discoverVariants(type, level);
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
        } else if (entity instanceof Zombie zombie) {
            variant.putBoolean("IsBaby", zombie.isBaby());
        }

        if (entity instanceof Sheep sheep) {
            variant.putByte("Color", (byte) sheep.getColor().getId());
        }
        if (entity instanceof Slime slime) {
            variant.putInt("Size", Math.max(0, slime.getSize() - 1));
        }
        if (entity instanceof Cat cat) {
            putHolderKey(variant, "CatVariant", cat.getVariant());
        }
        if (entity instanceof Wolf wolf) {
            putHolderKey(variant, "WolfVariant", wolf.getVariant());
            variant.putInt("CollarColor", wolf.getCollarColor().getId());
        }
        if (entity instanceof Frog frog) {
            putHolderKey(variant, "FrogVariant", frog.getVariant());
        }
        if (entity instanceof Axolotl axolotl) {
            variant.putInt("Variant", axolotl.getVariant().ordinal());
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
        if (entity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }

        if (entity instanceof AgeableMob ageable) {
            applyAge(ageable, variantNbt);
        } else if (entity instanceof Zombie zombie && hasBabyFlag(variantNbt)) {
            zombie.setBaby(readBabyFlag(variantNbt));
        }
        if (entity instanceof Sheep sheep && variantNbt.contains("Color", Tag.TAG_ANY_NUMERIC)) {
            sheep.setColor(DyeColor.byId(Math.max(0, Math.min(15, variantNbt.getInt("Color")))));
        }
        if (entity instanceof Slime slime && variantNbt.contains("Size", Tag.TAG_ANY_NUMERIC)) {
            slime.setSize(Math.max(1, variantNbt.getInt("Size") + 1), false);
        }
        if (entity instanceof Cat cat) {
            applyCatVariant(cat, variantNbt);
        }
        if (entity instanceof Wolf wolf) {
            applyWolfVariant(wolf, variantNbt);
            if (variantNbt.contains("CollarColor", Tag.TAG_ANY_NUMERIC)) {
                ((WolfAccessor) wolf).identity2$setCollarColor(DyeColor.byId(variantNbt.getInt("CollarColor")));
            }
        }
        if (entity instanceof Frog frog) {
            applyFrogVariant(frog, variantNbt);
        }
        if (entity instanceof Axolotl axolotl && variantNbt.contains("Variant", Tag.TAG_ANY_NUMERIC)) {
            Axolotl.Variant[] values = Axolotl.Variant.values();
            axolotl.setVariant(values[Math.max(0, Math.min(values.length - 1, variantNbt.getInt("Variant")))]);
        }
        if (entity instanceof Horse horse && variantNbt.contains("Variant", Tag.TAG_ANY_NUMERIC)) {
            int packed = variantNbt.getInt("Variant");
            ((HorseAccessor) horse).identity2$setVariantAndMarkings(
                    net.minecraft.world.entity.animal.horse.Variant.byId(packed & 255),
                    Markings.byId((packed >> 8) & 255)
            );
        }
        if (entity instanceof Goat goat) {
            applyGoatVariant(goat, variantNbt);
        }
        if (entity instanceof Villager villager) {
            applyVillagerVariant(villager, variantNbt);
        }
    }

    private static void applyAge(AgeableMob ageable, CompoundTag nbt) {
        if (nbt.contains("Age", Tag.TAG_ANY_NUMERIC)) {
            ageable.setAge(nbt.getInt("Age"));
        }
        if (hasBabyFlag(nbt)) {
            ageable.setBaby(readBabyFlag(nbt));
        }
    }

    private static boolean hasBabyFlag(CompoundTag nbt) {
        return nbt.contains("IsBaby", Tag.TAG_BYTE) || nbt.contains("Baby", Tag.TAG_BYTE);
    }

    private static boolean readBabyFlag(CompoundTag nbt) {
        return NbtCompat.getBooleanOr(nbt, "IsBaby", NbtCompat.getBooleanOr(nbt, "Baby", false));
    }

    private static void applyCatVariant(Cat cat, CompoundTag nbt) {
        ResourceLocation id = readResourceLocation(nbt, "CatVariant", "variant", "Variant");
        if (id != null) {
            BuiltInRegistries.CAT_VARIANT.getHolder(id).ifPresent(cat::setVariant);
        }
    }

    private static void applyFrogVariant(Frog frog, CompoundTag nbt) {
        ResourceLocation id = readResourceLocation(nbt, "FrogVariant", "variant", "Variant");
        if (id != null) {
            BuiltInRegistries.FROG_VARIANT.getHolder(id).ifPresent(frog::setVariant);
        }
    }

    private static void applyWolfVariant(Wolf wolf, CompoundTag nbt) {
        ResourceLocation id = readResourceLocation(nbt, "WolfVariant", "variant", "Variant");
        if (id == null) {
            return;
        }
        Registry<WolfVariant> registry = wolf.level().registryAccess().registryOrThrow(Registries.WOLF_VARIANT);
        registry.getHolder(id).ifPresent(wolf::setVariant);
    }

    private static void applyGoatVariant(Goat goat, CompoundTag nbt) {
        if (nbt.contains("HasLeftHorn", Tag.TAG_BYTE)) {
            goat.getEntityData().set(GoatAccessor.identity2$getHasLeftHornData(), nbt.getBoolean("HasLeftHorn"));
        }
        if (nbt.contains("HasRightHorn", Tag.TAG_BYTE)) {
            goat.getEntityData().set(GoatAccessor.identity2$getHasRightHornData(), nbt.getBoolean("HasRightHorn"));
        }
        if (nbt.contains("IsScreamingGoat", Tag.TAG_BYTE)) {
            goat.setScreamingGoat(nbt.getBoolean("IsScreamingGoat"));
        }
    }

    private static void applyVillagerVariant(Villager villager, CompoundTag nbt) {
        VillagerData data = villager.getVillagerData();
        CompoundTag nested = NbtCompat.getCompoundOrNull(nbt, "VillagerData");

        ResourceLocation professionId = readResourceLocation(nbt, "VillagerProfession", "Profession", "profession");
        if (professionId == null && nested != null) {
            professionId = parseResourceLocation(NbtCompat.getStringOr(nested, "profession", ""));
        }
        if (professionId != null && BuiltInRegistries.VILLAGER_PROFESSION.containsKey(professionId)) {
            data = data.setProfession(BuiltInRegistries.VILLAGER_PROFESSION.get(professionId));
        }

        ResourceLocation typeId = readResourceLocation(nbt, "VillagerType", "Type", "type");
        if (typeId == null && nested != null) {
            typeId = parseResourceLocation(NbtCompat.getStringOr(nested, "type", ""));
        }
        if (typeId != null && BuiltInRegistries.VILLAGER_TYPE.containsKey(typeId)) {
            data = data.setType(BuiltInRegistries.VILLAGER_TYPE.get(typeId));
        }

        int level = nbt.contains("VillagerLevel", Tag.TAG_ANY_NUMERIC)
                ? nbt.getInt("VillagerLevel")
                : nested != null && nested.contains("level", Tag.TAG_ANY_NUMERIC) ? nested.getInt("level") : data.getLevel();
        villager.setVillagerData(data.setLevel(Math.max(1, level)));
        villager.setOffers(new MerchantOffers());
    }

    private static int packHorseVariant(net.minecraft.world.entity.animal.horse.Variant coat, Markings markings) {
        return (coat.getId() & 255) | ((markings.getId() << 8) & 65280);
    }

    private static void putHolderKey(CompoundTag nbt, String key, Holder<?> holder) {
        if (holder != null) {
            holder.unwrapKey().ifPresent(resourceKey -> nbt.putString(key, resourceKey.location().toString()));
        }
    }

    private static void putRegistryKey(CompoundTag nbt, String key, @Nullable ResourceLocation id) {
        if (id != null) {
            nbt.putString(key, id.toString());
        }
    }

    @Nullable
    private static ResourceLocation readResourceLocation(CompoundTag nbt, String... keys) {
        for (String key : keys) {
            if (nbt.contains(key, Tag.TAG_STRING)) {
                ResourceLocation id = parseResourceLocation(NbtCompat.getStringOr(nbt, key, ""));
                if (id != null) {
                    return id;
                }
            }
        }
        return null;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
    }
}
