package net.Gabou.identity2.util;

import java.util.Map;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.entry.RegistryEntry;

public interface DefaultAttributeContainerAccessor {
    Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> getInstances();
}
