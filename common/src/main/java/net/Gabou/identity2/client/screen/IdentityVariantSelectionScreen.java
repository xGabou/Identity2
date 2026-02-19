package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class IdentityVariantSelectionScreen extends Screen {
    private static final int ROWS_PER_PAGE = 10;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final Identifier entityTypeId;
    private final List<IdentityVariant> variants;
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private int scrollOffset = 0;

    public IdentityVariantSelectionScreen(Screen parent, Identifier entityTypeId, List<IdentityVariant> variants) {
        super(Text.literal("Identity Variants"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
        this.variants = variants == null ? List.of() : new ArrayList<>(variants);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int left = centerX - 110;
        int top = 30;

        this.upButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshRows();
                }
            }).dimensions(left + 110, top, 52, 20).build()
        );
        this.downButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshRows();
                }
            }).dimensions(left + 168, top, 52, 20).build()
        );

        int listTop = top + 28;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            final int rowIndex = row;
            ButtonWidget rowButton = this.addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> selectRow(rowIndex)).dimensions(left, listTop + row * ROW_HEIGHT, 220, 18).build()
            );
            this.rowButtons.add(rowButton);
        }

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(this.parent)).dimensions(left, this.height - 52, 106, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close()).dimensions(left + 114, this.height - 52, 106, 20).build());

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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xB0101010, 0xD0101010);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Variants: " + this.entityTypeId), this.width / 2, 12, 0xFFFFFF);
    }

    private void refreshRows() {
        for (int i = 0; i < this.rowButtons.size(); i++) {
            ButtonWidget button = this.rowButtons.get(i);
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.variants.size()) {
                IdentityVariant variant = this.variants.get(index);
                button.visible = true;
                button.active = true;
                button.setMessage(Text.literal(variant.displayName()));
            } else {
                button.visible = false;
                button.active = false;
                button.setMessage(Text.empty());
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
        this.close();
    }

    private int maxScrollOffset() {
        return Math.max(0, this.variants.size() - ROWS_PER_PAGE);
    }
}

