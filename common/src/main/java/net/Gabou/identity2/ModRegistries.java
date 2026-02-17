package net.Gabou.identity2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.Gabou.identity2.platform.ModRegistryPlatform;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.item.Item;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModRegistries {
    public static final RegistryKey<Registry<IdentityAbilityDefinition>> IDENTITY_ABILITY_KEY =
        RegistryKey.ofRegistry(Identifier.of("identity2", "identity_ability"));

    public static final Codec<IdentityAbilityDefinition> IDENTITY_ABILITY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Item.ENTRY_CODEC.fieldOf("icon").forGetter(IdentityAbilityDefinition::icon),
        Codec.STRING.optionalFieldOf("command", "").forGetter(IdentityAbilityDefinition::command),
        Codec.INT.fieldOf("cooldown").forGetter(IdentityAbilityDefinition::cooldown),
        Codec.INT.optionalFieldOf("use_duration", 0).forGetter(IdentityAbilityDefinition::useduration),
        Identifier.CODEC.optionalFieldOf("predef", Identifier.of("null")).forGetter(IdentityAbilityDefinition::bultinability),
        Codec.BOOL.optionalFieldOf("override_attack", false).forGetter(IdentityAbilityDefinition::override_attack)
    ).apply(inst, IdentityAbilityDefinition::new));

    public static Registry<IdentityAbilityDefinition> identityAbilityRegistry;

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
        identityAbilityRegistry = (Registry<IdentityAbilityDefinition>) BuiltinRegistries.createWrapperLookup()
            .getOptional(IDENTITY_ABILITY_KEY)
            .orElse(null);
        return identityAbilityRegistry;
    }

    public static Registry<IdentityAbilityDefinition> getIdentityAbilityRegistry() {
        if (identityAbilityRegistry == null) {
            refreshIdentityAbilityRegistry();
        }
        return identityAbilityRegistry;
    }
}
