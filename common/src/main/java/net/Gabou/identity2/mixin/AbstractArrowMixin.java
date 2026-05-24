package net.Gabou.identity2.mixin;

import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void identity2$applyBoggedArrowPoison(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level().isClientSide()) {
            return;
        }
        if (!(arrow instanceof Arrow tippedArrow)) {
            return;
        }

        Entity owner = arrow.getOwner();
        if (!(owner instanceof Player player)) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        CustomData customData = ((EntityAccessor) arrow).getCustomData();
        if (identity.getType() == EntityType.BOGGED) {
            if (NbtCompat.getBooleanOr(((NbtComponentAccessor) (Object) customData).getNbt(), "identity2.bogged_poison_arrow", false)) {
                return;
            }

            tippedArrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            ((NbtComponentAccessor) (Object) customData).getNbt().putBoolean("identity2.bogged_poison_arrow", true);
            return;
        }

        if (identity.getType() == EntityType.STRAY) {
            if (NbtCompat.getBooleanOr(((NbtComponentAccessor) (Object) customData).getNbt(), "identity2.stray_slowness_arrow", false)) {
                return;
            }

            tippedArrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            ((NbtComponentAccessor) (Object) customData).getNbt().putBoolean("identity2.stray_slowness_arrow", true);
        }
    }
}
