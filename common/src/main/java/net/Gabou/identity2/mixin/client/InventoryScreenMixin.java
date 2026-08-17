package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Redirect(
        method = "extractBackground",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"
        )
    )
    private void identity2$renderMorphPreview(
        GuiGraphicsExtractor context,
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
        InventoryScreen.extractEntityInInventoryFollowsMouse(
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
