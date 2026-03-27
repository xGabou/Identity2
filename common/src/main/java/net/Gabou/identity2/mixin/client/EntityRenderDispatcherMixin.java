package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.Gabou.identity2.client.IdentityRenderStateHelper;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            ),
            require = 0
    )
    private <E extends Entity> void identity2$renderWithMorphEntity(
            EntityRenderer<? super E> originalRenderer,
            E entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light
    ) {
        Entity renderIdentity = MorphTransitionHelper.resolveRenderIdentity(
                entity,
                ((EntityAccessor) entity).getCurrentIdentity(),
                tickDelta
        );

        if (renderIdentity == null || renderIdentity == entity) {
            identity2$renderResolved(originalRenderer, entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        EntityRenderer<?> identityRenderer = this.getRenderer(renderIdentity);
        if (identityRenderer == null) {
            identity2$renderResolved(originalRenderer, entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        IdentityRenderStateHelper.syncIdentityVisualState(entity, renderIdentity);
        identity2$renderResolved(identityRenderer, renderIdentity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void identity2$renderResolved(
            EntityRenderer<?> renderer,
            Entity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light
    ) {
        ((EntityRenderer) renderer).render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
