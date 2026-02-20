package net.Gabou.identity2.util;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public interface DefaultAttributeContainerAccessor {
    Map<Holder<Attribute>, AttributeInstance> getInstances();
}
