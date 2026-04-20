package ember.qualitycommands.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.DragonEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.registry.Registries;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.MinecraftClient;
import ember.qualitycommands.util.MinecraftClientAccessor;
import ember.qualitycommands.util.LimbAnimatorAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;
import com.llamalad7.mixinextras.sugar.Local;
import ember.qualitycommands.util.PlayerEntityRendererAccessor;
import ember.qualitycommands.QualityCommands;
import java.lang.reflect.Field;
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin{
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
    @Redirect(method = "renderArmHoldingItem",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"))
    private void rahir(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible){
        renderRightArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderArm",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"))
    private void rar(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible){
        renderRightArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderArmHoldingItem",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"))
    private void rahil(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible){
        renderLeftArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    @Redirect(method = "renderArm",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"))
    private void ral(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible){
        renderLeftArmOverride(renderer,matrices,queue,light,skinTexture,sleeveVisible);
    }
    
    private void renderRightArmOverride(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible) {
        
        try{
        EntityAccessor playerEntity=(EntityAccessor)MinecraftClient.getInstance().player;
		Entity identity=playerEntity.getCurrentIdentity();
        if(identity==null){
            renderer.renderRightArm(matrices, queue, light, skinTexture, sleeveVisible);
            return;
        }
        //renderer.renderArm(matrices, queue, light, skinTexture, renderer.getModel().rightArm, sleeveVisible);
        if(((NbtComponentAccessor)(Object)playerEntity.getCustomData()).getNbt().getString("model_override").isPresent()){
            String d=((NbtComponentAccessor)(Object)playerEntity.getCustomData()).getNbt().getString("model_override").get();
            if((d.length()!=0)){
                if(identity==null){
                    playerEntity.setCurrentIdentity(d);
                    identity=playerEntity.getCurrentIdentity();
                }
                
        
        
        EntityRenderer idrenderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(identity);
        ModelPart targetPart=null;
        
        EntityModel eModel=null;
        if(idrenderer instanceof LivingEntityRenderer){
            try{
            eModel=((LivingEntityRenderer)idrenderer).getModel();
            }catch(Exception e){
                eModel=(EntityModel)getFieldFromClassHeirarchy(eModel.getClass(),"model").get((Object)eModel);
            }
        }
        if(idrenderer instanceof EnderDragonEntityRenderer){
            eModel=((ember.qualitycommands.util.EnderDragonEntityRendererAccessor)(EnderDragonEntityRenderer)idrenderer).getModel();
        }
        if(eModel!=null){
        targetPart=eModel.getRootPart().createPartGetter().apply(net.minecraft.client.render.entity.model.EntityModelPartNames.RIGHT_ARM);
        if(targetPart==null){
            targetPart=eModel.getRootPart().createPartGetter().apply(net.minecraft.client.render.entity.model.EntityModelPartNames.RIGHT_FRONT_LEG);
        }
        
        Identifier texture=null;
        if(idrenderer instanceof LivingEntityRenderer lidr){
            texture=lidr.getTexture((LivingEntityRenderState)lidr.createRenderState());
        }else{
            try{
            texture=(Identifier)getFieldFromClassHeirarchy(idrenderer.getClass(),"TEXTURE").get((Object)idrenderer);
            }catch(Exception e){
                int x=0;
            }
        }
        if((targetPart!=null)&&(texture!=null)){
            Identifier ftexture=texture;
            try {

                float paox=((PlayerEntityModel)renderer.getModel()).rightArm.originX;
                float paoy=((PlayerEntityModel)renderer.getModel()).rightArm.originY;
                float paoz=((PlayerEntityModel)renderer.getModel()).rightArm.originZ;
                float ox=targetPart.originX;
                float oy=targetPart.originY;
                float oz=targetPart.originZ;
                boolean shouldCancel=false;
                if(idrenderer instanceof LivingEntityRenderer){
                    shouldCancel=true;
                }
            if(true){
                targetPart.traverse().forEach((part)->{

                
                try{
                    
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, ftexture, part, sleeveVisible);
                }catch(Exception e){
                    int x=0;
                }
                
            });
        }else{
            targetPart.traverse().forEach((part)->{

                part.originX+=paox-ox;
                part.originY+=paoy-oy;
                part.originZ+=paoz-oz;
                
                try{
                    
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, ftexture, part, sleeveVisible);
                }catch(Exception e){
                    int x=0;
                }
                part.originX-=paox-ox;
                part.originY-=paoy-oy;
                part.originZ-=paoz-oz;
                
            });
        }
        } catch (Exception e) {
            int x=0;
            //QualityCommands.LOGGER.info(" TEXTURE missing");
        }
                                        
                }
            }else{
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, skinTexture, ((PlayerEntityModel)renderer.getModel()).rightArm, sleeveVisible);
            }
        }else{
            ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, skinTexture, ((PlayerEntityModel)renderer.getModel()).rightArm, sleeveVisible);
        }
        }
    } catch (Exception e) {
        QualityCommands.LOGGER.info("RenderError!");
    }
    }
    private void renderLeftArmOverride(PlayerEntityRenderer renderer, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible) {
        
        try{
        EntityAccessor playerEntity=(EntityAccessor)MinecraftClient.getInstance().player;
		Entity identity=playerEntity.getCurrentIdentity();
        if(identity==null){
            renderer.renderRightArm(matrices, queue, light, skinTexture, sleeveVisible);
            return;
        }
        //renderer.renderArm(matrices, queue, light, skinTexture, renderer.getModel().rightArm, sleeveVisible);
        if(((NbtComponentAccessor)(Object)playerEntity.getCustomData()).getNbt().getString("model_override").isPresent()){
            String d=((NbtComponentAccessor)(Object)playerEntity.getCustomData()).getNbt().getString("model_override").get();
            if((d.length()!=0)){
                if(identity==null){
                    playerEntity.setCurrentIdentity(d);
                    identity=playerEntity.getCurrentIdentity();
                }
                
        
        
        EntityRenderer idrenderer=((MinecraftClientAccessor)MinecraftClient.getInstance()).getEntityRenderManager().getRenderer(identity);
        ModelPart targetPart=null;
        
        EntityModel eModel=null;
        if(idrenderer instanceof LivingEntityRenderer){
            try{
            eModel=((LivingEntityRenderer)idrenderer).getModel();
            }catch(Exception e){
                eModel=(EntityModel)getFieldFromClassHeirarchy(eModel.getClass(),"model").get((Object)eModel);
            }
        }
        if(idrenderer instanceof EnderDragonEntityRenderer){
            eModel=((ember.qualitycommands.util.EnderDragonEntityRendererAccessor)(EnderDragonEntityRenderer)idrenderer).getModel();
        }
        if(eModel!=null){
        targetPart=eModel.getRootPart().createPartGetter().apply(net.minecraft.client.render.entity.model.EntityModelPartNames.LEFT_ARM);
        if(targetPart==null){
            targetPart=eModel.getRootPart().createPartGetter().apply(net.minecraft.client.render.entity.model.EntityModelPartNames.LEFT_FRONT_LEG);
        }
        
        Identifier texture=null;
        if(idrenderer instanceof LivingEntityRenderer lidr){
            texture=lidr.getTexture((LivingEntityRenderState)lidr.createRenderState());
        }else{
            try{
            texture=(Identifier)getFieldFromClassHeirarchy(idrenderer.getClass(),"TEXTURE").get((Object)idrenderer);
            }catch(Exception e){
                int x=0;
            }
        }
        if((targetPart!=null)&&(texture!=null)){
            Identifier ftexture=texture;
            try {

                float paox=((PlayerEntityModel)renderer.getModel()).leftArm.originX;
                float paoy=((PlayerEntityModel)renderer.getModel()).leftArm.originY;
                float paoz=((PlayerEntityModel)renderer.getModel()).leftArm.originZ;
                float ox=targetPart.originX;
                float oy=targetPart.originY;
                float oz=targetPart.originZ;
                boolean shouldCancel=false;
                if(idrenderer instanceof LivingEntityRenderer){
                    shouldCancel=true;
                }
            if(true){
                targetPart.traverse().forEach((part)->{

                
                try{
                    
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, ftexture, part, sleeveVisible);
                }catch(Exception e){
                    int x=0;
                }
                
            });
        }else{
            targetPart.traverse().forEach((part)->{

                part.originX+=paox-ox;
                part.originY+=paoy-oy;
                part.originZ+=paoz-oz;
                
                try{
                    
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, ftexture, part, sleeveVisible);
                }catch(Exception e){
                    int x=0;
                }
                part.originX-=paox-ox;
                part.originY-=paoy-oy;
                part.originZ-=paoz-oz;
                
            });
        }
        } catch (Exception e) {
            int x=0;
            //QualityCommands.LOGGER.info(" TEXTURE missing");
        }
                                        
                }
            }else{
                ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, skinTexture, ((PlayerEntityModel)renderer.getModel()).leftArm, sleeveVisible);
            }
        }else{
            ((PlayerEntityRendererAccessor)renderer).callRenderArm(matrices, queue, light, skinTexture, ((PlayerEntityModel)renderer.getModel()).leftArm, sleeveVisible);
        }
        }
    } catch (Exception e) {
        QualityCommands.LOGGER.info("RenderError!");
    }
    }
}
