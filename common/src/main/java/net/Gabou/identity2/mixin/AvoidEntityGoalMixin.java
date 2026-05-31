package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    private Class<T> avoidClass;

    @Shadow
    @Final
    protected float maxDist;

    @Shadow
    protected T toAvoid;

    @Shadow
    private Path path;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void identity2$avoidMorphedPlayers(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        Player player = mob.level().getNearestPlayer(
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                maxDist,
                this::identity2$isAvoidableMorph
        );
        if (player == null) {
            return;
        }
        Path candidatePath = mob.getNavigation().createPath(player, 0);
        if (candidatePath == null) {
            return;
        }
        this.toAvoid = (T) player;
        this.path = candidatePath;
        cir.setReturnValue(true);
    }

    private boolean identity2$isAvoidableMorph(Entity candidate) {
        if (!(candidate instanceof ServerPlayer player) || player.isSpectator()) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (!(identity instanceof LivingEntity)) {
            return false;
        }
        EntityType<?> type = identity.getType();
        if (mob instanceof Creeper && (avoidClass == Cat.class || avoidClass == Ocelot.class)) {
            return type == EntityType.CAT || type == EntityType.OCELOT;
        }
        if (mob instanceof Skeleton && avoidClass == Wolf.class) {
            return type == EntityType.WOLF;
        }
        return avoidClass.isInstance(identity);
    }
}
