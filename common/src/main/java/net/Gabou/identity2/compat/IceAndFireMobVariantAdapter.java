package net.Gabou.identity2.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.Gabou.identity2.api.variant.IdentityVariantAdapter;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.util.EntityNbtIoCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Linkage-free Ice and Fire defaults for entities whose NBT readers replace healthy constructor
 * values with zero when keys are absent. It also exposes their bounded cosmetic/size variants.
 */
public final class IceAndFireMobVariantAdapter implements IdentityVariantAdapter {
    private static final String NAMESPACE = "iceandfire";
    private static final List<String> SEA_SERPENT_COLOURS = List.of(
            "Blue", "Bronze", "Deep Blue", "Green", "Purple", "Red", "Teal"
    );
    private static final List<String> DEATHWORM_COLOURS = List.of("Yellow", "Red", "White");
    private static final Set<String> MYRMEX_TYPES = Set.of(
            "myrmex_worker",
            "myrmex_soldier",
            "myrmex_sentinel",
            "myrmex_royal",
            "myrmex_queen",
            "myrmex_swarmer"
    );

    private final ResourceLocation typeId;
    private final Kind kind;

    private IceAndFireMobVariantAdapter(ResourceLocation typeId, Kind kind) {
        this.typeId = typeId;
        this.kind = kind;
    }

    @Nullable
    public static IdentityVariantAdapter create(EntityType<?> type) {
        if (type == null) {
            return null;
        }
        ResourceLocation id = EntityType.getKey(type);
        if (id == null || !NAMESPACE.equals(id.getNamespace())) {
            return null;
        }
        Kind kind = switch (id.getPath()) {
            case "sea_serpent" -> Kind.SEA_SERPENT;
            case "deathworm" -> Kind.DEATHWORM;
            case "hydra" -> Kind.HYDRA;
            case "dread_ghoul" -> Kind.DREAD_GHOUL;
            case "dread_beast" -> Kind.DREAD_BEAST;
            case "dread_scuttler" -> Kind.DREAD_SCUTTLER;
            default -> MYRMEX_TYPES.contains(id.getPath()) ? Kind.MYRMEX : null;
        };
        return kind == null ? null : new IceAndFireMobVariantAdapter(id, kind);
    }

    @Override
    public boolean replacesGenericExtraction() {
        return true;
    }

