package net.Gabou.identity2.util;

public interface EnderDragonEntityAccessor {
    int setTicksUntilNextGrowl(int ticks);

    int getTicksUntilNextGrowl();

    void runTickWithEndCrystals();
}
