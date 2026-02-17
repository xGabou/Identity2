package net.Gabou.identity2.util;

import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public record IdentityAbilityDefinition(
    RegistryEntry<Item> icon,
    String command,
    int cooldown,
    int useduration,
    Identifier bultinability,
    boolean override_attack
) {
}
