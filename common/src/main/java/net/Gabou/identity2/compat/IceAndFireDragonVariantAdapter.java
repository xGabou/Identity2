package net.Gabou.identity2.compat;

import java.util.ArrayList;
import java.util.List;
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
 * Optional, class-linkage-free support for Ice and Fire dragon growth variants.
 *
 * <p>Ice and Fire persists dragon age, sex, and colour in the root entity NBT as
 * {@code AgeTicks}, {@code Gender}, and {@code Variant}. Keeping this adapter in
 * common code is safe on loaders where Ice and Fire is absent because it only
 * compares registry identifiers and vanilla NBT.</p>
 */
public final class IceAndFireDragonVariantAdapter implements IdentityVariantAdapter {
    private static final String NAMESPACE = "iceandfire";
    private static final int TICKS_PER_DRAGON_DAY = 24_000;
    // Stage five uses day 125 so its selectable entry is the full-size, full-strength adult.
    private static final int[] STAGE_AGES_DAYS = {0, 25, 50, 75, 125};

    private final ResourceLocation typeId;
    private final List<String> colours;

    private IceAndFireDragonVariantAdapter(ResourceLocation typeId, List<String> colours) {
        this.typeId = typeId;
        this.colours = colours;
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
        List<String> colours = switch (id.getPath()) {
            case "fire_dragon" -> List.of("Red", "Green", "Bronze", "Gray");
            case "ice_dragon" -> List.of("Blue", "White", "Sapphire", "Silver");
            case "lightning_dragon" -> List.of("Electric", "Amethyst", "Copper", "Black");
            default -> null;
        };
        return colours == null ? null : new IceAndFireDragonVariantAdapter(id, colours);
    }

    @Override
    public boolean replacesGenericExtraction() {
        return true;
    }

    @Override
    public CompoundTag extractVariantData(LivingEntity entity) {
        CompoundTag full = EntityNbtIoCompat.saveWithoutId(entity);
        CompoundTag variant = new CompoundTag();
        int ageDays = Math.max(0, full.contains("AgeTicks", Tag.TAG_ANY_NUMERIC) ? full.getInt("AgeTicks") : 0)
                / TICKS_PER_DRAGON_DAY;
        variant.putInt("AgeTicks", canonicalStageAge(ageDays) * TICKS_PER_DRAGON_DAY);
        variant.putBoolean("Gender", full.contains("Gender", Tag.TAG_BYTE) && full.getBoolean("Gender"));
        variant.putInt("Variant", clamp(
                full.contains("Variant", Tag.TAG_ANY_NUMERIC) ? full.getInt("Variant") : 0,
                0,
                colours.size() - 1
        ));
        return variant;
    }

    @Override
    public CompoundTag prepareVariantData(CompoundTag variantNbt) {
        CompoundTag prepared = variantNbt == null ? new CompoundTag() : variantNbt.copy();
        prepared.putInt("AgeTicks", clamp(
                prepared.contains("AgeTicks", Tag.TAG_ANY_NUMERIC) ? prepared.getInt("AgeTicks") : 0,
                0,
                125 * TICKS_PER_DRAGON_DAY
        ));
        prepared.putBoolean("Gender", prepared.contains("Gender", Tag.TAG_BYTE) && prepared.getBoolean("Gender"));
        prepared.putInt("Variant", clamp(
                prepared.contains("Variant", Tag.TAG_ANY_NUMERIC) ? prepared.getInt("Variant") : 0,
                0,
                colours.size() - 1
        ));
        prepared.putBoolean("AgingDisabled", true);
        return prepared;
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
        List<IdentityVariant> variants = new ArrayList<>(colours.size() * STAGE_AGES_DAYS.length * 2);
        String dragonName = title(typeId.getPath());
        for (int colour = 0; colour < colours.size(); colour++) {
            for (int stage = 1; stage <= STAGE_AGES_DAYS.length; stage++) {
                for (boolean male : new boolean[] {false, true}) {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putInt("AgeTicks", STAGE_AGES_DAYS[stage - 1] * TICKS_PER_DRAGON_DAY);
                    nbt.putBoolean("Gender", male);
                    nbt.putInt("Variant", colour);
                    variants.add(new IdentityVariant(
                            typeId,
                            dragonName + " " + colours.get(colour) + " Stage " + stage + " " + (male ? "Male" : "Female"),
                            nbt
                    ));
                }
            }
        }
        return List.copyOf(variants);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int canonicalStageAge(int ageDays) {
        if (ageDays >= 100) {
            return 125;
        }
        if (ageDays >= 75) {
            return 75;
        }
        if (ageDays >= 50) {
            return 50;
        }
        if (ageDays >= 25) {
            return 25;
        }
        return 0;
    }

    private static String title(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean upper = true;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
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
}
