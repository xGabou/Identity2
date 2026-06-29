package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;

import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.DefaultAttributeContainerAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
@Mixin(AttributeSupplier.class)
public class DefaultAttributeContainerMixin implements DefaultAttributeContainerAccessor{
    
    @Shadow
    @Mutable
    public Map<Holder<Attribute>, AttributeInstance> instances;
    public Map<Holder<Attribute>, AttributeInstance> getInstances(){
        return instances;
    }
}

