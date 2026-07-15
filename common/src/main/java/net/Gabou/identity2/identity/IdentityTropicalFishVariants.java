package net.Gabou.identity2.identity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.TropicalFish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IdentityTropicalFishVariants {
    private IdentityTropicalFishVariants() {
    }

    public static List<IdentityVariant> discover(ResourceLocation typeId) {
        List<IdentityVariant> variants = new ArrayList<>(TropicalFish.COMMON_VARIANTS.size());
        for (TropicalFish.Variant fishVariant : TropicalFish.COMMON_VARIANTS) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Variant", fishVariant.getPackedId());
            String label = "Tropical Fish "
                    + capitalize(fishVariant.pattern().name())
                    + " " + capitalize(fishVariant.baseColor().getName())
                    + "/" + capitalize(fishVariant.patternColor().getName());
            variants.add(new IdentityVariant(typeId, label, nbt));
        }
        return variants;
    }

    private static String capitalize(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            result.append(capitalizeNext ? Character.toUpperCase(character) : character);
            capitalizeNext = character == ' ';
        }
        return result.toString();
    }
}
