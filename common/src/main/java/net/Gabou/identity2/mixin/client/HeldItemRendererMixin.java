package net.Gabou.identity2.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

import net.minecraft.client.model.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.Gabou.identity2.ModEffects;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;
import net.Gabou.identity2.util.PlayerEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.Field;
@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin{
    // ModelPart origins are in 1/16th block units.
    // Tune these if the morph arm still needs adjustment.
    private static final float ARM_TUNE_X = 0.0f;
    private static final float ARM_TUNE_Y = 0.0f;
    private static final float ARM_TUNE_Z = -2.0f;

    private static Field getFieldFromClassHeirarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true); // Strip Java access checks
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass(); // Move up the hierarchy
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }
    @Redirect(method = "renderPlayerArm",
              require = 0,
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;Z)V"))
    private void rahir(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible){
        renderRightArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderMapHand",
              require = 0,
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;Z)V"))
    private void rar(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible){
        renderRightArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderPlayerArm",
              require = 0,
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;Z)V"))
    private void rahil(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible){
        renderLeftArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderMapHand",
              require = 0,
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;Z)V"))
    private void ral(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible){
        renderLeftArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    
    private void renderRightArmOverride(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible) {
        try {
            EntityAccessor playerEntity = (EntityAccessor) Minecraft.getInstance().player;
            Entity identity = playerEntity.getCurrentIdentity();
            if (identity == null) {
                renderer.renderRightHand(matrices, queue, light, skinTexture, sleeveVisible);
                return;
            }
            EntityRenderer idrenderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager().getRenderer(identity);
            ModelPart targetPart = null;

            EntityModel eModel = null;
            if (idrenderer instanceof LivingEntityRenderer) {
                try {
                    eModel = ((LivingEntityRenderer) idrenderer).getModel();
                } catch (Exception e) {
                    eModel = (EntityModel) getFieldFromClassHeirarchy(eModel.getClass(), "model").get((Object) eModel);
                }
            }
            if (idrenderer instanceof EnderDragonRenderer) {
                eModel = ((net.Gabou.identity2.util.EnderDragonEntityRendererAccessor) (EnderDragonRenderer) idrenderer).getModel();
            }
            if (eModel == null) {
                return;
            }
            targetPart = eModel.root().createPartLookup().apply(net.minecraft.client.model.geom.PartNames.RIGHT_ARM);
            if (targetPart == null) {
                targetPart = eModel.root().createPartLookup().apply(net.minecraft.client.model.geom.PartNames.RIGHT_FRONT_LEG);
            }
            /*if (targetPart == null) {
                targetPart = eModel.root().createPartLookup().apply("body");
            }
            if (targetPart == null) {
                targetPart = eModel.root();
            }*/

            ResourceLocation texture = null;
            if (idrenderer instanceof LivingEntityRenderer lidr && identity instanceof LivingEntity livingIdentity) {
                LivingEntityRenderState renderState = (LivingEntityRenderState) lidr.createRenderState();
                lidr.extractRenderState(livingIdentity, renderState, 0.0F);
                texture = lidr.getTextureLocation(renderState);
            } else {
                try {
                    texture = (ResourceLocation) getFieldFromClassHeirarchy(idrenderer.getClass(), "TEXTURE").get((Object) idrenderer);
                } catch (Exception ignored) {
                }
            }
            if (targetPart == null || texture == null) {
                return;
            }
            try {
                float paox = ((PlayerModel) renderer.getModel()).rightArm.x;
                float paoy = ((PlayerModel) renderer.getModel()).rightArm.y;
                float paoz = ((PlayerModel) renderer.getModel()).rightArm.z;
                float ox = targetPart.x;
                float oy = targetPart.y;
                float oz = targetPart.z;
                float offsetX = Mth.clamp((paox - ox) + ARM_TUNE_X, -8.0F, 8.0F);
                float offsetY = Mth.clamp((paoy - oy) + ARM_TUNE_Y, -8.0F, 8.0F);
                float offsetZ = Mth.clamp((paoz - oz) + ARM_TUNE_Z, -8.0F, 8.0F);
                matrices.pushPose();
                matrices.translate(offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
                ((PlayerEntityRendererAccessor) renderer).callRenderArm(matrices, queue, light, texture, targetPart, sleeveVisible);
                matrices.popPose();
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }
    private void renderLeftArmOverride(AvatarRenderer renderer, PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture, boolean sleeveVisible) {
        try {
            EntityAccessor playerEntity = (EntityAccessor) Minecraft.getInstance().player;
            Entity identity = playerEntity.getCurrentIdentity();
            if (identity == null) {
                renderer.renderLeftHand(matrices, queue, light, skinTexture, sleeveVisible);
                return;
            }
            EntityRenderer idrenderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager().getRenderer(identity);
            ModelPart targetPart = null;

            EntityModel eModel = null;
            if (idrenderer instanceof LivingEntityRenderer) {
                try {
                    eModel = ((LivingEntityRenderer) idrenderer).getModel();
                } catch (Exception e) {
                    eModel = (EntityModel) getFieldFromClassHeirarchy(eModel.getClass(), "model").get((Object) eModel);
                }
            }
            if (idrenderer instanceof EnderDragonRenderer) {
                eModel = ((net.Gabou.identity2.util.EnderDragonEntityRendererAccessor) (EnderDragonRenderer) idrenderer).getModel();
            }
            if (eModel == null) {
                return;
            }
            targetPart = eModel.root().createPartLookup().apply(net.minecraft.client.model.geom.PartNames.LEFT_ARM);
            if (targetPart == null) {
                targetPart = eModel.root().createPartLookup().apply(net.minecraft.client.model.geom.PartNames.LEFT_FRONT_LEG);
            }
            /*if (targetPart == null) {
                targetPart = eModel.root().createPartLookup().apply("body");
            }
            if (targetPart == null) {
                targetPart = eModel.root();
            }*/

            ResourceLocation texture = null;
            if (idrenderer instanceof LivingEntityRenderer lidr && identity instanceof LivingEntity livingIdentity) {
                LivingEntityRenderState renderState = (LivingEntityRenderState) lidr.createRenderState();
                lidr.extractRenderState(livingIdentity, renderState, 0.0F);
                texture = lidr.getTextureLocation(renderState);
            } else {
                try {
                    texture = (ResourceLocation) getFieldFromClassHeirarchy(idrenderer.getClass(), "TEXTURE").get((Object) idrenderer);
                } catch (Exception ignored) {
                }
            }
            if (targetPart == null || texture == null) {
                return;
            }
            try {
                float paox = ((PlayerModel) renderer.getModel()).leftArm.x;
                float paoy = ((PlayerModel) renderer.getModel()).leftArm.y;
                float paoz = ((PlayerModel) renderer.getModel()).leftArm.z;
                float ox = targetPart.x;
                float oy = targetPart.y;
                float oz = targetPart.z;
                float offsetX = Mth.clamp((paox - ox) - ARM_TUNE_X, -8.0F, 8.0F);
                float offsetY = Mth.clamp((paoy - oy) + ARM_TUNE_Y, -8.0F, 8.0F);
                float offsetZ = Mth.clamp((paoz - oz) + ARM_TUNE_Z, -8.0F, 8.0F);
                matrices.pushPose();
                matrices.translate(offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
                ((PlayerEntityRendererAccessor) renderer).callRenderArm(matrices, queue, light, texture, targetPart, sleeveVisible);
                matrices.popPose();
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }
}

