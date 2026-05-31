package net.Gabou.identity2.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderMan.class)
public interface EnderManAccessor {

    @Accessor("DATA_CREEPY")
    static EntityDataAccessor<Boolean> identity2$getDataCreepy() {
        throw new AssertionError();
    }

    @Accessor("DATA_STARED_AT")
    static EntityDataAccessor<Boolean> identity2$getDataStaredAt() {
        throw new AssertionError();
    }
}
