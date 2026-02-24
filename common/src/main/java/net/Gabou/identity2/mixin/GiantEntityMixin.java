package net.Gabou.identity2.mixin;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.util.MobGoalSelectorAccessor;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Giant.class)
public abstract class GiantEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void identity2$initZombieAi(EntityType<? extends Giant> entityType, Level level, CallbackInfo info) {
        if (!IdentitySettings.enableGiantZombieAiAndHardSpawns) {
            return;
        }
        if (level.isClientSide()) {
            return;
        }

        Giant giant = (Giant) (Object) this;
        MobGoalSelectorAccessor accessor = (MobGoalSelectorAccessor) giant;
        accessor.identity2$getGoalSelector().addGoal(0, new FloatGoal(giant));
        accessor.identity2$getGoalSelector().addGoal(2, new MeleeAttackGoal(giant, 1.0D, false));
        accessor.identity2$getGoalSelector().addGoal(5, new WaterAvoidingRandomStrollGoal(giant, 1.0D));
        accessor.identity2$getGoalSelector().addGoal(7, new LookAtPlayerGoal(giant, Player.class, 8.0F));
        accessor.identity2$getGoalSelector().addGoal(8, new RandomLookAroundGoal(giant));

        accessor.identity2$getTargetSelector().addGoal(1, new HurtByTargetGoal(giant));
        accessor.identity2$getTargetSelector().addGoal(2, new NearestAttackableTargetGoal<>(giant, Player.class, true));
        accessor.identity2$getTargetSelector().addGoal(3, new NearestAttackableTargetGoal<>(giant, Villager.class, false));
        accessor.identity2$getTargetSelector().addGoal(3, new NearestAttackableTargetGoal<>(giant, IronGolem.class, true));
    }
}
