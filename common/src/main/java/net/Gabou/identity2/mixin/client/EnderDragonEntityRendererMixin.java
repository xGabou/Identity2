package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
@Mixin(EnderDragonRenderer.class)
public class EnderDragonEntityRendererMixin implements EnderDragonEntityRendererAccessor{
    @Shadow
    public EnderDragonRenderer.DragonModel model;
    @Unique
    private boolean identity2$dragonFlipPosePushed;

    public EnderDragonRenderer.DragonModel getModel(){
        return this.model;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void identity2$applyDragonFlip(
            EnderDragon dragon,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource buffer,
            int light,
            CallbackInfo info
    ) {
        this.identity2$dragonFlipPosePushed = false;
        Entity owner = ((EntityAccessor) dragon).getIdentityOwner();
        if (owner == null) {
            return;
        }
        int remaining = PredefIdentityAbilities.getSyncedTicksRemaining(
                owner,
                PredefIdentityAbilities.ANIM_DRAGON_FLIP_TICKS_KEY
        );
        if (remaining <= 0) {
            return;
        }
        int duration = 28;
        int elapsed = net.minecraft.util.Mth.clamp(duration - remaining, 0, duration);
        float progress = elapsed / (float) duration;
        float eased = 0.5F - 0.5F * net.minecraft.util.Mth.cos(progress * (float) Math.PI);
        matrices.pushPose();
        matrices.mulPose(Axis.XP.rotationDegrees(eased * 360.0F));
        this.identity2$dragonFlipPosePushed = true;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void identity2$popDragonFlip(
            EnderDragon dragon,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource buffer,
            int light,
            CallbackInfo info
    ) {
        if (this.identity2$dragonFlipPosePushed) {
            matrices.popPose();
            this.identity2$dragonFlipPosePushed = false;
        }
    }
	
}
