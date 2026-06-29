package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.ModelPartCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    // ModelPart origins are in 1/16th block units.
    // Tune these if the morph arm still needs adjustment.
    private static final float ARM_TUNE_X = 0.0f;
    private static final float ARM_TUNE_Y = 0.0f;
    private static final float ARM_TUNE_Z = -2.0f;
    private static final float ARM_TUNE_ROT_X = -8.0f;
    private static final float ARM_TUNE_ROT_Y = 0.0f;
    private static final float ARM_TUNE_ROT_Z = 16.0f;
    private static final String[] RIGHT_HAND_PART_CANDIDATES = new String[] {
        PartNames.RIGHT_ARM,
        "rightArm",
        "right_hand",
        "rightHand",
        "right_arm",
        "body.right_arm",
        "body.rightArm",
        "bone.body.right_arm",
        "bone.body.rightArm",
        "root.right_arm",
        "root.rightArm",
        "right_front_leg",
        "right_front_foot",
        "rightFrontLeg",
        "rightFrontFoot",
        "right_wing",
        "rightWing",
        "right_claw",
        "rightClaw",
        "right"
    };
    private static final String[] LEFT_HAND_PART_CANDIDATES = new String[] {
        PartNames.LEFT_ARM,
        "leftArm",
        "left_hand",
        "leftHand",
        "left_arm",
        "body.left_arm",
        "body.leftArm",
        "bone.body.left_arm",
        "bone.body.leftArm",
        "root.left_arm",
        "root.leftArm",
        "left_front_leg",
        "left_front_foot",
        "leftFrontLeg",
        "leftFrontFoot",
        "left_wing",
        "leftWing",
        "left_claw",
        "leftClaw",
        "left"
    };
    private static Field modelPartChildrenField;

    private static Field getFieldFromClassHeirarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }

    @Redirect(
        method = "renderPlayerArm",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"
        )
    )
    private void identity2$redirectRenderPlayerRightArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, player, true);
    }

    @Redirect(
        method = "renderMapHand",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"
        )
    )
    private void identity2$redirectRenderMapRightArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, player, true);
    }

    @Redirect(
        method = "renderPlayerArm",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"
        )
    )
    private void identity2$redirectRenderPlayerLeftArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, player, false);
    }

    @Redirect(
        method = "renderMapHand",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"
        )
    )
    private void identity2$redirectRenderMapLeftArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, player, false);
    }

    private void identity2$renderArmOverride(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player,
        boolean rightArm
    ) {
        Minecraft client = Minecraft.getInstance();
        Entity identity = client.player == null ? null : ((EntityAccessor) client.player).getCurrentIdentity();
        boolean morphed = identity != null;
        try {
            if (client.player == null) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, player, rightArm);
                return;
            }

            if (identity == null) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, player, rightArm);
                return;
            }

            EntityRenderer<?> identityRenderer = ((MinecraftClientAccessor) client).getEntityRenderManager().getRenderer(identity);
            if (identityRenderer == null) {
                return;
            }

            EntityModel<?> identityModel = null;
            if (identityRenderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> livingRenderer) {
                identityModel = livingRenderer.getModel();
            } else if (identityRenderer instanceof EnderDragonRenderer dragonRenderer) {
                identityModel = ((EnderDragonEntityRendererAccessor) dragonRenderer).getModel();
            }

            if (identityModel == null) {
                return;
            }

            ModelPart identityArm = identity2$resolveIdentityHandPart(identityModel, rightArm);
            if (identityArm == null) {
                return;
            }

            ResourceLocation identityTexture = identity2$resolveIdentityTexture(identityRenderer, identity);
            if (identityTexture == null) {
                return;
            }

            PlayerModel playerModel = renderer.getModel();
            ModelPart playerArm = rightArm ? playerModel.rightArm : playerModel.leftArm;

            float offsetX = Mth.clamp((playerArm.x - identityArm.x), -12.0F, 12.0F) + (rightArm ? ARM_TUNE_X : -ARM_TUNE_X);
            float offsetY = Mth.clamp((playerArm.y - identityArm.y), -12.0F, 12.0F) + ARM_TUNE_Y;
            float offsetZ = Mth.clamp((playerArm.z - identityArm.z), -12.0F, 12.0F) + ARM_TUNE_Z;

            matrices.pushPose();
            matrices.translate(offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(ARM_TUNE_ROT_X));
            matrices.mulPose(Axis.YP.rotationDegrees(rightArm ? ARM_TUNE_ROT_Y : -ARM_TUNE_ROT_Y));
            matrices.mulPose(Axis.ZP.rotationDegrees(rightArm ? ARM_TUNE_ROT_Z : -ARM_TUNE_ROT_Z));
            ModelPartCompat.PartSnapshot armSnapshot = ModelPartCompat.PartSnapshot.capture(identityArm);
            try {
                identityArm.resetPose();
                identity2$renderIdentityHand(matrices, queue, light, identityTexture, identityArm);
            } finally {
                armSnapshot.restore(identityArm);
            }
            matrices.popPose();
        } catch (Exception ignored) {
            if (!morphed) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, player, rightArm);
            }
        }
    }

    private static void identity2$renderIdentityHand(
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation texture,
        ModelPart arm
    ) {
        VertexConsumer vertexConsumer = queue.getBuffer(RenderType.entityCutoutNoCull(texture));
        arm.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
    }

    private static void identity2$renderVanillaHand(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        AbstractClientPlayer player,
        boolean rightArm
    ) {
        if (rightArm) {
            renderer.renderRightHand(matrices, queue, light, player);
        } else {
            renderer.renderLeftHand(matrices, queue, light, player);
        }
    }

    private static ModelPart identity2$resolveIdentityHandPart(EntityModel<?> model, boolean rightArm) {
        if (model instanceof HumanoidModel<?> humanoidModel) {
            ModelPart humanoidArm = rightArm ? humanoidModel.rightArm : humanoidModel.leftArm;
            if (humanoidArm != null && !humanoidArm.isEmpty()) {
                return humanoidArm;
            }
        }

        ModelPart root = ModelPartCompat.tryGetRoot(model);
        if (root == null) {
            return null;
        }
        ModelPart exact = identity2$findPartByNames(root, rightArm ? RIGHT_HAND_PART_CANDIDATES : LEFT_HAND_PART_CANDIDATES);
        if (exact != null && !exact.isEmpty()) {
            return exact;
        }
        ModelPart preferred = identity2$findNamedSidePart(root, rightArm, true);
        if (preferred != null) {
            return preferred;
        }
        ModelPart anySide = identity2$findNamedSidePart(root, rightArm, false);
        if (anySide != null) {
            return anySide;
        }
        return null;
    }

    private static ModelPart identity2$findPartByNames(ModelPart root, String[] candidates) {
        for (String candidate : candidates) {
            ModelPart current = root;
            boolean valid = true;

            for (String segment : candidate.split("\\.")) {
                try {
                    current = current.getChild(segment);
                } catch (IllegalArgumentException e) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return current;
            }
        }
        return null;
    }

    private static ModelPart identity2$findNamedSidePart(ModelPart root, boolean rightArm, boolean requirePreferredToken) {
        return identity2$findNamedSidePartRecursive(root, rightArm, requirePreferredToken);
    }

    private static ModelPart identity2$findNamedSidePartRecursive(ModelPart part, boolean rightArm, boolean requirePreferredToken) {
        Map<String, ModelPart> children = identity2$getModelPartChildren(part);
        if (children == null || children.isEmpty()) {
            return null;
        }

        String sideToken = rightArm ? "right" : "left";
        ModelPart fallback = null;
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            String name = entry.getKey();
            ModelPart child = entry.getValue();
            String lower = name.toLowerCase(Locale.ROOT);
            boolean sideMatch = lower.contains(sideToken);
            boolean preferredToken = lower.contains("arm")
                || lower.contains("hand")
                || lower.contains("wing")
                || lower.contains("front")
                || lower.contains("leg")
                || lower.contains("foot")
                || lower.contains("claw")
                || lower.contains("tentacle")
                || lower.contains("fin");

            if (sideMatch && (!requirePreferredToken || preferredToken) && !child.isEmpty()) {
                return child;
            }
            ModelPart nested = identity2$findNamedSidePartRecursive(child, rightArm, requirePreferredToken);
            if (nested != null) {
                return nested;
            }
            if (sideMatch && fallback == null) {
                fallback = child;
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> identity2$getModelPartChildren(ModelPart part) {
        try {
            if (modelPartChildrenField == null) {
                modelPartChildrenField = getFieldFromClassHeirarchy(ModelPart.class, "children");
            }
            return (Map<String, ModelPart>) modelPartChildrenField.get(part);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ResourceLocation identity2$resolveIdentityTexture(EntityRenderer<?> identityRenderer, Entity identity) {
        try {
            return ((EntityRenderer) identityRenderer).getTextureLocation(identity);
        } catch (Throwable ignored) {
        }
        if (identityRenderer instanceof EnderDragonRenderer) {
            try {
                return (ResourceLocation) getFieldFromClassHeirarchy(identityRenderer.getClass(), "DRAGON_LOCATION").get(null);
            } catch (Throwable ignored) {
            }
        }
        try {
            return (ResourceLocation) getFieldFromClassHeirarchy(identityRenderer.getClass(), "TEXTURE").get(identityRenderer);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
