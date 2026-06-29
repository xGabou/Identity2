package net.Gabou.identity2.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Goat.class)
public interface GoatAccessor {
    @Accessor("DATA_HAS_LEFT_HORN")
    static EntityDataAccessor<Boolean> identity2$getHasLeftHornData() {
        throw new AssertionError();
    }

    @Accessor("DATA_HAS_RIGHT_HORN")
    static EntityDataAccessor<Boolean> identity2$getHasRightHornData() {
        throw new AssertionError();
    }
}
