package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.client.render.MorphRenderStateHelper;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    // The first-person state requires an AvatarRenderState even while the world renders a morph.
    @Redirect(method = "extractPlayerState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState identity2$extractLocalAvatar(LevelExtractor extractor, Entity player, float partialTick) {
        return MorphRenderStateHelper.extractAvatarState(player, partialTick);
    }
}
