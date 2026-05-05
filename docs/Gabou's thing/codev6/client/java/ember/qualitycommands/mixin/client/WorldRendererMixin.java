package ember.qualitycommands.mixin.client;

import net.minecraft.registry.Registries;
import ember.qualitycommands.QualityCommandsClient;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.block.entity.state.ShulkerBoxBlockEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ShulkerEntity;
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
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import ember.qualitycommands.ModEffects;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import java.util.Set;
import ember.qualitycommands.ModBlocks;
import net.minecraft.client.world.ClientWorld;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.BlockEntityAccessor;
import ember.qualitycommands.util.EnderDragonEntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import java.util.function.BiFunction;
import net.minecraft.util.ResourceLocation;
import ember.qualitycommands.QualityCommands;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
@Mixin(WorldRenderer.class)
public class WorldRendererMixin{
	private static ShulkerBoxBlockEntity box=null;
    @Inject(method = "fillEntityRenderStates", at = @At("TAIL"))
	private void fillEntityRenderStates(Camera camera, Frustum frustum, RenderTickCounter tickCounter, WorldRenderState renderStates,CallbackInfo info) {
        if(MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson()==false){
        if(((EntityAccessor)MinecraftClient.getInstance().player).getCurrentIdentity()!=null)
            if(((EntityAccessor)MinecraftClient.getInstance().player).getCurrentIdentity() instanceof ShulkerEntity shulkerIdentity){
                float g = tickCounter.getTickProgress(false);
                EntityRenderState entityRenderState = this.getAndUpdateRenderState(MinecraftClient.getInstance().player, g);
                //renderStates.entityRenderStates.add(entityRenderState);
                if(box==null){
                    box=(ShulkerBoxBlockEntity)((ShulkerBoxBlock)Blocks.SHULKER_BOX).createBlockEntity(BlockPos.ofFloored(0,0,0),Blocks.SHULKER_BOX.getDefaultState());
                }
                ((BlockEntityAccessor)box).setPos(shulkerIdentity.getBlockPos());
                box.setWorld(shulkerIdentity.getEntityWorld());
                BlockEntityRenderState blockEntityRenderState = this.blockEntityRenderManager.getRenderState(box, g, null);
                if (blockEntityRenderState != null) {
                    if(blockEntityRenderState instanceof ShulkerBoxBlockEntityRenderState sbers){
                        sbers.facing=shulkerIdentity.getAttachedFace().getOpposite();
                        sbers.dyeColor=shulkerIdentity.getColor();
                        sbers.animationProgress=shulkerIdentity.getOpenProgress(g);
                        renderStates.blockEntityRenderStates.add(sbers);
                    }
                }else{
                    QualityCommands.LOGGER.info("Invalid rstate");
                }
                //renderStates.blockEntityRenderStates.add(null);
            }
    }
    }
    @Shadow
    private BlockEntityRenderManager blockEntityRenderManager;
    @Shadow
    private EntityRenderState getAndUpdateRenderState(Entity entity, float tickProgress) {return null;}
}
