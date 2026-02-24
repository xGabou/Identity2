package net.Gabou.identity2.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public interface PlayerEntityRendererAccessor {
    void callRenderArm(
        PoseStack matrices,
        MultiBufferSource queue,
        int light,
        ResourceLocation skinTexture,
        ModelPart arm,
        boolean sleeveVisible
    );
}
