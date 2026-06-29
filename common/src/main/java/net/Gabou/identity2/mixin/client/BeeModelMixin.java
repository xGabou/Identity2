package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeeModel.class)
public abstract class BeeModelMixin {
    @Shadow @Final private ModelPart bone;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void identity2$applyFullMorphRoll(
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo info
    ) {
        if (!(entity instanceof Bee bee)) {
            return;
        }
        Entity owner = ((EntityAccessor) bee).getIdentityOwner();
        if (owner == null) {
            return;
        }
        int remaining = PredefIdentityAbilities.getSyncedTicksRemaining(
                owner,
                PredefIdentityAbilities.ANIM_ROLL_TICKS_KEY
        );
        if (remaining <= 0) {
            return;
        }
        int duration = 24;
        int elapsed = Mth.clamp(duration - remaining, 0, duration);
        float progress = elapsed / (float) duration;
        this.bone.xRot = progress * ((float) Math.PI * 2.0F);
    }
}
