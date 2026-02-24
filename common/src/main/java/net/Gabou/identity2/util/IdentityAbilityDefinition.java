package net.Gabou.identity2.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public record IdentityAbilityDefinition(
    Holder<Item> icon,
    String command,
    int cooldown,
    int useduration,
    ResourceLocation bultinability,
    boolean override_attack
) {
}
