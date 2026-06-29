package net.Gabou.identity2.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntitySoundInvoker {
    @Invoker("playStepSound")
    void identity2$invokePlayStepSound(BlockPos pos, BlockState state);
}
