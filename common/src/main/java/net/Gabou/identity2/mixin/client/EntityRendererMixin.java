package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void identity2$createRenderState(T entity, float tickProgress, CallbackInfoReturnable<S> cir) {
        identity2$applyModelPartOverrides(entity);
    }

    private static void identity2$applyModelPartOverrides(Entity entity) {
        CompoundTag nbt = ((NbtComponentAccessor) (Object) (((EntityAccessor) entity).getCustomData())).getNbt();
        boolean hasHiddenPartOverrides = false;
        for (String key : nbt.keySet()) {
            if (key.startsWith("hidden_parts.")) {
                hasHiddenPartOverrides = true;
                break;
            }
        }

        boolean shouldHideHead = false;
        if (Minecraft.getInstance().player != null) {
            Entity playerIdentity = ((EntityAccessor) Minecraft.getInstance().player).getCurrentIdentity();
            shouldHideHead = playerIdentity instanceof Shulker;
        }

        if (!hasHiddenPartOverrides && !shouldHideHead) {
            return;
        }

        EntityModel model = Identity2Client.getModel(entity);
        if (model == null) {
            return;
        }

        if (hasHiddenPartOverrides) {
            for (String key : nbt.keySet()) {
                if (key.startsWith("hidden_parts.")) {
                    ModelPart part = model.root().createPartLookup().apply(key.substring(13));
                    if (part != null) {
                        part.skipDraw = nbt.getBooleanOr(key, false);
                    }
                }
            }
        }

        if (shouldHideHead) {
            ModelPart head = model.root().createPartLookup().apply("head");
            if (head != null) {
                head.skipDraw = true;
                head.xScale = 0.0F;
            }
        }
    }
}
