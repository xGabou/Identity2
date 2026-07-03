package net.Gabou.identity2.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.Gabou.identity2.util.EntityNbtIoCompat;
import org.jetbrains.annotations.Nullable;

final class IdentityMenuRenderHelper {
    private static final float MAX_MOUSE_COMPONENT = 1.0F;

    private IdentityMenuRenderHelper() {
    }

    @Nullable
    static LivingEntity buildPreviewEntity(ResourceLocation entityId, @Nullable CompoundTag variantNbt) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            return null;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            return null;
        }

        Entity entity;
        try {
            entity = type.create(world);
        } catch (Throwable ignored) {
            return null;
        }
        if (!(entity instanceof LivingEntity living)) {
            disposeEntity(entity);
            return null;
        }

        if (variantNbt != null && !variantNbt.isEmpty()) {
            applyVariantData(living, world, variantNbt);
        }

        return living;
    }

    static void renderEntityInBox(
        GuiGraphics context,
        int left,
        int top,
        int right,
        int bottom,
        int mouseX,
        int mouseY,
        float idleTick,
        LivingEntity entity
    ) {
        if (entity == null || right - left < 8 || bottom - top < 8) {
            return;
        }

        int inset = 3;
        int renderLeft = left + inset;
        int renderTop = top + inset;
        int renderRight = right - inset;
        int renderBottom = bottom - inset;
        if (renderRight - renderLeft < 6 || renderBottom - renderTop < 6) {
            return;
        }

        int scale = computeScale(entity, renderRight - renderLeft, renderBottom - renderTop);
        float centerX = (renderLeft + renderRight) * 0.5F;
        float centerY = (renderTop + renderBottom) * 0.5F;
        float angleX = Mth.clamp((centerX - mouseX) / 40.0F, -MAX_MOUSE_COMPONENT, MAX_MOUSE_COMPONENT);
        float angleY = Mth.clamp((centerY - mouseY) / 45.0F, -MAX_MOUSE_COMPONENT, MAX_MOUSE_COMPONENT);
        angleX += Mth.sin(idleTick * 0.07F) * 0.12F;
        angleY *= 0.55F;

        float syntheticMouseX = centerX - angleX * 40.0F;
        float syntheticMouseY = centerY - angleY * 40.0F;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            context,
            renderLeft,
            renderTop,
            renderRight,
            renderBottom,
            scale,
            0.0F,
            syntheticMouseX,
            syntheticMouseY,
            entity
        );
    }

    static float resolveIdleTick(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            return client.level.getGameTime() + partialTick;
        }
        return (float) ((System.currentTimeMillis() % 100000L) / 50.0D);
    }

    static void disposeEntity(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }
        try {
            entity.discard();
        } catch (Throwable ignored) {
        }
    }

    private static void applyVariantData(Entity entity, ClientLevel world, CompoundTag variantNbt) {
        try {
            CompoundTag fullData = EntityNbtIoCompat.saveWithoutId(entity);
            fullData.merge(variantNbt.copy());
            EntityNbtIoCompat.load(entity, fullData, world.registryAccess());
        } catch (Throwable ignored) {
        }
    }

    private static int computeScale(LivingEntity entity, int width, int height) {
        float maxDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
        if (maxDimension < 0.15F) {
            maxDimension = 0.15F;
        }
        int minSide = Math.min(width, height);
        int target = (int) (minSide * 0.55F / maxDimension);
        return Mth.clamp(target, 4, 48);
    }
}
