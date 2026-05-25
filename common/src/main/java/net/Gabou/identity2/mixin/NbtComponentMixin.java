package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CompoundTag.class)
public class NbtComponentMixin implements NbtComponentAccessor {
    @Override
    public CompoundTag getNbt() {
        return (CompoundTag) (Object) this;
    }
}
