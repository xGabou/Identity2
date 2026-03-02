package net.Gabou.identity2.util;

import net.minecraft.resources.ResourceLocation;

public record IdentityAbilityDefinition(
    ResourceLocation icon,
    String command,
    int cooldown,
    int useduration,
    ResourceLocation bultinability,
    boolean override_attack
) {
}
