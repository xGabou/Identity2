package net.Gabou.identity2.mixin;

import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessor {
    @Invoker("updateTrades")
    void identity2$invokeUpdateTrades();
}
