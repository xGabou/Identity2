package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public final class ModRegistries {
    public static final ResourceKey<Registry<IdentityAbilityDefinition>> IDENTITY_ABILITY_KEY =
        ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("identity2", "identity_ability"));

    public static final Codec<IdentityAbilityDefinition> IDENTITY_ABILITY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Item.CODEC.fieldOf("icon").forGetter(IdentityAbilityDefinition::icon),
        Codec.STRING.optionalFieldOf("command", "").forGetter(IdentityAbilityDefinition::command),
        Codec.INT.fieldOf("cooldown").forGetter(IdentityAbilityDefinition::cooldown),
        Codec.INT.optionalFieldOf("use_duration", 0).forGetter(IdentityAbilityDefinition::useduration),
        Identifier.CODEC.optionalFieldOf("predef", Identifier.parse("null")).forGetter(IdentityAbilityDefinition::bultinability),
        Codec.BOOL.optionalFieldOf("override_attack", false).forGetter(IdentityAbilityDefinition::override_attack)
    ).apply(inst, IdentityAbilityDefinition::new));

    public static Registry<IdentityAbilityDefinition> identityAbilityRegistry;
    private static volatile Map<Identifier, IdentityAbilityDefinition> identityAbilityLookupCache = Map.of();

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
    public static synchronized Registry<IdentityAbilityDefinition> refreshIdentityAbilityRegistry() {
        long start = System.nanoTime();
        Registry<IdentityAbilityDefinition> registry = (Registry<IdentityAbilityDefinition>) VanillaRegistries.createLookup()
            .lookup(IDENTITY_ABILITY_KEY)
            .orElse(null);
        setIdentityAbilityRegistry(registry);
        long elapsedNanos = System.nanoTime() - start;
        if (Identity2.LOGGER.isDebugEnabled()) {
            Identity2.LOGGER.debug(
                "Refreshed identity ability registry: present={}, entries={}, took {} us",
                identityAbilityRegistry != null,
                identityAbilityLookupCache.size(),
                elapsedNanos / 1_000L
            );
        }
        return identityAbilityRegistry;
    }

    public static synchronized void setIdentityAbilityRegistry(Registry<IdentityAbilityDefinition> registry) {
        identityAbilityRegistry = registry;
        identityAbilityLookupCache = buildIdentityAbilityLookupCache(registry);
    }

    public static Registry<IdentityAbilityDefinition> getIdentityAbilityRegistry() {
        return identityAbilityRegistry;
    }

    public static IdentityAbilityDefinition resolveIdentityAbility(EntityType<?> type) {
        if (type == null) {
            return null;
        }

        Identifier typeId = EntityType.getKey(type);
        if (typeId == null) {
            return null;
        }

        Map<Identifier, IdentityAbilityDefinition> lookup = identityAbilityLookupCache;
        if (lookup.isEmpty() && identityAbilityRegistry != null) {
            synchronized (ModRegistries.class) {
                if (identityAbilityLookupCache.isEmpty() && identityAbilityRegistry != null) {
                    identityAbilityLookupCache = buildIdentityAbilityLookupCache(identityAbilityRegistry);
                }
                lookup = identityAbilityLookupCache;
            }
        }

        IdentityAbilityDefinition exact = lookup.get(typeId);
        if (exact != null) {
            return exact;
        }

        // Compatibility fallback: many datapacks define abilities by path only
        // under minecraft/identity2 namespace. Try these aliases for modded types.
        IdentityAbilityDefinition minecraftAlias = lookup.get(
            Identifier.fromNamespaceAndPath("minecraft", typeId.getPath())
        );
        if (minecraftAlias != null) {
            return minecraftAlias;
        }

        return lookup.get(Identifier.fromNamespaceAndPath(Identity2.MOD_ID, typeId.getPath()));
    }

    private static Map<Identifier, IdentityAbilityDefinition> buildIdentityAbilityLookupCache(Registry<IdentityAbilityDefinition> registry) {
        if (registry == null) {
            return Map.of();
        }

        Map<Identifier, IdentityAbilityDefinition> lookup = new HashMap<>();
        for (Identifier id : registry.keySet()) {
            IdentityAbilityDefinition definition = registry.getValue(id);
            if (definition != null) {
                lookup.put(id, definition);
            }
        }
        if (lookup.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(lookup);
    }
}
