package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.ModelPartCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    private static final ThreadLocal<Map<ModelPart, ModelPartCompat.PartSnapshot>> identity2$snapshots =
            ThreadLocal.withInitial(HashMap::new);

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void identity2$beforeRender(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        identity2$applyModelPartOverrides(entity);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void identity2$afterRender(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        identity2$restoreModelPartOverrides();
    }

    private static void identity2$applyModelPartOverrides(Entity entity) {
        Map<ModelPart, ModelPartCompat.PartSnapshot> snapshots = identity2$snapshots.get();
        snapshots.clear();

        if (!(entity instanceof EntityAccessor accessor)) {
            return;
        }
        CompoundTag nbt = accessor.getCustomData();
        boolean hasHiddenPartOverrides = false;

        for (String key : net.Gabou.identity2.util.NbtCompat.keySet(nbt)) {
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

        EntityModel<?> model = Identity2Client.getModel(entity);
        if (model == null) {
            return;
        }

        if (hasHiddenPartOverrides) {
            for (String key : net.Gabou.identity2.util.NbtCompat.keySet(nbt)) {
                if (!key.startsWith("hidden_parts.")) continue;

                String partName = key.substring("hidden_parts.".length());
                ModelPart root = ModelPartCompat.tryGetRoot(model);
                if (root == null) return;

                ModelPart part = ModelPartCompat.tryGetChild(root, partName);
                if (part != null) {

                    snapshots.putIfAbsent(part, ModelPartCompat.PartSnapshot.capture(part));
                    part.skipDraw = net.Gabou.identity2.util.NbtCompat.getBooleanOr(nbt, key, false);
                }
            }
        }

        if (shouldHideHead) {
            ModelPart root = ModelPartCompat.tryGetRoot(model);
            if (root == null) return;
            ModelPart head = ModelPartCompat.tryGetChild(root, "head");
            if (head != null) {
                snapshots.putIfAbsent(head, ModelPartCompat.PartSnapshot.capture(head));
                head.skipDraw = true;
                head.xScale = 0.0F;
            }
        }
    }

    private static void identity2$restoreModelPartOverrides() {
        Map<ModelPart, ModelPartCompat.PartSnapshot> snapshots = identity2$snapshots.get();
        for (Map.Entry<ModelPart, ModelPartCompat.PartSnapshot> e : snapshots.entrySet()) {
            e.getValue().restore(e.getKey());
        }
        snapshots.clear();
    }


}
