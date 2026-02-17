package net.Gabou.identity2.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public interface PlayerEntityRendererAccessor {
    void callRenderArm(
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        Identifier skinTexture,
        ModelPart arm,
        boolean sleeveVisible
    );
}
