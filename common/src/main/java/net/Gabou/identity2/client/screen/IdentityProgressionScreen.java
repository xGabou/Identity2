package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.packets.ProgressionJarStateS2CPacketPayload;
import net.Gabou.identity2.packets.ProgressionPlayerChargesS2CPacketPayload;
import net.Gabou.identity2.progression.ProgressionChargeCodec;
import net.Gabou.identity2.progression.SoulJarChargeStorage;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class IdentityProgressionScreen extends Screen {
    private static final int SLOT_SIZE = 18;
    private static final int LIST_ROW_HEIGHT = 18;

    private final Map<String, Integer> playerCharges = new HashMap<>();
    private final Map<String, Integer> jarCharges = new HashMap<>();
    private final List<String> chargeEntries = new ArrayList<>();
    private final List<InventorySlotView> inventorySlots = new ArrayList<>();

    private EditBox amountField;
    private Button upButton;
    private Button downButton;
    private Button depositButton;
    private Button withdrawButton;
    private Button clearJarButton;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int rowsPerPage;
    private int dropZoneLeft;
    private int dropZoneTop;
    private int dropZoneRight;
    private int dropZoneBottom;

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int selectedJarSlot = -1;
    private int draggedJarSlot = -1;
    private ItemStack draggedJarStack = ItemStack.EMPTY;
    private String selectedJarId = "";
    private String selectedJarTier = "";
    private boolean pendingRequest = false;
    private String statusText = "Loading charges...";

    public IdentityProgressionScreen() {
        super(Component.literal("Morph Charges & Soul Jar"));
    }

    @Override
    protected void init() {
        this.panelWidth = Math.max(360, Math.min(620, this.width - 20));
        this.panelHeight = Math.max(300, Math.min(360, this.height - 14));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        int padding = 12;
        int contentLeft = this.panelLeft + padding;
        int contentTop = this.panelTop + 24;
        int contentWidth = this.panelWidth - padding * 2;

        this.listLeft = contentLeft;
        this.listTop = contentTop + 36;
        this.listWidth = Math.max(160, contentWidth / 2);
        this.listHeight = 154;
        this.rowsPerPage = Math.max(4, this.listHeight / LIST_ROW_HEIGHT);

        this.dropZoneLeft = this.listLeft + this.listWidth + 10;
        this.dropZoneTop = contentTop + 2;
        this.dropZoneRight = contentLeft + contentWidth;
        this.dropZoneBottom = this.dropZoneTop + 28;

        this.amountField = this.addRenderableWidget(new EditBox(this.font, this.dropZoneLeft, this.dropZoneBottom + 10, 92, 20, Component.empty()));
        this.amountField.setHint(Component.literal("amount"));
        this.amountField.setValue("1");
        this.amountField.setResponder(value -> refreshButtons());

        int controlY = this.listTop + this.listHeight + 4;
        this.upButton = this.addRenderableWidget(
            Button.builder(Component.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshButtons();
                }
            }).bounds(this.listLeft, controlY, 54, 20).build()
        );
        this.downButton = this.addRenderableWidget(
            Button.builder(Component.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshButtons();
                }
            }).bounds(this.listLeft + 58, controlY, 54, 20).build()
        );

        this.depositButton = this.addRenderableWidget(
            Button.builder(Component.literal("Deposit -> Jar"), button -> requestTransfer(true))
                .bounds(this.dropZoneLeft, this.amountField.getY() + 28, 132, 20)
                .build()
        );
        this.withdrawButton = this.addRenderableWidget(
            Button.builder(Component.literal("<- Withdraw"), button -> requestTransfer(false))
                .bounds(this.dropZoneLeft, this.amountField.getY() + 52, 132, 20)
                .build()
        );
        this.clearJarButton = this.addRenderableWidget(
            Button.builder(Component.literal("Remove Jar"), button -> clearSelectedJar())
                .bounds(this.dropZoneLeft, this.amountField.getY() + 76, 132, 20)
                .build()
        );

        int footerY = this.panelTop + this.panelHeight - 26;
        this.addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(this.panelLeft + this.panelWidth - 96, footerY, 84, 20)
                .build()
        );

        buildInventorySlots();
        rebuildChargeEntries();
        refreshButtons();
        Identity2Client.requestProgressionChargeSync();
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (i == 0) {
            InventorySlotView slot = findInventorySlot(d, e);
            if (slot != null) {
                ItemStack stack = getInventoryStack(slot.index());
                if (SoulJarChargeStorage.isPotentialSoulJarItem(stack)) {
                    this.draggedJarSlot = slot.index();
                    this.draggedJarStack = stack.copyWithCount(1);
                    this.statusText = "Dragging jar from slot " + this.draggedJarSlot + ".";
                    // Quick-select on click as a fallback when drag fails in some input stacks.
                    this.pendingRequest = true;
                    Identity2Client.sendProgressionJarSelect(this.draggedJarSlot);
                    refreshButtons();
                    return true;
                }
                this.statusText = "Selected item is not a Soul Jar.";
                return true;
            }

            int clickedRow = rowAt(d, e);
            if (clickedRow >= 0) {
                int entryIndex = this.scrollOffset + clickedRow;
                if (entryIndex >= 0 && entryIndex < this.chargeEntries.size()) {
                    this.selectedIndex = entryIndex;
                    refreshButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked( d,  e,  i);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.draggedJarStack.isEmpty()) {
            boolean alreadyRequested = this.pendingRequest;
            if (isWithinDropZone(mouseX, mouseY)) {
                this.pendingRequest = true;
                this.statusText = "Selecting jar...";
                Identity2Client.sendProgressionJarSelect(this.draggedJarSlot);
            } else if (!alreadyRequested) {
                this.statusText = "Drop jar inside selector box.";
            }
            this.draggedJarSlot = -1;
            this.draggedJarStack = ItemStack.EMPTY;
            refreshButtons();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double scrollAmount) {
        if (!isWithinList(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, scrollAmount);
        }
        if (scrollAmount > 0.0D && this.scrollOffset > 0) {
            this.scrollOffset--;
            refreshButtons();
            return true;
        }
        if (scrollAmount < 0.0D && this.scrollOffset < maxScrollOffset()) {
            this.scrollOffset++;
            refreshButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, scrollAmount);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackgroundPanel(context);
        renderDropZone(context, mouseX, mouseY);
        renderChargeList(context, mouseX, mouseY);
        renderInventoryStrip(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);

        int textX = this.panelLeft + 12;
        context.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, 0xFFEAF7FF);
        context.drawString(this.font, Component.literal("Morph Charges"), this.listLeft, this.listTop - 12, 0xFFCDE7F4);
        context.drawString(this.font, Component.literal("Amount"), this.dropZoneLeft, this.amountField.getY() - 10, 0xFFCDE7F4);
        renderStatusBlock(context);

        if (!this.draggedJarStack.isEmpty()) {
            context.renderItem(this.draggedJarStack, mouseX - 8, mouseY - 8);
        }

        renderJarHoverPreview(context, mouseX, mouseY);
    }

    private void renderBackgroundPanel(GuiGraphics context) {
        context.fillGradient(0, 0, this.width, this.height, 0xCC09131A, 0xE20A1821);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xDE172732);
        context.fillGradient(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 12, 0x805EC8A6, 0x105EC8A6);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 1, 0xFF64D5B3);
        context.fill(this.panelLeft, this.panelTop + this.panelHeight - 1, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft, this.panelTop, this.panelLeft + 1, this.panelTop + this.panelHeight, 0xFF293D4A);
        context.fill(this.panelLeft + this.panelWidth - 1, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xFF293D4A);
    }

    private void renderDropZone(GuiGraphics context, int mouseX, int mouseY) {
        boolean hover = isWithinDropZone(mouseX, mouseY);
        int border = hover ? 0xFF76D5BD : 0xFF3E6570;
        int fill = hover ? 0x7A1D3740 : 0x5A102029;
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneRight, this.dropZoneBottom, fill);
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneRight, this.dropZoneTop + 1, border);
        context.fill(this.dropZoneLeft, this.dropZoneBottom - 1, this.dropZoneRight, this.dropZoneBottom, border);
        context.fill(this.dropZoneLeft, this.dropZoneTop, this.dropZoneLeft + 1, this.dropZoneBottom, border);
        context.fill(this.dropZoneRight - 1, this.dropZoneTop, this.dropZoneRight, this.dropZoneBottom, border);
        String label = this.selectedJarSlot >= 0
            ? ("Jar: " + this.selectedJarId + " [" + this.selectedJarTier + "]")
            : "Drag Soul Jar Here";
        context.drawCenteredString(this.font, Component.literal(label), (this.dropZoneLeft + this.dropZoneRight) / 2, this.dropZoneTop + 9, 0xFFDDF2FA);
    }

    private void renderChargeList(GuiGraphics context, int mouseX, int mouseY) {
        int borderLeft = this.listLeft - 2;
        int borderTop = this.listTop - 2;
        int borderRight = this.listLeft + this.listWidth + 2;
        int borderBottom = this.listTop + this.listHeight + 2;
        context.fill(borderLeft, borderTop, borderRight, borderBottom, 0x66304B54);
        context.fill(this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listTop + this.listHeight, 0x6E0F1B24);

        int maxRows = Math.min(this.rowsPerPage, this.chargeEntries.size() - this.scrollOffset);
        for (int row = 0; row < maxRows; row++) {
            int index = this.scrollOffset + row;
            if (index < 0 || index >= this.chargeEntries.size()) {
                continue;
            }
            String identityId = this.chargeEntries.get(index);
            int y = this.listTop + row * LIST_ROW_HEIGHT;
            int rowBottom = y + LIST_ROW_HEIGHT - 1;
            boolean selected = index == this.selectedIndex;
            boolean hovered = isWithinRow(mouseX, mouseY, row);
            int rowColor = selected ? 0x99446374 : (hovered ? 0x66354E5B : 0x33000000);
            context.fill(this.listLeft + 1, y + 1, this.listLeft + this.listWidth - 1, rowBottom, rowColor);

            int playerCount = Math.max(0, this.playerCharges.getOrDefault(identityId, 0));
            int jarCount = Math.max(0, this.jarCharges.getOrDefault(identityId, 0));
            String display = shortenIdentity(identityId) + "  P:" + playerCount + "  J:" + jarCount;
            int color = playerCount <= 0 ? 0xFFB89999 : 0xFFDDEDF4;
            context.drawString(this.font, Component.literal(display), this.listLeft + 6, y + 5, color);
        }
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
            if (!stack.isEmpty()) {
                context.renderItem(stack, left + 1, top + 1);
            }
        }
    }

    private void renderStatusBlock(GuiGraphics context) {
        int inventoryTop = getInventoryTop();
        int statusX = this.dropZoneLeft;
        int statusY = this.clearJarButton.getY() + 24;
        int statusMaxY = inventoryTop - 8;
        int lineHeight = 12;
        int maxLines = Math.max(0, (statusMaxY - statusY) / lineHeight);
        if (maxLines <= 0) {
            return;
        }

        String selectedMorph = selectedIdentityId();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Selected Jar: " + (this.selectedJarSlot >= 0 ? (truncateForRightPanel(this.selectedJarId) + " (slot " + this.selectedJarSlot + ")") : "none")));
        lines.add(Component.literal("Selected Morph: " + (selectedMorph == null ? "none" : truncateForRightPanel(selectedMorph))));
        lines.add(Component.literal("Status: " + truncateForRightPanel(this.statusText)));
        lines.add(Component.literal("P = player charges, J = jar charges"));

        int rendered = Math.min(maxLines, lines.size());
        for (int i = 0; i < rendered; i++) {
            int color = switch (i) {
                case 0, 1 -> 0xFF95B8CC;
                case 2 -> 0xFF9BD3AE;
                default -> 0xFF8FB0C2;
            };
            context.drawString(this.font, lines.get(i), statusX, statusY + i * lineHeight, color);
        }
    }

    private void renderJarHoverPreview(GuiGraphics context, int mouseX, int mouseY) {
        InventorySlotView hovered = findInventorySlot(mouseX, mouseY);
        if (hovered == null) {
            return;
        }
        ItemStack stack = getInventoryStack(hovered.index());
        if (!SoulJarChargeStorage.isPotentialSoulJarItem(stack)) {
            return;
        }

        SoulJarChargeStorage.JarSnapshot snapshot = SoulJarChargeStorage.read(stack);
        List<String> lines = new ArrayList<>();
        if (snapshot == null) {
            lines.add("Soul Jar");
            lines.add("Not initialized yet");
        } else {
            lines.add("Jar: " + snapshot.jarId() + " [" + snapshot.tier() + "]");
            if (snapshot.charges().isEmpty()) {
                lines.add("No stored charges");
            } else {
                snapshot.charges().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(6)
                    .forEach(entry -> lines.add(shortenIdentity(entry.getKey()) + ": " + entry.getValue()));
            }
        }

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        int boxWidth = width + 8;
        int boxHeight = lines.size() * 12 + 6;
        int x = mouseX + 12;
        int y = mouseY - 10;
        if (x + boxWidth > this.width - 4) {
            x = mouseX - boxWidth - 12;
        }
        if (y + boxHeight > this.height - 4) {
            y = this.height - boxHeight - 4;
        }
        if (y < 4) {
            y = 4;
        }

        context.fill(x, y, x + boxWidth, y + boxHeight, 0xE0101B24);
        context.fill(x, y, x + boxWidth, y + 1, 0xFF6AAFA0);
        context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0xFF36515A);
        context.fill(x, y, x + 1, y + boxHeight, 0xFF36515A);
        context.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, 0xFF36515A);
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFEAF7FF : 0xFFD6E8EE;
            context.drawString(this.font, Component.literal(lines.get(i)), x + 4, y + 3 + i * 12, color);
        }
    }

    private void requestTransfer(boolean deposit) {
        if (this.pendingRequest || this.selectedJarSlot < 0) {
            return;
        }
        String selectedMorph = selectedIdentityId();
        if (selectedMorph == null) {
            this.statusText = "Select a morph entry first.";
            return;
        }
        int amount = parseTransferAmount();
        if (amount <= 0) {
            this.statusText = "Amount must be a positive number.";
            return;
        }
        this.pendingRequest = true;
        this.statusText = deposit ? "Depositing..." : "Withdrawing...";
        Identity2Client.sendProgressionJarTransfer(this.selectedJarSlot, selectedMorph, amount, deposit);
        refreshButtons();
    }

    private void clearSelectedJar() {
        this.pendingRequest = true;
        this.statusText = "Removing selected jar...";
        Identity2Client.sendProgressionJarSelect(-1);
        refreshButtons();
    }

    private int parseTransferAmount() {
        if (this.amountField == null) {
            return 0;
        }
        String raw = this.amountField.getValue().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void refreshButtons() {
        String selectedMorph = selectedIdentityId();
        int playerCount = selectedMorph == null ? 0 : Math.max(0, this.playerCharges.getOrDefault(selectedMorph, 0));
        int jarCount = selectedMorph == null ? 0 : Math.max(0, this.jarCharges.getOrDefault(selectedMorph, 0));
        int amount = parseTransferAmount();
        boolean hasJar = this.selectedJarSlot >= 0;
        boolean canTransfer = !this.pendingRequest && hasJar && selectedMorph != null && amount > 0;
        this.depositButton.active = canTransfer && playerCount >= amount;
        this.withdrawButton.active = canTransfer && jarCount >= amount;
        this.clearJarButton.active = !this.pendingRequest && hasJar;
        this.upButton.active = this.scrollOffset > 0;
        this.downButton.active = this.scrollOffset < maxScrollOffset();

        if (this.pendingRequest) {
            return;
        }
        if (!hasJar) {
            this.statusText = "Select a Soul Jar first.";
            return;
        }
        if (selectedMorph == null) {
            this.statusText = "Select a morph entry from the list.";
            return;
        }
        if (amount <= 0) {
            this.statusText = "Enter an amount greater than 0.";
            return;
        }
        boolean canDeposit = playerCount >= amount;
        boolean canWithdraw = jarCount >= amount;
        if (canDeposit && canWithdraw) {
            this.statusText = "Ready: Deposit or Withdraw.";
            return;
        }
        if (canDeposit) {
            this.statusText = "Ready: Deposit. Jar does not have enough for withdraw.";
            return;
        }
        if (canWithdraw) {
            this.statusText = "Ready: Withdraw. Player does not have enough for deposit.";
            return;
        }
        this.statusText = "Not enough charges in player or jar for this amount.";
    }

    private String selectedIdentityId() {
        if (this.chargeEntries.isEmpty()) {
            return null;
        }
        int safeIndex = Mth.clamp(this.selectedIndex, 0, this.chargeEntries.size() - 1);
        this.selectedIndex = safeIndex;
        return this.chargeEntries.get(safeIndex);
    }

    private void rebuildChargeEntries() {
        Set<String> merged = new HashSet<>();
        merged.addAll(readUnlockedIdentityIds());
        merged.addAll(this.playerCharges.keySet());
        merged.addAll(this.jarCharges.keySet());
        this.chargeEntries.clear();
        this.chargeEntries.addAll(merged.stream().filter(id -> id != null && !id.isBlank()).sorted(Comparator.naturalOrder()).toList());
        if (this.chargeEntries.isEmpty()) {
            this.selectedIndex = 0;
            this.scrollOffset = 0;
            return;
        }
        this.selectedIndex = Mth.clamp(this.selectedIndex, 0, this.chargeEntries.size() - 1);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScrollOffset());
    }

    private Set<String> readUnlockedIdentityIds() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return Set.of();
        }
        return IdentityProgression.readUnlockedIdentityIdSet(
            ((NbtComponentAccessor) ((EntityAccessor) client.player).getCustomData()).getNbt()
        );
    }

    private int rowAt(double mouseX, double mouseY) {
        if (!isWithinList(mouseX, mouseY)) {
            return -1;
        }
        int row = (int) ((mouseY - this.listTop) / LIST_ROW_HEIGHT);
        if (row < 0 || row >= this.rowsPerPage) {
            return -1;
        }
        return row;
    }

    private boolean isWithinList(double mouseX, double mouseY) {
        return mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth && mouseY >= this.listTop && mouseY < this.listTop + this.listHeight;
    }

    private boolean isWithinDropZone(double mouseX, double mouseY) {
        return mouseX >= this.dropZoneLeft && mouseX < this.dropZoneRight && mouseY >= this.dropZoneTop && mouseY < this.dropZoneBottom;
    }

    private boolean isWithinRow(int mouseX, int mouseY, int row) {
        int y = this.listTop + row * LIST_ROW_HEIGHT;
        return mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT;
    }

    private int maxScrollOffset() {
        return Math.max(0, this.chargeEntries.size() - this.rowsPerPage);
    }

    private int getInventoryTop() {
        int min = Integer.MAX_VALUE;
        for (InventorySlotView slot : this.inventorySlots) {
            min = Math.min(min, slot.y());
        }
        if (min == Integer.MAX_VALUE) {
            return this.panelTop + this.panelHeight - 104;
        }
        return min;
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
            if (mouseX >= slot.x() && mouseX < slot.x() + SLOT_SIZE && mouseY >= slot.y() && mouseY < slot.y() + SLOT_SIZE) {
                return slot;
            }
        }
        return null;
    }

    private ItemStack getInventoryStack(int slotIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = client.player.getInventory();
        if (slotIndex < 0 || slotIndex >= inventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return inventory.getItem(slotIndex);
    }

    private String shortenIdentity(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        if (id.length() <= 24) {
            return id;
        }
        return id.substring(0, 24) + "...";
    }

    private String truncateForRightPanel(String value) {
        if (value == null) {
            return "";
        }
        int maxChars = 36;
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }

    public static void onPlayerChargeSync(ProgressionPlayerChargesS2CPacketPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof IdentityProgressionScreen screen) {
            screen.applyPlayerChargeSync(payload.serializedCharges());
        }
    }

    public static void onJarStateSync(ProgressionJarStateS2CPacketPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof IdentityProgressionScreen screen) {
            screen.applyJarStateSync(payload);
        }
    }

    private void applyPlayerChargeSync(String serializedCharges) {
        this.playerCharges.clear();
        this.playerCharges.putAll(ProgressionChargeCodec.deserialize(serializedCharges));
        rebuildChargeEntries();
        refreshButtons();
    }

    private void applyJarStateSync(ProgressionJarStateS2CPacketPayload payload) {
        this.pendingRequest = false;
        this.selectedJarSlot = payload.slotIndex();
        this.selectedJarId = payload.jarId() == null ? "" : payload.jarId();
        this.selectedJarTier = payload.jarTier() == null ? "" : payload.jarTier();
        this.playerCharges.clear();
        this.playerCharges.putAll(ProgressionChargeCodec.deserialize(payload.serializedPlayerCharges()));
        this.jarCharges.clear();
        this.jarCharges.putAll(ProgressionChargeCodec.deserialize(payload.serializedJarCharges()));
        this.statusText = payload.message() == null || payload.message().isBlank() ? "Updated." : payload.message();
        rebuildChargeEntries();
        refreshButtons();
    }

    private record InventorySlotView(int index, int x, int y) {
    }
}
