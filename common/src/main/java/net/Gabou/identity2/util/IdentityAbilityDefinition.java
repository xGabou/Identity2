package net.Gabou.identity2.util;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record IdentityAbilityDefinition(
    ResourceLocation icon,
    String command,
    int cooldown,
    int useduration,
    ResourceLocation bultinability,
    boolean override_attack,
    String action,
    double strength,
    double range,
    int duration,
    List<String> traits
) {
    public IdentityAbilityDefinition {
        command = command == null ? "" : command;
        action = action == null ? "" : action;
        traits = traits == null ? List.of() : List.copyOf(traits);
    }

    public boolean hasTrait(String trait) {
        if (trait == null || trait.isBlank()) {
            return false;
        }
        for (String entry : traits) {
            if (trait.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }
}
