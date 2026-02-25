package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.PlayerEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.mojang.math.Axis;
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
        PartNames.RIGHT_FRONT_LEG,
        "rightFrontLeg",
        "right_front_foot",
        "rightFrontFoot",
        PartNames.RIGHT_HIND_LEG,
        "rightRearLeg",
        "right_hind_foot",
        "rightRearFoot",
        PartNames.RIGHT_WING,
        "rightWing",
        "right_wing_tip",
        "rightWingTip",
        "right_fin",
        "rightFin",
        "right_tentacle",
        "rightTentacle",
        "right_claw",
        "rightClaw",
        "right"
    };
    private static final String[] LEFT_HAND_PART_CANDIDATES = new String[] {
        PartNames.LEFT_ARM,
        "leftArm",
        PartNames.LEFT_FRONT_LEG,
        "leftFrontLeg",
        "left_front_foot",
        "leftFrontFoot",
        PartNames.LEFT_HIND_LEG,
        "leftRearLeg",
        "left_hind_foot",
        "leftRearFoot",
        PartNames.LEFT_WING,
        "leftWing",
        "left_wing_tip",
        "leftWingTip",
        "left_fin",
        "leftFin",
        "left_tentacle",
        "leftTentacle",
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
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V"
        )
    )
    private void identity2$redirectRenderPlayerRightArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, skinTexture, sleeveVisible, true);
    }

    @Redirect(
        method = "renderMapHand",
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V"
        )
    )
    private void identity2$redirectRenderMapRightArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, skinTexture, sleeveVisible, true);
    }

    @Redirect(
        method = "renderPlayerArm",
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V"
        )
    )
    private void identity2$redirectRenderPlayerLeftArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, skinTexture, sleeveVisible, false);
    }

    @Redirect(
        method = "renderMapHand",
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V"
        )
    )
    private void identity2$redirectRenderMapLeftArm(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible
    ) {
        identity2$renderArmOverride(renderer, matrices, queue, light, skinTexture, sleeveVisible, false);
    }

    private void identity2$renderArmOverride(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible,
        boolean rightArm
    ) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
                return;
            }

            Entity identity = ((EntityAccessor) client.player).getCurrentIdentity();
            if (identity == null) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
                return;
            }

            EntityRenderer<?, ?> identityRenderer = ((MinecraftClientAccessor) client).getEntityRenderManager().getRenderer(identity);
            if (identityRenderer == null) {
                identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
                return;
            }

            EntityModel<?> identityModel = null;
            if (identityRenderer instanceof LivingEntityRenderer livingRenderer) {
                identityModel = livingRenderer.getModel();
            } else if (identityRenderer instanceof EnderDragonRenderer dragonRenderer) {
                identityModel = ((EnderDragonEntityRendererAccessor) dragonRenderer).getModel();
            }

            if (identityModel == null) {
                if (identity.getType() == EntityType.PLAYER) {
                    identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
                }
                return;
            }

            ModelPart identityArm = identity2$resolveIdentityHandPart(identityModel, rightArm);
            if (identityArm == null) {
                if (identity.getType() == EntityType.PLAYER) {
                    identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
                }
                return;
            }

            ResourceLocation identityTexture = identity2$resolveIdentityTexture(identityRenderer);
            ResourceLocation texture = identityTexture == null ? skinTexture : identityTexture;

            PlayerModel playerModel = (PlayerModel) renderer.getModel();
            ModelPart playerArm = rightArm ? playerModel.rightArm : playerModel.leftArm;

            float offsetX = (playerArm.x - identityArm.x) + (rightArm ? ARM_TUNE_X : -ARM_TUNE_X);
            float offsetY = (playerArm.y - identityArm.y) + ARM_TUNE_Y;
            float offsetZ = (playerArm.z - identityArm.z) + ARM_TUNE_Z;

            matrices.pushPose();
            matrices.translate(offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(ARM_TUNE_ROT_X));
            matrices.mulPose(Axis.YP.rotationDegrees(rightArm ? ARM_TUNE_ROT_Y : -ARM_TUNE_ROT_Y));
            matrices.mulPose(Axis.ZP.rotationDegrees(rightArm ? ARM_TUNE_ROT_Z : -ARM_TUNE_ROT_Z));
            identity2$callRenderHand(renderer, matrices, queue, light, texture, identityArm, sleeveVisible);
            matrices.popPose();
        } catch (Exception ignored) {
            identity2$renderVanillaHand(renderer, matrices, queue, light, skinTexture, sleeveVisible, rightArm);
        }
    }

    private static void identity2$callRenderHand(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        ModelPart arm,
        boolean sleeveVisible
    ) {
        if (renderer instanceof PlayerEntityRendererAccessor accessor) {
            accessor.callRenderArm(matrices, queue, light, skinTexture, arm, sleeveVisible);
        } else {
            PlayerModel model = (PlayerModel) renderer.getModel();
            if (arm == model.leftArm) {
                renderer.renderLeftHand(matrices, queue, light, skinTexture, sleeveVisible);
            } else {
                renderer.renderRightHand(matrices, queue, light, skinTexture, sleeveVisible);
            }
        }
    }

    private static void identity2$renderVanillaHand(
        PlayerRenderer renderer,
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        boolean sleeveVisible,
        boolean rightArm
    ) {
        if (rightArm) {
            renderer.renderRightHand(matrices, queue, light, skinTexture, sleeveVisible);
        } else {
            renderer.renderLeftHand(matrices, queue, light, skinTexture, sleeveVisible);
        }
    }

    private static ModelPart identity2$resolveIdentityHandPart(EntityModel<?> model, boolean rightArm) {
        ModelPart root = model.root();
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

        if (exact != null) {
            return exact;
        }

        for (ModelPart part : root.getAllParts()) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        return null;
    }

    private static ModelPart identity2$findPartByNames(ModelPart root, String[] candidates) {
        java.util.function.Function<String, ModelPart> lookup = root.createPartLookup();
        for (String candidate : candidates) {
            ModelPart part = lookup.apply(candidate);
            if (part != null) {
                return part;
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

    private static ResourceLocation identity2$resolveIdentityTexture(EntityRenderer<?, ?> identityRenderer) {
        if (identityRenderer instanceof LivingEntityRenderer livingRenderer) {
            try {
                return (ResourceLocation) livingRenderer.getTextureLocation((LivingEntityRenderState) livingRenderer.createRenderState());
            } catch (Throwable ignored) {
            }
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
