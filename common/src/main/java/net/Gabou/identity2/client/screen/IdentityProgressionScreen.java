package net.Gabou.identity2.client.screen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class IdentityProgressionScreen extends Screen {
    private static final String SOUL_JAR_ITEM_KEY = "identity2_soul_jar";
    private static final int SLOT_SIZE = 18;

    private EditBox jarIdField;
    private EditBox tierField;
    private EditBox identityField;
    private EditBox amountField;
    private EditBox targetField;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int dropZoneLeft;
    private int dropZoneTop;
    private int dropZoneRight;
    private int dropZoneBottom;
    private final List<InventorySlotView> inventorySlots = new ArrayList<>();
    private String selectedJarId = "";
    private String selectedJarTier = "";
    private String selectedJarName = "";
    private ItemStack draggedJarStack = ItemStack.EMPTY;
    private String lastAction = "";
    private String lastStatus = "";

    public IdentityProgressionScreen() {
        super(Component.literal("Identity Progression Manager"));
    }

    @Override
    protected void init() {
        this.panelWidth = Math.max(340, Math.min(620, this.width - 20));
        this.panelHeight = Math.max(280, Math.min(352, this.height - 14));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        int padding = 12;
        int contentLeft = this.panelLeft + padding;
        int contentTop = this.panelTop + 26;
        int contentWidth = this.panelWidth - padding * 2;
        int columnGap = 8;
        int columnWidth = Math.max(120, (contentWidth - columnGap) / 2);

        this.dropZoneLeft = contentLeft;
        this.dropZoneTop = contentTop;
        this.dropZoneRight = contentLeft + contentWidth;
        this.dropZoneBottom = contentTop + 24;

        int jarRow = this.dropZoneBottom + 8;
        this.jarIdField = createField(contentLeft, jarRow, columnWidth, "manual jar id (optional fallback)");
        this.tierField = createField(contentLeft + columnWidth + columnGap, jarRow, columnWidth, "tier (mud/glass/reinforced/true_soul)");
        this.identityField = createField(contentLeft, jarRow + 32, columnWidth, "identity id (example: minecraft:bee)");
        this.amountField = createField(contentLeft + columnWidth + columnGap, jarRow + 32, columnWidth, "amount");
        this.amountField.setValue("1");
        this.targetField = createField(contentLeft, jarRow + 64, contentWidth, "target player (optional)");

        int buttonTop = jarRow + 92;
        int buttonWidth = Math.max(120, (contentWidth - columnGap) / 2);
        int buttonHeight = 20;
        int rowGap = 4;

        addRenderableWidget(
            Button.builder(Component.literal("List Jars"), button -> runCommand("identity progression jar list", true))
                .bounds(contentLeft, buttonTop, buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Create Jar"), button -> createJar())
                .bounds(contentLeft + buttonWidth + columnGap, buttonTop, buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Upgrade Jar"), button -> upgradeJar())
                .bounds(contentLeft, buttonTop + (buttonHeight + rowGap), buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Store Morph"), button -> storeMorph())
                .bounds(contentLeft + buttonWidth + columnGap, buttonTop + (buttonHeight + rowGap), buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Remove Morph"), button -> removeMorph())
                .bounds(contentLeft, buttonTop + (buttonHeight + rowGap) * 2, buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Absorb Morph"), button -> absorbMorph())
                .bounds(contentLeft + buttonWidth + columnGap, buttonTop + (buttonHeight + rowGap) * 2, buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Charges Get"), button -> getCharges())
                .bounds(contentLeft, buttonTop + (buttonHeight + rowGap) * 3, buttonWidth, buttonHeight)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Charges Add"), button -> addCharges())
                .bounds(contentLeft + buttonWidth + columnGap, buttonTop + (buttonHeight + rowGap) * 3, buttonWidth, buttonHeight)
                .build()
        );

        int footerY = this.panelTop + this.panelHeight - 26;
        int closeWidth = 110;
        addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(this.panelLeft + this.panelWidth - closeWidth - 12, footerY, closeWidth, 20)
                .build()
        );

        buildInventorySlots();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            InventorySlotView slotView = findInventorySlot(mouseX, mouseY);
            if (slotView != null) {
                ItemStack stack = getInventoryStack(slotView.index());
                JarDescriptor descriptor = readJarDescriptor(stack);
                if (descriptor != null) {
                    this.draggedJarStack = stack.copyWithCount(1);
                    this.lastStatus = "Dragging jar: " + descriptor.jarId();
                    return true;
                }
                this.lastStatus = "Selected item is not a Soul Jar.";
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && !this.draggedJarStack.isEmpty()) {
            double mouseX = event.x();
            double mouseY = event.y();
            tryDropJarSelection(mouseX, mouseY);
            this.draggedJarStack = ItemStack.EMPTY;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderProgressionBackground(context);
        renderPanelSections(context);
        renderDropZone(context, mouseX, mouseY);
        renderInventoryStrip(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);

        int textX = this.panelLeft + 12;
        int labelTop = this.panelTop + 30;
        int columnSplit = textX + ((this.panelWidth - 24 - 8) / 2) + 8;

        context.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, 0xEAF7FF);
        context.drawString(this.font, Component.literal("Jar ID"), textX, labelTop + 30, 0xBED8E8);
        context.drawString(this.font, Component.literal("Tier"), columnSplit, labelTop + 30, 0xBED8E8);
        context.drawString(this.font, Component.literal("Identity ID"), textX, labelTop + 62, 0xBED8E8);
        context.drawString(this.font, Component.literal("Amount"), columnSplit, labelTop + 62, 0xBED8E8);
        context.drawString(this.font, Component.literal("Target (Optional)"), textX, labelTop + 94, 0xBED8E8);
        context.drawString(this.font, Component.literal("Drag a Soul Jar from inventory into selector box."), textX, this.panelTop + this.panelHeight - 52, 0x8FB0C2);
        if (!this.lastAction.isBlank()) {
            context.drawString(this.font, Component.literal("Last: " + this.lastAction), textX, this.panelTop + this.panelHeight - 40, 0xD7EAF5);
        }
        if (!this.lastStatus.isBlank()) {
            context.drawString(this.font, Component.literal(this.lastStatus), textX, this.panelTop + this.panelHeight - 28, 0x9BD3AE);
        }

        if (!this.draggedJarStack.isEmpty()) {
            context.renderItem(this.draggedJarStack, mouseX - 8, mouseY - 8);
        }
    }

    private void renderProgressionBackground(GuiGraphics context) {
        context.fillGradient(0, 0, this.width, this.height, 0xCC09131A, 0xE20A1821);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xDE172732);
        context.fillGradient(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 12, 0x805EC8A6, 0x105EC8A6);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 1, 0xFF64D5B3);
        context.fill(this.panelLeft, this.panelTop + this.panelHeight - 1, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + 1, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft + this.panelWidth - 1, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
    }

    private void renderPanelSections(GuiGraphics context) {
        int contentLeft = this.panelLeft + 10;
        int contentTop = this.panelTop + 24;
        int contentRight = this.panelLeft + this.panelWidth - 10;
        int contentBottom = this.panelTop + this.panelHeight - 34;
        context.fill(contentLeft - 2, contentTop - 2, contentRight + 2, contentBottom + 2, 0x66304B54);
        context.fill(contentLeft, contentTop, contentRight, contentBottom, 0x6E0F1B24);
    }

    private void renderDropZone(GuiGraphics context, int mouseX, int mouseY) {
        boolean hovered = isWithinDropZone(mouseX, mouseY);
        int border = hovered ? 0xFF76D5BD : 0xFF3E6570;
        int fill = hovered ? 0x7A1D3740 : 0x5A102029;
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneRight, this.dropZoneBottom, fill);
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneRight, this.dropZoneTop + 1, border);
        context.fill(this.dropZoneLeft, this.dropZoneBottom - 1, this.dropZoneRight, this.dropZoneBottom, border);
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneLeft + 1, this.dropZoneBottom, border);
        context.fill(this.dropZoneRight - 1, this.dropZoneTop, this.dropZoneRight, this.dropZoneBottom, border);

        String text = this.selectedJarId.isBlank()
            ? "Drop Soul Jar Here (from inventory)"
            : ("Selected: " + this.selectedJarId + " [" + this.selectedJarTier + "]");
        context.drawCenteredString(this.font, Component.literal(text), (this.dropZoneLeft + this.dropZoneRight) / 2, this.dropZoneTop + 8, 0xDDF2FA);
    }

    private void renderInventoryStrip(GuiGraphics context, int mouseX, int mouseY) {
        for (InventorySlotView slot : this.inventorySlots) {
            int left = slot.x();
            int top = slot.y();
            int right = left + SLOT_SIZE;
            int bottom = top + SLOT_SIZE;
            boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
            int border = hovered ? 0xFF8ADCC7 : 0xFF4B6C76;
            context.fill(left, top, right, bottom, 0x70101D26);
            context.fill(left, top, right, top + 1, border);
            context.fill(left, bottom - 1, right, bottom, border);
            context.fill(left, top, left + 1, bottom, border);
            context.fill(right - 1, top, right, bottom, border);

            ItemStack stack = getInventoryStack(slot.index());
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            context.renderItem(stack, left + 1, top + 1);
        }
    }

    private void buildInventorySlots() {
        this.inventorySlots.clear();
        int inventoryWidth = SLOT_SIZE * 9;
        int left = this.panelLeft + (this.panelWidth - inventoryWidth) / 2;
        int top = this.panelTop + this.panelHeight - 104;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                this.inventorySlots.add(new InventorySlotView(index, left + col * SLOT_SIZE, top + row * SLOT_SIZE));
            }
        }
        int hotbarTop = top + SLOT_SIZE * 3 + 4;
        for (int col = 0; col < 9; col++) {
            this.inventorySlots.add(new InventorySlotView(col, left + col * SLOT_SIZE, hotbarTop));
        }
    }

    private InventorySlotView findInventorySlot(double mouseX, double mouseY) {
        for (InventorySlotView slot : this.inventorySlots) {
            if (
                mouseX >= slot.x() && mouseX < slot.x() + SLOT_SIZE &&
                mouseY >= slot.y() && mouseY < slot.y() + SLOT_SIZE
            ) {
                return slot;
            }
        }
        return null;
    }

    private ItemStack getInventoryStack(int index) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = client.player.getInventory();
        if (index < 0 || index >= inventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return inventory.getItem(index);
    }

    private void tryDropJarSelection(double mouseX, double mouseY) {
        if (!isWithinDropZone(mouseX, mouseY)) {
            this.lastStatus = "Drop the jar inside the selector box.";
            return;
        }
        JarDescriptor descriptor = readJarDescriptor(this.draggedJarStack);
        if (descriptor == null) {
            this.lastStatus = "That item is not a valid Soul Jar.";
            return;
        }
        this.selectedJarId = descriptor.jarId();
        this.selectedJarTier = descriptor.tier();
        this.selectedJarName = descriptor.displayName();
        if (this.jarIdField != null) {
            this.jarIdField.setValue(this.selectedJarId);
        }
        if (this.tierField != null && this.tierField.getValue().isBlank()) {
            this.tierField.setValue(this.selectedJarTier);
        }
        this.lastStatus = "Selected jar: " + this.selectedJarId + " (" + this.selectedJarName + ")";
    }

    private boolean isWithinDropZone(double mouseX, double mouseY) {
        return mouseX >= this.dropZoneLeft && mouseX < this.dropZoneRight && mouseY >= this.dropZoneTop && mouseY < this.dropZoneBottom;
    }

    private JarDescriptor readJarDescriptor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        CompoundTag jarTag = root.getCompound(SOUL_JAR_ITEM_KEY).orElse(null);
        if (jarTag == null || jarTag.isEmpty()) {
            return null;
        }
        String jarId = jarTag.getStringOr("jar_id", "").trim().toLowerCase(Locale.ROOT);
        if (jarId.isBlank()) {
            return null;
        }
        String tier = jarTag.getStringOr("tier", "mud").trim().toLowerCase(Locale.ROOT);
        String displayName = stack.getHoverName().getString();
        return new JarDescriptor(jarId, tier.isBlank() ? "mud" : tier, displayName);
    }

    private EditBox createField(int x, int y, int width, String hint) {
        EditBox field = this.addRenderableWidget(new EditBox(this.font, x, y, width, 20, Component.empty()));
        field.setHint(Component.literal(hint));
        return field;
    }

    private void createJar() {
        String tier = required(this.tierField, "Tier is required.");
        if (tier == null) {
            return;
        }

        String jarId = this.jarIdField == null ? "" : this.jarIdField.getValue().trim();
        if (jarId.isBlank()) {
            jarId = "jar_" + (System.currentTimeMillis() % 100000L);
            if (this.jarIdField != null) {
                this.jarIdField.setValue(jarId);
            }
            this.lastStatus = "Auto-generated jar id: " + jarId;
        }

        runCommand("identity progression jar create " + jarId + " " + tier, true);
    }

    private void upgradeJar() {
        String jarId = selectedOrRequiredJarId();
        String tier = required(this.tierField, "Tier is required.");
        if (jarId == null || tier == null) {
            return;
        }
        runCommand("identity progression jar upgrade " + jarId + " " + tier, true);
    }

    private void storeMorph() {
        String jarId = selectedOrRequiredJarId();
        String identityId = required(this.identityField, "Identity ID is required.");
        if (jarId == null || identityId == null) {
            return;
        }
        runCommand("identity progression jar store " + jarId + " " + identityId, true);
    }

    private void removeMorph() {
        String jarId = selectedOrRequiredJarId();
        String identityId = required(this.identityField, "Identity ID is required.");
        if (jarId == null || identityId == null) {
            return;
        }
        runCommand("identity progression jar remove " + jarId + " " + identityId, true);
    }

    private void absorbMorph() {
        String jarId = selectedOrRequiredJarId();
        String identityId = required(this.identityField, "Identity ID is required.");
        if (jarId == null || identityId == null) {
            return;
        }
        runCommand("identity progression jar absorb " + jarId + " " + identityId, true);
    }

    private void getCharges() {
        String identityId = required(this.identityField, "Identity ID is required.");
        if (identityId == null) {
            return;
        }
        runCommand("identity progression charges get " + identityId, true);
    }

    private void addCharges() {
        String identityId = required(this.identityField, "Identity ID is required.");
        String amount = required(this.amountField, "Amount is required.");
        if (identityId == null || amount == null) {
            return;
        }
        runCommand("identity progression charges add " + identityId + " " + amount, true);
    }

    private String selectedOrRequiredJarId() {
        if (!this.selectedJarId.isBlank()) {
            return this.selectedJarId;
        }
        return required(this.jarIdField, "Drop/select a jar first, or enter jar ID.");
    }

    private String required(EditBox field, String missingMessage) {
        if (field == null) {
            this.lastStatus = missingMessage;
            return null;
        }
        String value = field.getValue().trim();
        if (value.isBlank()) {
            this.lastStatus = missingMessage;
            return null;
        }
        return value;
    }

    private void runCommand(String baseCommand, boolean appendTarget) {
        String command = baseCommand == null ? "" : baseCommand.trim();
        if (command.isBlank()) {
            this.lastStatus = "Command is empty.";
            return;
        }

        if (appendTarget && this.targetField != null) {
            String target = this.targetField.getValue().trim();
            if (!target.isBlank()) {
                command = command + " " + target;
            }
        }

        this.lastAction = "/" + command;
        if (sendCommand(command)) {
            this.lastStatus = "Sent command to server.";
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(new ChatScreen("/" + command, false));
            this.lastStatus = "Fallback: command prefilled in chat.";
        } else {
            this.lastStatus = "Failed to send command.";
        }
    }

    private boolean sendCommand(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        Object connection = client.getConnection();
        if (connection == null) {
            return false;
        }

        if (invokeStringMethod(connection, "sendCommand", command)) {
            return true;
        }
        if (invokeStringMethod(connection, "sendUnsignedCommand", command)) {
            return true;
        }
        return invokeStringMethod(connection, "sendChat", "/" + command);
    }

    private boolean invokeStringMethod(Object target, String methodName, String value) {
        if (target == null || methodName == null || value == null) {
            return false;
        }
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0] != String.class) {
                    continue;
                }
                method.invoke(target, value);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private record InventorySlotView(int index, int x, int y) {
    }

    private record JarDescriptor(String jarId, String tier, String displayName) {
    }
}
