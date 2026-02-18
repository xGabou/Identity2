package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class IdentitySelectionScreen extends Screen {
    private static final int ROWS_PER_PAGE = 10;
    private static final int ROW_HEIGHT = 20;

    private final List<IdentityEntry> allEntries = new ArrayList<>();
    private final List<IdentityEntry> filteredEntries = new ArrayList<>();
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private FilterMode filterMode = FilterMode.ALL;
    private TextFieldWidget searchField;
    private ButtonWidget filterButton;
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private int scrollOffset = 0;

    public IdentitySelectionScreen() {
        super(Text.literal("Identity Selection"));
    }

    @Override
    protected void init() {
        buildEntries();

        int centerX = this.width / 2;
        int left = centerX - 110;
        int top = 30;

        this.searchField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, left, top, 220, 20, Text.literal("Search")));
        this.searchField.setPlaceholder(Text.literal("Search identity id"));
        this.searchField.setChangedListener(value -> refreshEntries(true));

        this.filterButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Filter: All"), button -> {
                this.filterMode = this.filterMode.next();
                refreshEntries(true);
            }).dimensions(left, top + 24, 106, 20).build()
        );
        this.upButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshEntries(false);
                }
            }).dimensions(left + 110, top + 24, 52, 20).build()
        );
        this.downButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshEntries(false);
                }
            }).dimensions(left + 168, top + 24, 52, 20).build()
        );

        int listTop = top + 52;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            final int rowIndex = row;
            ButtonWidget rowButton = this.addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> selectRow(rowIndex)).dimensions(left, listTop + row * ROW_HEIGHT, 220, 18).build()
            );
            this.rowButtons.add(rowButton);
        }

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Return to Original"), button -> {
                Identity2Client.sendMorphRequest("");
                this.close();
            }).dimensions(left, this.height - 52, 140, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close()).dimensions(left + 146, this.height - 52, 74, 20).build());

        this.setInitialFocus(this.searchField);
        refreshEntries(true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            refreshEntries(false);
            return true;
        }
        if (verticalAmount < 0 && this.scrollOffset < maxScrollOffset()) {
            this.scrollOffset++;
            refreshEntries(false);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderIdentityMenuBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Unlocked: " + unlockedCount() + " / " + this.allEntries.size()),
            this.width / 2 - 110,
            this.height - 66,
            0xA0A0A0
        );
    }

    private void renderIdentityMenuBackground(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, 0xB0101010, 0xD0101010);
    }

    private void selectRow(int rowIndex) {
        int index = this.scrollOffset + rowIndex;
        if (index < 0 || index >= this.filteredEntries.size()) {
            return;
        }

        IdentityEntry entry = this.filteredEntries.get(index);
        if (isLockedForMorph(entry)) {
            return;
        }

        Identity2Client.sendMorphRequest(entry.id().toString());
        this.close();
    }

    private void buildEntries() {
        this.allEntries.clear();
        Set<String> unlocked = readUnlockedIdentities();
        for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
            if (!IdentityProgression.isMorphableIdentity(id)) {
                continue;
            }
            String text = id.toString();
            boolean isUnlocked = unlocked.contains(text);
            this.allEntries.add(new IdentityEntry(id, isUnlocked, text.toLowerCase(Locale.ROOT)));
        }
        this.allEntries.sort(Comparator.comparing(entry -> entry.id().toString()));
    }

    private void refreshEntries(boolean resetScroll) {
        this.filteredEntries.clear();
        String query = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);

        for (IdentityEntry entry : this.allEntries) {
            if (!query.isEmpty() && !entry.searchableId().contains(query)) {
                continue;
            }
            if (this.filterMode == FilterMode.UNLOCKED && !entry.unlocked()) {
                continue;
            }
            if (this.filterMode == FilterMode.LOCKED && entry.unlocked()) {
                continue;
            }
            this.filteredEntries.add(entry);
        }

        if (resetScroll) {
            this.scrollOffset = 0;
        } else {
            this.scrollOffset = Math.min(this.scrollOffset, maxScrollOffset());
        }

        if (this.filterButton != null) {
            this.filterButton.setMessage(Text.literal("Filter: " + this.filterMode.label()));
        }

        for (int i = 0; i < this.rowButtons.size(); i++) {
            ButtonWidget button = this.rowButtons.get(i);
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.filteredEntries.size()) {
                IdentityEntry entry = this.filteredEntries.get(index);
                boolean lockedForMorph = isLockedForMorph(entry);
                button.visible = true;
                button.active = !lockedForMorph;
                button.setMessage(Text.literal((entry.unlocked() ? "[Unlocked] " : "[Locked] ") + entry.id()));
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

    private int maxScrollOffset() {
        return Math.max(0, this.filteredEntries.size() - ROWS_PER_PAGE);
    }

    private int unlockedCount() {
        int count = 0;
        for (IdentityEntry entry : this.allEntries) {
            if (entry.unlocked()) {
                count++;
            }
        }
        return count;
    }

    private boolean isLockedForMorph(IdentityEntry entry) {
        if (!IdentitySettings.requireUnlockedIdentityForMorph) {
            return false;
        }
        return !entry.unlocked();
    }

    private static Set<String> readUnlockedIdentities() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Set.of();
        }

        String csv = ((NbtComponentAccessor) (Object) ((EntityAccessor) client.player).getCustomData()).getNbt()
            .getString(IdentityProgression.UNLOCKED_IDENTITIES_CACHE_KEY, "");
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        Set<String> unlocked = new HashSet<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                unlocked.add(trimmed);
            }
        }
        return unlocked;
    }

    private enum FilterMode {
        ALL("All"),
        UNLOCKED("Unlocked"),
        LOCKED("Locked");

        private final String label;

        FilterMode(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }

        public FilterMode next() {
            return switch (this) {
                case ALL -> UNLOCKED;
                case UNLOCKED -> LOCKED;
                case LOCKED -> ALL;
            };
        }
    }

    private record IdentityEntry(Identifier id, boolean unlocked, String searchableId) {
    }
}
