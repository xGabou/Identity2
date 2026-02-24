package net.Gabou.identity2.checkonly;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public abstract class EntityMethodChecks extends net.minecraft.world.entity.Entity {
    public EntityMethodChecks(net.minecraft.world.entity.EntityType<?> type, Level world) {
        super(type,world);
    }
    @Override
    public void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition){
        return;
    }
}
