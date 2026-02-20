package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class IdentityVariantSelectionScreen extends Screen {
    private static final int ROWS_PER_PAGE = 10;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final Identifier entityTypeId;
    private final List<IdentityVariant> variants;
    private final List<Button> rowButtons = new ArrayList<>();
    private Button upButton;
    private Button downButton;
    private int scrollOffset = 0;

    public IdentityVariantSelectionScreen(Screen parent, Identifier entityTypeId, List<IdentityVariant> variants) {
        super(Component.literal("Identity Variants"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
        this.variants = variants == null ? List.of() : new ArrayList<>(variants);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int left = centerX - 110;
        int top = 30;

        this.upButton = this.addRenderableWidget(
            Button.builder(Component.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshRows();
                }
            }).bounds(left + 110, top, 52, 20).build()
        );
        this.downButton = this.addRenderableWidget(
            Button.builder(Component.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshRows();
                }
            }).bounds(left + 168, top, 52, 20).build()
        );

        int listTop = top + 28;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            final int rowIndex = row;
            Button rowButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> selectRow(rowIndex)).bounds(left, listTop + row * ROW_HEIGHT, 220, 18).build()
            );
            this.rowButtons.add(rowButton);
        }

        this.addRenderableWidget(
            Button.builder(Component.literal("Back"), button -> this.minecraft.setScreen(this.parent)).bounds(left, this.height - 52, 106, 20).build()
        );
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose()).bounds(left + 114, this.height - 52, 106, 20).build());

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
        context.fillGradient(0, 0, this.width, this.height, 0xB0101010, 0xD0101010);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, Component.literal("Variants: " + this.entityTypeId), this.width / 2, 12, 0xFFFFFF);
    }

    private void refreshRows() {
        for (int i = 0; i < this.rowButtons.size(); i++) {
            Button button = this.rowButtons.get(i);
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.variants.size()) {
                IdentityVariant variant = this.variants.get(index);
                button.visible = true;
                button.active = true;
                button.setMessage(Component.literal(variant.displayName()));
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

    private int maxScrollOffset() {
        return Math.max(0, this.variants.size() - ROWS_PER_PAGE);
    }
}

