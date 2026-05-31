package net.Gabou.identity2.mixin;

import java.util.List;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void identity2$avoidCatMorphs(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        if (phantom.level().isClientSide()) {
            return;
        }
        List<Player> scaryPlayers = phantom.level().getEntitiesOfClass(
                Player.class,
                phantom.getBoundingBox().inflate(16.0D),
                this::identity2$isCatLikeMorph
        );
        if (scaryPlayers.isEmpty()) {
            return;
        }

        Player nearest = scaryPlayers.get(0);
        phantom.setTarget(null);
        Vec3 away = phantom.position().subtract(nearest.position());
        if (away.lengthSqr() > 1.0E-4D) {
            Vec3 push = away.normalize().scale(0.18D);
            phantom.setDeltaMovement(phantom.getDeltaMovement().add(push.x, 0.04D, push.z));
        }
    }

    private boolean identity2$isCatLikeMorph(Player player) {
        if (player == null || player.isSpectator() || player.isCreative()) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (!(identity instanceof LivingEntity)) {
            return false;
        }
        EntityType<?> type = identity.getType();
        return type == EntityType.CAT || type == EntityType.OCELOT;
    }
}
