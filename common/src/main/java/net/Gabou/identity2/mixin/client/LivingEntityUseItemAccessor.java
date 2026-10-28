package net.Gabou.identity2.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityUseItemAccessor {
    @Accessor("useItem")
    void identity2$setUseItem(ItemStack item);

    @Accessor("useItemRemaining")
    void identity2$setUseItemRemaining(int ticks);

    @Invoker("setLivingEntityFlag")
    void identity2$setLivingEntityFlag(int flag, boolean value);
}
