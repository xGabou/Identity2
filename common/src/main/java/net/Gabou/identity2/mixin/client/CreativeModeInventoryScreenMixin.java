package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
    @Redirect(
        method = "renderBg",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"
        )
    )
    private void identity2$renderMorphPreview(
        GuiGraphics context,
        int x1,
        int y1,
        int x2,
        int y2,
        int scale,
        float bodyRotation,
        float mouseX,
        float mouseY,
        LivingEntity entity
    ) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            context,
            x1,
            y1,
            x2,
            y2,
            scale,
            bodyRotation,
            mouseX,
            mouseY,
            identity2$resolvePreviewEntity(entity)
        );
    }

    private static LivingEntity identity2$resolvePreviewEntity(LivingEntity fallback) {
        if (Minecraft.getInstance().player == null) {
            return fallback;
        }
        Entity identity = ((EntityAccessor) Minecraft.getInstance().player).getCurrentIdentity();
        if (identity instanceof LivingEntity livingIdentity) {
            return livingIdentity;
        }
        return fallback;
    }
}
