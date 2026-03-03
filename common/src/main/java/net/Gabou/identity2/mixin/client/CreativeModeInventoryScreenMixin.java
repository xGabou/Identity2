package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
    @Redirect(
        method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V"
        ),
        require = 0
    )
    private void identity2$renderMorphPreviewInCreativeInventory(
        GuiGraphics graphics,
        int x,
        int y,
        int scale,
        float mouseX,
        float mouseY,
        LivingEntity entity
    ) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics,
            x,
            y,
            scale,
            mouseX,
            mouseY,
            identity2$resolvePreviewEntity(entity)
        );
    }

    private static LivingEntity identity2$resolvePreviewEntity(LivingEntity fallback) {
        if (!(fallback instanceof Player player)) {
            return fallback;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity instanceof LivingEntity livingIdentity && !livingIdentity.isRemoved()) {
            return livingIdentity;
        }
        return fallback;
    }
}

