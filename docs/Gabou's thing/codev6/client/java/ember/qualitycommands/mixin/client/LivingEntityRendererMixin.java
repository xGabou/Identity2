package ember.qualitycommands.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;

import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Call;
import org.apache.logging.log4j.core.config.Order;
import org.jetbrains.annotations.Nullable;
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
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import ember.qualitycommands.ModEffects;
import ember.qualitycommands.QualityCommands;
import ember.qualitycommands.QualityCommandsClient;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.EntityRenderStateModifier;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import ember.qualitycommands.util.MinecraftClientAccessor;
import ember.qualitycommands.util.LimbAnimatorAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity,S extends LivingEntityRenderState, M extends EntityModel<S>>{
    @Shadow
    public static int getOverlay(LivingEntityRenderState entity, float whiteOverlayProgress) {
        return 0;
    }

    @Shadow protected float getAnimationCounter(S state){
        return 0.0f;
    };

    @Shadow public M getModel(){
        return null;
    };

    @Shadow protected boolean isVisible(S state){return false;}
    @Shadow protected M model;
    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", shift = At.Shift.BEFORE)//@At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILjava/lang/Object;ILjava/lang/Object;)V", shift = At.Shift.AFTER)
            
    )
    private void renderOverlayTexture(
            S livingEntityRenderStateobj,
            MatrixStack matrixStack,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            CameraRenderState cameraRenderState,
            CallbackInfo info
    ) {
        S livingEntityRenderState=(S)livingEntityRenderStateobj;
        QualityCommands.LOGGER.info("attempting render overlay textures");
        if (true||livingEntityRenderState instanceof PlayerEntityRenderState pers) {
            int overlay = getOverlay(livingEntityRenderState, getAnimationCounter(livingEntityRenderState));
            // todo: Revisar esto, lo de  hacer loop en cada frame como que no mola
                        boolean visible = isVisible(livingEntityRenderState);
                        // Si a pesar de no ser visible, lo puedes ver (espectador)
                        boolean revealed =!visible;

                        int argb = ColorHelper.fromFloats (revealed ? 0.15F : visible ? 1.0F : 0F,1, 1, 1);
                        
                        

                        // Si el modelo es visible
                        

                        // Only if you can directly see it
                        //if (visible) {
                        //    List<ModelColorPowerType> modelColorPowers = PowerHolderComponent.getPowerTypes(acpe, ModelColorPowerType.class);
                        //    if (!modelColorPowers.isEmpty()) {
                        //        argb = Alib.getArgbFromColorPowers(argb, modelColorPowers);
                        //    }
                        //}
                        for(String overlayTextureString:((EntityRenderStateModifier)livingEntityRenderState).getOverlays()){
                            QualityCommands.LOGGER.info(overlayTextureString);
                            ResourceLocation overlayTexture=ResourceLocation.of(overlayTextureString);
                        if ((overlayTextureString.length()>0)&overlayTexture != null) {
                            RenderLayer renderLayer;
//                                if (OriginsFursClient.isRenderingInWorld && FabricLoader.getInstance().isModLoaded("iris")) {
//                                    renderLayer = RenderLayer.getEntityTranslucent(overlayTexture);
//                                } else {
                            //renderLayer = RenderLayer.getEntityTranslucent(overlayTexture);
                            renderLayer=this.model.getLayer(overlayTexture);
//                                }
                            /*this.model.render(
                                    matrixStack,
                                    vertexConsumerProvider.getBuffer(renderLayer),
                                    light,
                                    overlay,
                                    argb
                            );*/
                            orderedRenderCommandQueue.submitModel(
                                this.model, livingEntityRenderState, matrixStack, renderLayer, livingEntityRenderState.light, overlay, argb, null, livingEntityRenderState.outlineColor, null
                            );
                        }
                    }
                    for(String overlayTextureString:((EntityRenderStateModifier)livingEntityRenderState).getOverlaysE()){
                        ResourceLocation emissiveTexture=ResourceLocation.of(overlayTextureString);
                        if (emissiveTexture != null) {
                            RenderLayer renderLayer = RenderLayer.getEntityTranslucentEmissive(emissiveTexture);
                            renderLayer=this.model.getLayer(emissiveTexture);
                            /*this.model.render(
                                    matrixStack,
                                    vertexConsumerProvider.getBuffer(renderLayer),
                                    light,
                                    overlay,
                                    argb
                            );*/
                            orderedRenderCommandQueue.submitModel(
                                this.model, livingEntityRenderState, matrixStack, renderLayer, livingEntityRenderState.light, overlay, argb, null, livingEntityRenderState.outlineColor, null
                            );
                        }
                    }
                    
                
            
        }
    }
}
