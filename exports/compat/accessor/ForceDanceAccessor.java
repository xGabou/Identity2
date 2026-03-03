package draylar.identity.forge.compat.accessor;

import net.minecraft.entity.data.TrackedData;

public interface ForceDanceAccessor {
    void identity$startForceDance();
    TrackedData<Integer> identity$getDanceTicksTracker();
    void identity$forceDance(int ticks);



}