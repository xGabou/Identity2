package net.Gabou.identity2.api;

import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.identity.IdentityVariantRegistry;
import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.Gabou.identity2.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdentityVariantDiscoveryTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void validCommandBabiesUseStableNbtInsteadOfReflectedSetters() {
        List<IdentityVariant> chickenVariants = IdentityApi.discoverCommandVariants(EntityType.CHICKEN);

        assertTrue(chickenVariants.stream().anyMatch(IdentityVariantDiscoveryTest::isBaby));
        assertTrue(chickenVariants.stream().anyMatch(variant -> variant.variantNbt().contains("Age")));
    }

    @Test
    void invalidBabyFormsStayExcluded() {
        assertTrue(IdentityApi.isBabyVariantBlocked(EntityType.FROG));
        assertTrue(IdentityApi.isBabyVariantBlocked(EntityType.WANDERING_TRADER));
        assertFalse(IdentityApi.discoverCommandVariants(EntityType.FROG).stream().anyMatch(IdentityVariantDiscoveryTest::isBaby));
        assertFalse(IdentityApi.discoverCommandVariants(EntityType.WANDERING_TRADER).stream().anyMatch(IdentityVariantDiscoveryTest::isBaby));
    }

    @Test
    void zombieBabyRemainsAvailableToCommands() {
        assertTrue(IdentityApi.discoverCommandVariants(EntityType.ZOMBIE).stream().anyMatch(IdentityVariantDiscoveryTest::isBaby));
    }

    @Test
    void tropicalFishCommandsExposeUniquePackedVariants() {
        List<IdentityVariant> variants = IdentityApi.discoverCommandVariants(EntityType.TROPICAL_FISH);
        Set<Integer> packedVariants = new HashSet<>();

        for (IdentityVariant variant : variants) {
            assertTrue(variant.variantNbt().contains("Variant"));
            packedVariants.add(NbtCompat.getIntOr(variant.variantNbt(), "Variant", -1));
        }

        assertFalse(variants.isEmpty());
        assertEquals(variants.size(), packedVariants.size());
    }

    @Test
    void armadilloHasSpecificPackagedAbility() {
        assertTrue(PredefIdentityAbilities.predef.containsKey(ResourceLocation.parse("minecraft:armadillo")));
        assertTrue(getClass().getClassLoader().getResource(
                "data/minecraft/identity2/identity_ability/armadillo.json"
        ) != null);
    }

    @Test
    void variantReferencesAreStableAcrossCompoundKeyOrder() {
        CompoundTag first = new CompoundTag();
        first.putInt("Variant", 7);
        first.putString("Owner", "test");
        CompoundTag second = new CompoundTag();
        second.putString("Owner", "test");
        second.putInt("Variant", 7);

        ResourceLocation sheep = ResourceLocation.parse("minecraft:sheep");
        assertEquals(
            IdentityVariantRegistry.stableId(sheep, first),
            IdentityVariantRegistry.stableId(sheep, second)
        );
        assertNotEquals(
            IdentityVariantRegistry.stableId(sheep, first),
            IdentityVariantRegistry.stableId(ResourceLocation.parse("minecraft:cat"), first)
        );
    }

    @Test
    void babyReferencesUseNormalizedVariantData() {
        CompoundTag age = new CompoundTag();
        age.putInt("Age", -24000);
        CompoundTag flag = new CompoundTag();
        flag.putBoolean("IsBaby", true);

        ResourceLocation zombie = ResourceLocation.parse("minecraft:zombie");
        assertEquals(
            IdentityVariantRegistry.stableId(zombie, age),
            IdentityVariantRegistry.stableId(zombie, flag)
        );
        CompoundTag adult = new CompoundTag();
        adult.putBoolean("IsBaby", false);
        assertEquals("", IdentityVariantRegistry.stableId(ResourceLocation.parse("minecraft:cow"), adult));
        assertTrue(IdentityVariantRegistry.DEFINITION_CHUNK_BYTES < 32767);
    }

    @Test
    void transientLowercaseEntityDataIsNotStoredAsAVariant() {
        CompoundTag baseline = new CompoundTag();
        CompoundTag current = new CompoundTag();
        net.minecraft.nbt.ListTag attributes = new net.minecraft.nbt.ListTag();
        CompoundTag attribute = new CompoundTag();
        attribute.putString("id", "minecraft:generic.movement_speed");
        attribute.putDouble("base", 0.2D);
        attributes.add(attribute);
        current.put("attributes", attributes);
        current.putFloat("health", 7.0F);

        assertTrue(IdentityVariantNbtHelper.computeVariantDiff(baseline, current).isEmpty());
        assertTrue(net.Gabou.identity2.identity.IdentityProgression.normalizeVariantForUnlock(current).isEmpty());
    }

    private static boolean isBaby(IdentityVariant variant) {
        return NbtCompat.getBooleanOr(variant.variantNbt(), "IsBaby", false);
    }
}
