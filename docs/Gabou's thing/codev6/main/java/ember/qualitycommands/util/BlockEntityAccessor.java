package ember.qualitycommands.util;

import net.minecraft.registry.MutableRegistry;
import net.minecraft.util.math.BlockPos;

public abstract interface BlockEntityAccessor {
    public abstract void setPos(BlockPos newPos);
    //public static MutableRegistry<MutableRegistry<?>> getRoot(){return null};
}
