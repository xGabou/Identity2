package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModRegistries {
    public static final ResourceKey<Registry<IdentityAbilityDefinition>> IDENTITY_ABILITY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("identity2", "identity_ability"));

    public static final Codec<IdentityAbilityDefinition> IDENTITY_ABILITY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Item.CODEC.fieldOf("icon").forGetter(IdentityAbilityDefinition::icon),
        Codec.STRING.optionalFieldOf("command", "").forGetter(IdentityAbilityDefinition::command),
        Codec.INT.fieldOf("cooldown").forGetter(IdentityAbilityDefinition::cooldown),
        Codec.INT.optionalFieldOf("use_duration", 0).forGetter(IdentityAbilityDefinition::useduration),
        ResourceLocation.CODEC.optionalFieldOf("predef", ResourceLocation.parse("null")).forGetter(IdentityAbilityDefinition::bultinability),
        Codec.BOOL.optionalFieldOf("override_attack", false).forGetter(IdentityAbilityDefinition::override_attack)
    ).apply(inst, IdentityAbilityDefinition::new));

    public static Registry<IdentityAbilityDefinition> identityAbilityRegistry;
    private static final long IDENTITY_ABILITY_LOOKUP_RETRY_DELAY_MS = 5000L;
    private static long nextIdentityAbilityLookupAtMs = 0L;

    private static boolean initialized = false;
    private static ModRegistryPlatform platform = ModRegistryPlatform.NOOP;

    private ModRegistries() {
    }

    public static void setPlatform(ModRegistryPlatform registryPlatform) {
        platform = registryPlatform == null ? ModRegistryPlatform.NOOP : registryPlatform;
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        platform.registerIdentityAbilityRegistry();
        refreshIdentityAbilityRegistry();
    }

    @SuppressWarnings("unchecked")
    public static Registry<IdentityAbilityDefinition> refreshIdentityAbilityRegistry() {
        identityAbilityRegistry = (Registry<IdentityAbilityDefinition>) VanillaRegistries.createLookup()
            .lookup(IDENTITY_ABILITY_KEY)
            .orElse(null);
        if (identityAbilityRegistry == null) {
            nextIdentityAbilityLookupAtMs = System.currentTimeMillis() + IDENTITY_ABILITY_LOOKUP_RETRY_DELAY_MS;
        } else {
            nextIdentityAbilityLookupAtMs = 0L;
        }
        return identityAbilityRegistry;
    }

    public static Registry<IdentityAbilityDefinition> getIdentityAbilityRegistry() {
        if (identityAbilityRegistry != null) {
            return identityAbilityRegistry;
        }
        if (!initialized) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now < nextIdentityAbilityLookupAtMs) {
            return null;
        }
        return refreshIdentityAbilityRegistry();
    }

    public static IdentityAbilityDefinition resolveIdentityAbility(EntityType<?> type) {
        Registry<IdentityAbilityDefinition> registry = getIdentityAbilityRegistry();
        if (registry == null || type == null) {
            return null;
        }

        ResourceLocation typeId = EntityType.getKey(type);
        if (typeId == null) {
            return null;
        }

        IdentityAbilityDefinition exact = registry.getValue(typeId);
        if (exact != null) {
            return exact;
        }

        // Compatibility fallback: many datapacks define abilities by path only
        // under minecraft/identity2 namespace. Try these aliases for modded types.
        IdentityAbilityDefinition minecraftAlias = registry.getValue(
            ResourceLocation.fromNamespaceAndPath("minecraft", typeId.getPath())
        );
        if (minecraftAlias != null) {
            return minecraftAlias;
        }

        return registry.getValue(ResourceLocation.fromNamespaceAndPath(Identity2.MOD_ID, typeId.getPath()));
    }
}
