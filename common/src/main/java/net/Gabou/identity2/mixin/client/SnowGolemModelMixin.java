package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowGolemModel.class)
public abstract class SnowGolemModelMixin {
    @Shadow @Final private ModelPart upperBody;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void identity2$normalizeMorphTorso(
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo info
    ) {
        if (!(entity instanceof EntityAccessor accessor) || accessor.getIdentityOwner() == null) {
            return;
        }
        this.upperBody.yRot = 0.0F;
    }
}
