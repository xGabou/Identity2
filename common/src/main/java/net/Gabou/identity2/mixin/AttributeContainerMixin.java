package net.Gabou.identity2.mixin;
import com.google.common.collect.Lists;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;

import net.Gabou.identity2.util.AttributeContainerAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
@Mixin(AttributeMap.class)
public class AttributeContainerMixin implements AttributeContainerAccessor{
    
    @Shadow
    @Mutable
    public AttributeSupplier supplier;
    public AttributeSupplier getDefaultAttributes(){
        return this.supplier;
   }

    @Override
    public void setDefaultAttributes(AttributeSupplier supplier) {
        this.supplier = supplier;
    }
}