    @Override
    public CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag full = EntityNbtIoCompat.saveWithoutId(entity);
        CompoundTag variant = new CompoundTag();
        switch (kind) {
            case SEA_SERPENT -> {
                boolean ancient = full.contains("Ancient", Tag.TAG_BYTE) && full.getBoolean("Ancient");
                variant.putInt("Variant", numericInt(full, "Variant", 0));
                variant.putBoolean("Ancient", ancient);
                variant.putFloat("Scale", ancient ? 7.5F : 3.5F);
            }
            case DEATHWORM -> {
                float scale = numericFloat(full, "Scale", 0.425F);
                variant.putInt("Variant", numericInt(full, "Variant", 0));
                variant.putFloat("Scale", scale >= 1.0F ? 1.7F : 0.425F);
                variant.putInt("WormAge", 10);
            }
            case HYDRA -> variant.putInt("Variant", numericInt(full, "Variant", 0));
            case DREAD_GHOUL, DREAD_BEAST -> variant.putInt("Variant", numericInt(full, "Variant", 0));
            case DREAD_SCUTTLER -> {
                // Natural scale is continuous; one stable canonical render/combat size is selectable.
            }
            case MYRMEX -> variant.putBoolean(
                    "Variant", full.contains("Variant", Tag.TAG_BYTE) && full.getBoolean("Variant")
            );
        }
        return canonicalize(variant);
    }

    @Override
    public CompoundTag prepareVariantData(CompoundTag variantNbt) {
        return canonicalize(variantNbt == null ? new CompoundTag() : variantNbt.copy());
    }

    @Override
    public void applyVariantData(Entity entity, CompoundTag variantNbt) {
        if (entity == null) {
            return;
        }
        CompoundTag current = EntityNbtIoCompat.saveWithoutId(entity);
        current.merge(prepareVariantData(variantNbt));
        EntityNbtIoCompat.load(entity, current, entity.level().registryAccess());
        entity.refreshDimensions();
    }

    @Override
    public List<IdentityVariant> discoverVariants(EntityType<?> type, Level level) {
        List<IdentityVariant> variants = new ArrayList<>();
        String name = title(typeId.getPath());
        switch (kind) {
            case SEA_SERPENT -> {
                for (int colour = 0; colour < SEA_SERPENT_COLOURS.size(); colour++) {
                    variants.add(variant(name + " " + SEA_SERPENT_COLOURS.get(colour), seaSerpentNbt(colour, false)));
                    variants.add(variant(name + " " + SEA_SERPENT_COLOURS.get(colour) + " Ancient", seaSerpentNbt(colour, true)));
                }
            }
            case DEATHWORM -> {
                for (int colour = 0; colour < DEATHWORM_COLOURS.size(); colour++) {
                    variants.add(variant(name + " " + DEATHWORM_COLOURS.get(colour), deathwormNbt(colour, false)));
                    variants.add(variant(name + " " + DEATHWORM_COLOURS.get(colour) + " Giant", deathwormNbt(colour, true)));
                }
            }
            case HYDRA -> addNumberedVariants(variants, name, 3, "Variant");
            case DREAD_GHOUL -> addNumberedVariants(variants, name, 3, "Variant");
            case DREAD_BEAST -> addNumberedVariants(variants, name, 2, "Variant");
            case DREAD_SCUTTLER -> variants.add(variant(name, new CompoundTag()));
            case MYRMEX -> {
                CompoundTag desert = new CompoundTag();
                desert.putBoolean("Variant", false);
                variants.add(variant(name + " Desert", desert));
                CompoundTag jungle = new CompoundTag();
                jungle.putBoolean("Variant", true);
                variants.add(variant(name + " Jungle", jungle));
            }
        }
        return List.copyOf(variants);
    }

    private CompoundTag canonicalize(CompoundTag data) {
        switch (kind) {
            case SEA_SERPENT -> {
                int colour = clamp(numericInt(data, "Variant", 0), 0, SEA_SERPENT_COLOURS.size() - 1);
                boolean ancient = data.contains("Ancient", Tag.TAG_BYTE) && data.getBoolean("Ancient");
                data.putInt("Variant", colour);
                data.putBoolean("Ancient", ancient);
                data.putFloat("Scale", ancient ? 7.5F : 3.5F);
            }
            case DEATHWORM -> {
                boolean giant = numericFloat(data, "Scale", 0.425F) >= 1.0F;
                data.putInt("Variant", clamp(numericInt(data, "Variant", 0), 0, DEATHWORM_COLOURS.size() - 1));
                data.putFloat("Scale", giant ? 1.7F : 0.425F);
                data.putInt("WormAge", 10);
                data.putInt("GrowthCounter", 0);
                data.putBoolean("WillExplode", false);
            }
            case HYDRA -> {
                data.putInt("Variant", clamp(numericInt(data, "Variant", 0), 0, 2));
                data.putInt("HeadCount", 3);
                data.putInt("SeveredHead", -1);
                for (int head = 0; head < 9; head++) {
                    data.putFloat("HeadDamage" + head, 0.0F);
                }
            }
            case DREAD_GHOUL -> {
                data.putInt("Variant", clamp(numericInt(data, "Variant", 0), 0, 2));
                data.putInt("ScreamStage", 0);
                data.putFloat("DreadScale", 1.0F);
            }
            case DREAD_BEAST -> {
                data.putInt("Variant", clamp(numericInt(data, "Variant", 0), 0, 1));
                data.putFloat("DreadScale", 1.0F);
            }
            case DREAD_SCUTTLER -> data.putFloat("Scale", 1.0F);
            case MYRMEX -> {
                boolean jungle = data.contains("Variant", Tag.TAG_BYTE) && data.getBoolean("Variant");
                data.putBoolean("Variant", jungle);
                data.putInt("GrowthStage", 2);
                data.putInt("GrowthTicks", 0);
            }
        }
        return data;
    }

    private IdentityVariant variant(String label, CompoundTag nbt) {
        return new IdentityVariant(typeId, label, canonicalize(nbt));
    }

    private static CompoundTag seaSerpentNbt(int colour, boolean ancient) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Variant", colour);
        nbt.putBoolean("Ancient", ancient);
        nbt.putFloat("Scale", ancient ? 7.5F : 3.5F);
        return nbt;
    }

    private static CompoundTag deathwormNbt(int colour, boolean giant) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Variant", colour);
        nbt.putFloat("Scale", giant ? 1.7F : 0.425F);
        nbt.putInt("WormAge", 10);
        return nbt;
    }

    private void addNumberedVariants(List<IdentityVariant> variants, String name, int count, String key) {
        for (int index = 0; index < count; index++) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt(key, index);
            variants.add(variant(name + " Variant " + (index + 1), nbt));
        }
    }

    private static int numericInt(CompoundTag data, String key, int fallback) {
        return data.contains(key, Tag.TAG_ANY_NUMERIC) ? data.getInt(key) : fallback;
    }

    private static float numericFloat(CompoundTag data, String key, float fallback) {
        return data.contains(key, Tag.TAG_ANY_NUMERIC) ? data.getFloat(key) : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String title(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean upper = true;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '_') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(ch) : ch);
                upper = false;
            }
        }
        return out.toString();
    }

    private enum Kind {
        SEA_SERPENT,
        DEATHWORM,
        HYDRA,
        DREAD_GHOUL,
        DREAD_BEAST,
        DREAD_SCUTTLER,
        MYRMEX
    }
}
