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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.registry.Registries;
import ember.qualitycommands.util.EntityAccessor;
import ember.qualitycommands.util.NbtComponentAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
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
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin implements ember.qualitycommands.util.PlayerEntityRendererAccessor{
    @Shadow
    private void renderArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture,ModelPart arm, boolean sleeveVisible) {}
    public void callRenderArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture,ModelPart arm, boolean sleeveVisible) {
    this.renderArm(matrices, queue, light, skinTexture,arm, sleeveVisible);
    }
    /*@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void getAndUpdateRenderStateModifier(AbstractClientPlayerEntity entity,PlayerEntityRenderState playerEntityRenderState, float tickProgress,CallbackInfo info) {
		PlayerEntityRenderState entityRenderState=(PlayerEntityRenderState)playerEntityRenderState;
        if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").isPresent()){
            if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get().length()!=0){
                if(Registries.ENTITY_TYPE.containsId(Identifier.of(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get()))){
                    EntityType<?> newType=Registries.ENTITY_TYPE.get(Identifier.of(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get()));
                    playerEntityRenderState.entityType=newType;
                }
                
            }
        }
	}*/
}
