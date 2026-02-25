package net.Gabou.identity2.mixin;

import net.Gabou.identity2.IdentitySettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieEntityMixin {
    @Unique
    private static final float IDENTITY2_GIANT_REPLACE_CHANCE_HARD = 0.005F;

    @Inject(method = "tick", at = @At("HEAD"))
    private void identity2$maybeReplaceWithGiant(CallbackInfo info) {
        if (!IdentitySettings.enableGiantZombieAiAndHardSpawns) {
            return;
        }

        Zombie zombie = (Zombie) (Object) this;
        if (zombie.level().isClientSide()) {
            return;
        }
        if (!(zombie.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (zombie.tickCount != 1) {
            return;
        }
        if (!zombie.isAlive() || zombie.isRemoved() || zombie.isBaby()) {
            return;
        }
        if (serverLevel.getDifficulty() != Difficulty.HARD) {
            return;
        }
        if (zombie.getRandom().nextFloat() > IDENTITY2_GIANT_REPLACE_CHANCE_HARD) {
            return;
        }

        Giant giant = EntityType.GIANT.create(serverLevel);
        if (giant == null) {
            return;
        }

        giant.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
        giant.setDeltaMovement(zombie.getDeltaMovement());
        giant.setNoAi(false);

        if (!serverLevel.addFreshEntity(giant)) {
            return;
        }
        zombie.discard();
    }
}


