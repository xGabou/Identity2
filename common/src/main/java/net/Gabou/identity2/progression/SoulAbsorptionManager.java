package net.Gabou.identity2.progression;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;

public final class SoulAbsorptionManager {
    private static final String ABSORBED_MORPHS_KEY = "identity2.progression.absorbed_morphs";
    private static final Codec<List<String>> STRING_LIST_CODEC = Codec.STRING.listOf();

    private SoulAbsorptionManager() {
    }

    public static boolean isAbsorbed(ServerPlayer player, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null || !ProgressionConfig.enableSoulAbsorption()) {
            return false;
        }
        Set<String> absorbed = getAbsorbedSet(player);
        String token = IdentityProgression.toVariantUnlockToken(variantNbt);
        return absorbed.contains(absorptionKey(identityId, token)) || absorbed.contains(absorptionKey(identityId, "-"));
    }

    public static AbsorptionResult absorbMorph(ServerPlayer player, String jarId, ResourceLocation identityId, CompoundTag variantNbt) {
        if (player == null || identityId == null) {
            return AbsorptionResult.failure("Invalid absorption request.");
        }
        if (!ProgressionConfig.enableSoulAbsorption()) {
            return AbsorptionResult.failure("Soul absorption system is disabled.");
        }
        if (!ProgressionConfig.enableSoulJars()) {
            return AbsorptionResult.failure("Soul jar system is disabled.");
        }

        if (!SoulJarManager.isStoredInAnyJar(player, identityId, variantNbt)) {
            return AbsorptionResult.failure("Morph is not stored in a soul jar.");
        }
        if (!SoulJarManager.isPermanentInJar(player, identityId, variantNbt)) {
            return AbsorptionResult.failure("Morph must be permanent in a soul jar before absorption.");
        }

        if (!SoulJarManager.removeMorphFromJar(player, jarId, identityId, variantNbt)) {
            return AbsorptionResult.failure("Could not remove morph from jar for absorption.");
        }

        Set<String> absorbed = getAbsorbedSet(player);
        String key = absorptionKey(identityId, IdentityProgression.toVariantUnlockToken(variantNbt));
        if (!absorbed.add(key)) {
            return AbsorptionResult.failure("Morph is already absorbed.");
        }
        setAbsorbedSet(player, absorbed);
        return AbsorptionResult.success("Absorbed morph permanently: " + identityId);
    }

    public static Set<String> getAbsorbedIdentityIds(ServerPlayer player) {
        Set<String> ids = new HashSet<>();
        for (String key : getAbsorbedSet(player)) {
            int separator = key.indexOf('|');
            if (separator <= 0) {
                continue;
            }
            ids.add(key.substring(0, separator));
        }
        return ids;
    }

    private static Set<String> getAbsorbedSet(ServerPlayer player) {
        CompoundTag customData = getCustomData(player);
        return new HashSet<>(net.Gabou.identity2.util.NbtCompat.read(customData, ABSORBED_MORPHS_KEY, STRING_LIST_CODEC).orElse(List.of()));
    }

    private static void setAbsorbedSet(ServerPlayer player, Set<String> absorbed) {
        CompoundTag customData = getCustomData(player);
        net.Gabou.identity2.util.NbtCompat.store(customData, ABSORBED_MORPHS_KEY, STRING_LIST_CODEC, new ArrayList<>(absorbed));
    }

    private static String absorptionKey(ResourceLocation identityId, String variantToken) {
        return identityId + "|" + (variantToken == null || variantToken.isBlank() ? "-" : variantToken);
    }

    private static CompoundTag getCustomData(ServerPlayer player) {
        CustomData customData = ((EntityAccessor) player).getCustomData();
        return ((NbtComponentAccessor) (Object) customData).getNbt();
    }

    public record AbsorptionResult(boolean success, String message) {
        public static AbsorptionResult success(String message) {
            return new AbsorptionResult(true, message);
        }

        public static AbsorptionResult failure(String message) {
            return new AbsorptionResult(false, message);
        }
    }
}

