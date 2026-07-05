package net.Gabou.identity2.mixin;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieEntityMixin {
    @Unique
    private boolean identity2$eligibleGiantSpawnOrigin;

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void identity2$captureGiantSpawnOrigin(
            ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        this.identity2$eligibleGiantSpawnOrigin = spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.REINFORCEMENT;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void identity2$maybeReplaceWithGiant(CallbackInfo info) {
        if (!IdentitySettings.enableGiantZombieAiAndHardSpawns) {
            return;
        }
        if (!this.identity2$eligibleGiantSpawnOrigin) {
            return;
        }

        Zombie zombie = (Zombie) (Object) this;
        if (((EntityAccessor) zombie).getIdentityOwner() != null) {
            return;
        }
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
        double chance = Math.max(0.0D, Math.min(1.0D, IdentitySettings.giantZombieSpawnReplacementChance));
        if (chance <= 0.0D || zombie.getRandom().nextDouble() > chance) {
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


