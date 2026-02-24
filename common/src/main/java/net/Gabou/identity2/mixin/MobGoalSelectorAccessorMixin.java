package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.MobGoalSelectorAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Mob.class)
public abstract class MobGoalSelectorAccessorMixin implements MobGoalSelectorAccessor {
    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Override
    public GoalSelector identity2$getGoalSelector() {
        return this.goalSelector;
    }

    @Override
    public GoalSelector identity2$getTargetSelector() {
        return this.targetSelector;
    }
}

