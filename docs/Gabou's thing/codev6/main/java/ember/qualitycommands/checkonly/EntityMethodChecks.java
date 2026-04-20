package ember.qualitycommands.checkonly;

import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public abstract class EntityMethodChecks extends net.minecraft.entity.Entity {
    public EntityMethodChecks(net.minecraft.entity.EntityType<?> type, World world) {
        super(type,world);
    }
    @Override
    public void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition){
        return;
    }
}
