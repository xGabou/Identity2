package draylar.identity.forge.ability;


import com.starfish_studios.naturalist.core.registry.NaturalistEntityTypes;
import draylar.identity.ability.AbilityRegistry;

import draylar.identity.forge.ability.impl.BearAbility;
import net.Gabou.gaboulibs.util.CompatUtils;

public class NaturalistAbilityRegistry {

    private NaturalistAbilityRegistry() {}

    public static void init() {
        if (!CompatUtils.isNaturalistLoaded()) {
            return;
        }
        AbilityRegistry.register(NaturalistEntityTypes.BEAR.get(), new BearAbility());
    }
}
