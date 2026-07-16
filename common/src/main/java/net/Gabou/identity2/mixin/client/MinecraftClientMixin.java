package net.Gabou.identity2.mixin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
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
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.Gabou.identity2.util.MinecraftClientAccessor;
@Mixin(Minecraft.class)
public class MinecraftClientMixin implements MinecraftClientAccessor{
    @Shadow
    public EntityRenderDispatcher entityRenderDispatcher;
    public EntityRenderDispatcher getEntityRenderManager(){
        return this.entityRenderDispatcher;
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void identity2$shootShulkerOnLeftClick(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = ((Minecraft) (Object) this).player;
        if (player == null || !IdentitySettings.enableMorphAbilities) {
            return;
        }
        net.minecraft.world.entity.Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity != null
                && identity.getType() == net.minecraft.world.entity.EntityType.SHULKER
                && PredefIdentityAbilities.isShulkerOpen(player)) {
            Identity2Client.sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_OVERRIDE_ATTACK);
            cir.setReturnValue(false);
        }
    }
	
}
