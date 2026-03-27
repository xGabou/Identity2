package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModRegistries {
    public static final ResourceKey<Registry<IdentityAbilityDefinition>> IDENTITY_ABILITY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("identity2", "identity_ability"));

    public static final Codec<IdentityAbilityDefinition> IDENTITY_ABILITY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("icon").forGetter(IdentityAbilityDefinition::icon),
        Codec.STRING.optionalFieldOf("command", "").forGetter(IdentityAbilityDefinition::command),
        Codec.INT.fieldOf("cooldown").forGetter(IdentityAbilityDefinition::cooldown),
        Codec.INT.optionalFieldOf("use_duration", 0).forGetter(IdentityAbilityDefinition::useduration),
        ResourceLocation.CODEC.optionalFieldOf("predef", ResourceLocation.parse("null")).forGetter(IdentityAbilityDefinition::bultinability),
        Codec.BOOL.optionalFieldOf("override_attack", false).forGetter(IdentityAbilityDefinition::override_attack)
    ).apply(inst, IdentityAbilityDefinition::new));

    public static volatile Registry<IdentityAbilityDefinition> identityAbilityRegistry;
    private static final Set<String> loggedMissingRegistryWarnings = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedMissingDefinitionWarnings = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedResolutionDebug = ConcurrentHashMap.newKeySet();
    private static volatile String identityAbilityRegistrySource = "uninitialized";

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

    public static Registry<IdentityAbilityDefinition> refreshIdentityAbilityRegistry() {
        return identityAbilityRegistry;
    }

    public static Registry<IdentityAbilityDefinition> getIdentityAbilityRegistry() {
        return identityAbilityRegistry;
    }

    public static Registry<IdentityAbilityDefinition> getIdentityAbilityRegistry(RegistryAccess registryAccess) {
        Registry<IdentityAbilityDefinition> registry = resolveIdentityAbilityRegistry(registryAccess);
        if (registry != null) {
            setIdentityAbilityRegistry(registry, "runtime_registry_access");
            return registry;
        }
        return identityAbilityRegistry;
    }

    public static void setIdentityAbilityRegistry(Registry<IdentityAbilityDefinition> registry, String source) {
        if (registry == null) {
            return;
        }

        Registry<IdentityAbilityDefinition> previous = identityAbilityRegistry;
        identityAbilityRegistry = registry;
        if (previous != registry) {
            identityAbilityRegistrySource = source == null || source.isBlank() ? "unknown" : source;
            loggedMissingRegistryWarnings.clear();
            loggedMissingDefinitionWarnings.clear();
            loggedResolutionDebug.clear();
            Identity2.LOGGER.debug(
                "Identity ability registry became available from {} with {} entries.",
                identityAbilityRegistrySource,
                registry.keySet().size()
            );
        }
    }

    public static IdentityAbilityDefinition resolveIdentityAbility(EntityType<?> type) {
        return resolveIdentityAbility(type, null);
    }

    public static IdentityAbilityDefinition resolveIdentityAbility(EntityType<?> type, RegistryAccess registryAccess) {
        if (type == null) {
            return null;
        }

        ResourceLocation typeId = EntityType.getKey(type);
        if (typeId == null) {
            return null;
        }

        Registry<IdentityAbilityDefinition> registry = getIdentityAbilityRegistry(registryAccess);
        if (registry == null) {
            logMissingRegistry(typeId);
            return null;
        }

        IdentityAbilityDefinition exact = registry.get(typeId);
        if (exact != null) {
            logResolution(typeId, typeId, exact);
            return exact;
        }

        // Compatibility fallback: many datapacks define abilities by path only
        // under minecraft/identity2 namespace. Try these aliases for modded types.
        ResourceLocation minecraftAliasId = ResourceLocation.fromNamespaceAndPath("minecraft", typeId.getPath());
        IdentityAbilityDefinition minecraftAlias = registry.get(minecraftAliasId);
        if (minecraftAlias != null) {
            logResolution(typeId, minecraftAliasId, minecraftAlias);
            return minecraftAlias;
        }

        ResourceLocation identity2AliasId = ResourceLocation.fromNamespaceAndPath(Identity2.MOD_ID, typeId.getPath());
        IdentityAbilityDefinition identity2Alias = registry.get(identity2AliasId);
        if (identity2Alias != null) {
            logResolution(typeId, identity2AliasId, identity2Alias);
            return identity2Alias;
        }

        logMissingDefinition(typeId);
        return null;
    }

    private static Registry<IdentityAbilityDefinition> resolveIdentityAbilityRegistry(RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return null;
        }
        try {
            return registryAccess.registryOrThrow(IDENTITY_ABILITY_KEY);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void logMissingRegistry(ResourceLocation typeId) {
        String key = String.valueOf(typeId);
        if (loggedMissingRegistryWarnings.add(key)) {
            Identity2.LOGGER.warn(
                "Identity ability registry is unavailable while resolving {}. Configured ability JSON will fall back to defaults until runtime registries are ready.",
                typeId
            );
        }
    }

    private static void logMissingDefinition(ResourceLocation typeId) {
        String key = String.valueOf(typeId);
        if (loggedMissingDefinitionWarnings.add(key)) {
            Identity2.LOGGER.warn(
                "No identity ability definition found for {} in the active identity ability registry from {}. Runtime fallback config will be used.",
                typeId,
                identityAbilityRegistrySource
            );
        }
    }

    private static void logResolution(ResourceLocation requestedId, ResourceLocation resolvedEntryId, IdentityAbilityDefinition definition) {
        String key = requestedId + "->" + resolvedEntryId;
        if (loggedResolutionDebug.add(key)) {
            String iconId = definition.icon() == null
                ? "null"
                : definition.icon().unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse(definition.icon().toString());
            Identity2.LOGGER.debug(
                "Resolved identity ability {} using registry entry {} (predef={}, icon={}, cooldown={}, use_duration={}).",
                requestedId,
                resolvedEntryId,
                definition.bultinability(),
                iconId,
                definition.cooldown(),
                definition.useduration()
            );
        }
    }
}
