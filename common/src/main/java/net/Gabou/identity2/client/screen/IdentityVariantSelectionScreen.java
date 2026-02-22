package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class IdentityVariantSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int MIN_ROWS = 6;
    private static final int MAX_ROWS = 14;

    private final Screen parent;
    private final Identifier entityTypeId;
    private final List<IdentityVariant> variants;
    private final List<Button> rowButtons = new ArrayList<>();
    private final Map<String, LivingEntity> previewEntityCache = new HashMap<>();
    private Button upButton;
    private Button downButton;
    private int scrollOffset = 0;
    private int rowsPerPage = 10;
    private Identifier cachedWorldId = null;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int listLeft;
    private int listWidth;
    private int listTop;
    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;

    public IdentityVariantSelectionScreen(Screen parent, Identifier entityTypeId, List<IdentityVariant> variants) {
        super(Component.literal("Identity Variants"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
        this.variants = variants == null ? List.of() : new ArrayList<>(variants);
    }

    @Override
    protected void init() {
        this.cachedWorldId = currentWorldId();
        clearPreviewEntities();

        this.panelWidth = Math.max(320, Math.min(this.width - 18, 760));
        this.panelHeight = Math.max(280, Math.min(this.height - 18, 460));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        int contentWidth = this.panelWidth - 24;
        int maxListWidth = Math.max(140, contentWidth - 140);
        this.listWidth = Mth.clamp((int) (contentWidth * 0.58F), 140, maxListWidth);
        this.previewWidth = contentWidth - this.listWidth - 10;
        this.listLeft = this.panelLeft + 12;
        this.previewLeft = this.listLeft + this.listWidth + 10;

        int controlsLeft = this.listLeft;
        int controlsTop = this.panelTop + 22;
        this.upButton = this.addRenderableWidget(
            Button.builder(Component.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshRows();
                }
            }).bounds(controlsLeft, controlsTop, 56, 20).build()
        );
        this.downButton = this.addRenderableWidget(
            Button.builder(Component.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshRows();
                }
            }).bounds(controlsLeft + 62, controlsTop, 56, 20).build()
        );

        int footerY = this.panelTop + this.panelHeight - 28;
        this.listTop = controlsTop + 28;
        int listBottom = footerY - 8;
        this.previewTop = this.listTop;
        this.previewHeight = listBottom - this.listTop;
        int listHeight = Math.max(ROW_HEIGHT * MIN_ROWS, listBottom - this.listTop);
        this.rowsPerPage = Math.max(MIN_ROWS, Math.min(MAX_ROWS, listHeight / ROW_HEIGHT));

        for (int row = 0; row < this.rowsPerPage; row++) {
            final int rowIndex = row;
            Button rowButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> selectRow(rowIndex))
                    .bounds(controlsLeft, this.listTop + row * ROW_HEIGHT, this.listWidth, ROW_HEIGHT - 2)
                    .build()
            );
            this.rowButtons.add(rowButton);
        }

        int footerButtonWidth = (this.panelWidth - 24 - 8) / 2;
        this.addRenderableWidget(
            Button.builder(Component.literal("Back"), button -> {
                clearPreviewEntities();
                this.minecraft.setScreen(this.parent);
            })
                .bounds(controlsLeft, footerY, footerButtonWidth, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(controlsLeft + footerButtonWidth + 8, footerY, footerButtonWidth, 20)
                .build()
        );

        refreshRows();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            refreshRows();
            return true;
        }
        if (verticalAmount < 0 && this.scrollOffset < maxScrollOffset()) {
            this.scrollOffset++;
            refreshRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Identifier worldId = currentWorldId();
        if (this.cachedWorldId == null && worldId != null) {
            this.cachedWorldId = worldId;
        }
        if (this.cachedWorldId != null && worldId == null) {
            clearPreviewEntities();
            this.cachedWorldId = null;
        }
        if (this.cachedWorldId != null && worldId != null && !this.cachedWorldId.equals(worldId)) {
            clearPreviewEntities();
            this.cachedWorldId = worldId;
        }

        renderBackground(context);
        renderPanelSections(context);
        super.render(context, mouseX, mouseY, delta);
        renderRowEntityPreviews(context, delta);
        renderPreviewPane(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, Component.literal("Variants: " + this.entityTypeId), this.width / 2, this.panelTop + 8, 0xF3FBF8);
        context.drawString(this.font, Component.literal("Showing: " + this.variants.size()), this.previewLeft + 10, this.panelTop + this.panelHeight - 40, 0x9EC4C9);
    }

    private void renderBackground(GuiGraphics context) {
        context.fillGradient(0, 0, this.width, this.height, 0xCC09131A, 0xE20A1821);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xDE172732);
        context.fillGradient(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 12, 0x805EC8A6, 0x105EC8A6);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 1, 0xFF64D5B3);
        context.fill(this.panelLeft, this.panelTop + this.panelHeight - 1, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + 1, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft + this.panelWidth - 1, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
    }

    private void renderPanelSections(GuiGraphics context) {
        context.fill(this.listLeft - 2, this.listTop - 2, this.listLeft + this.listWidth + 2, this.listTop + this.previewHeight + 2, 0x66203342);
        context.fill(this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listTop + this.previewHeight, 0x6E12202A);
        context.fill(this.previewLeft - 2, this.previewTop - 2, this.previewLeft + this.previewWidth + 2, this.previewTop + this.previewHeight + 2, 0x66304B54);
        context.fill(this.previewLeft, this.previewTop, this.previewLeft + this.previewWidth, this.previewTop + this.previewHeight, 0x6E0F1B24);
    }

    private void renderRowEntityPreviews(GuiGraphics context, float delta) {
        float tick = IdentityMenuRenderHelper.resolveIdleTick(delta);
        for (int i = 0; i < this.rowButtons.size(); i++) {
            Button button = this.rowButtons.get(i);
            if (!button.visible) {
                continue;
            }
            int index = this.scrollOffset + i;
            if (index < 0 || index >= this.variants.size()) {
                continue;
            }

            IdentityVariant variant = this.variants.get(index);
            LivingEntity preview = resolvePreviewEntity(variant);
            if (preview == null) {
                continue;
            }

            int iconLeft = button.getX() + 3;
            int iconTop = button.getY() + 1;
            int iconRight = iconLeft + 20;
            int iconBottom = button.getY() + button.getHeight() - 1;
            int iconMouseX = (iconLeft + iconRight) / 2 + (int) (Mth.sin((tick + i * 5) * 0.08F) * 5.0F);
            int iconMouseY = (iconTop + iconBottom) / 2;
            IdentityMenuRenderHelper.renderEntityInBox(context, iconLeft, iconTop, iconRight, iconBottom, iconMouseX, iconMouseY, tick, preview);
        }
    }

    private void renderPreviewPane(GuiGraphics context, int mouseX, int mouseY, float delta) {
        IdentityVariant focused = hoveredVariant(mouseX, mouseY);
        if (focused == null) {
            focused = firstVisibleVariant();
        }

        int textX = this.previewLeft + 10;
        int textY = this.previewTop + 8;
        context.drawString(this.font, Component.literal("Variant Preview"), textX, textY, 0xD3F9EB);
        if (focused == null) {
            context.drawString(this.font, Component.literal("No variants available"), textX, textY + 14, 0xA6C3C8);
            return;
        }

        context.drawString(this.font, Component.literal(focused.displayName()), textX, textY + 14, 0xEDF7F7);
        context.drawString(
            this.font,
            Component.literal(focused.variantNbt().isEmpty() ? "Default data" : "Custom variant data"),
            textX,
            textY + 26,
            0x9CB6BB
        );

        int boxLeft = this.previewLeft + 8;
        int boxTop = this.previewTop + 44;
        int boxRight = this.previewLeft + this.previewWidth - 8;
        int boxBottom = this.previewTop + this.previewHeight - 10;
        context.fill(boxLeft, boxTop, boxRight, boxBottom, 0x6813212C);
        context.fill(boxLeft, boxTop, boxRight, boxTop + 1, 0xFF36515A);
        context.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, 0xFF20343D);

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            context.drawCenteredString(this.font, Component.literal("Enter a world to render preview"), (boxLeft + boxRight) / 2, boxTop + 12, 0x9AB7BC);
            return;
        }

        LivingEntity previewEntity = resolvePreviewEntity(focused);
        if (previewEntity == null) {
            context.drawCenteredString(this.font, Component.literal("Preview unavailable"), (boxLeft + boxRight) / 2, boxTop + 12, 0xCDA6A6);
            return;
        }

        IdentityMenuRenderHelper.renderEntityInBox(
            context,
            boxLeft,
            boxTop,
            boxRight,
            boxBottom,
            mouseX,
            mouseY,
            IdentityMenuRenderHelper.resolveIdleTick(delta),
            previewEntity
        );
    }

    private IdentityVariant hoveredVariant(int mouseX, int mouseY) {
        for (int i = 0; i < this.rowButtons.size(); i++) {
            Button button = this.rowButtons.get(i);
            if (!button.visible) {
                continue;
            }
            if (mouseX < button.getX() || mouseX >= button.getX() + button.getWidth()) {
                continue;
            }
            if (mouseY < button.getY() || mouseY >= button.getY() + button.getHeight()) {
                continue;
            }
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.variants.size()) {
                return this.variants.get(index);
            }
        }
        return null;
    }

    private IdentityVariant firstVisibleVariant() {
        if (this.variants.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(this.scrollOffset, 0, this.variants.size() - 1);
        return this.variants.get(index);
    }

    private LivingEntity resolvePreviewEntity(IdentityVariant variant) {
        String key = variantCacheKey(variant);
        LivingEntity existing = this.previewEntityCache.get(key);
        Minecraft client = Minecraft.getInstance();
        if (existing != null && existing.level() == client.level) {
            return existing;
        }
        if (existing != null) {
            IdentityMenuRenderHelper.disposeEntity(existing);
            this.previewEntityCache.remove(key);
        }

        LivingEntity created = IdentityMenuRenderHelper.buildPreviewEntity(this.entityTypeId, variant.variantNbt());
        if (created != null) {
            this.previewEntityCache.put(key, created);
        }
        return created;
    }

    private String variantCacheKey(IdentityVariant variant) {
        return this.entityTypeId + "|" + variant.displayName() + "|" + IdentityProgression.serializeVariantNbt(variant.variantNbt());
    }

    private void clearPreviewEntities() {
        if (this.previewEntityCache.isEmpty()) {
            return;
        }
        for (LivingEntity entity : this.previewEntityCache.values()) {
            IdentityMenuRenderHelper.disposeEntity(entity);
        }
        this.previewEntityCache.clear();
    }

    private void refreshRows() {
        for (int i = 0; i < this.rowButtons.size(); i++) {
            Button button = this.rowButtons.get(i);
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.variants.size()) {
                IdentityVariant variant = this.variants.get(index);
                button.visible = true;
                button.active = true;
                button.setMessage(Component.literal("      " + variant.displayName()));
            } else {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.empty());
            }
        }
        if (this.upButton != null) {
            this.upButton.active = this.scrollOffset > 0;
        }
        if (this.downButton != null) {
            this.downButton.active = this.scrollOffset < maxScrollOffset();
        }
    }

    private void selectRow(int rowIndex) {
        int index = this.scrollOffset + rowIndex;
        if (index < 0 || index >= this.variants.size()) {
            return;
        }

        IdentityVariant variant = this.variants.get(index);
        String variantData = IdentityProgression.serializeVariantNbt(variant.variantNbt());
        Identity2Client.sendMorphRequest(this.entityTypeId.toString(), variantData);
        this.onClose();
    }

    private Identifier currentWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        return client.level.dimension().identifier();
    }

    private int maxScrollOffset() {
        return Math.max(0, this.variants.size() - this.rowsPerPage);
    }

    @Override
    public void onClose() {
        clearPreviewEntities();
        super.onClose();
    }

    @Override
    public void removed() {
        clearPreviewEntities();
        super.removed();
    }
}
