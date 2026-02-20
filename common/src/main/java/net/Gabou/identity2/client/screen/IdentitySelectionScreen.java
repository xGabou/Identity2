package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.client.identity.IdentityVariantDiscovery;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariant;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class IdentitySelectionScreen extends Screen {
    private static final int ROWS_PER_PAGE = 10;
    private static final int ROW_HEIGHT = 20;

    private final List<IdentityEntry> allEntries = new ArrayList<>();
    private final List<IdentityEntry> filteredEntries = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private final Map<Identifier, List<IdentityVariant>> variantCache = new HashMap<>();
    private FilterMode filterMode = FilterMode.ALL;
    private EditBox searchField;
    private Button filterButton;
    private Button upButton;
    private Button downButton;
    private int scrollOffset = 0;
    private Identifier cachedWorldId = null;

    public IdentitySelectionScreen() {
        super(Component.literal("Identity Selection"));
    }

    @Override
    protected void init() {
        this.cachedWorldId = currentWorldId();
        this.variantCache.clear();
        buildEntries();

        int centerX = this.width / 2;
        int left = centerX - 110;
        int top = 30;

        this.searchField = this.addRenderableWidget(new EditBox(this.font, left, top, 220, 20, Component.literal("Search")));
        this.searchField.setHint(Component.literal("Search identity id"));
        this.searchField.setResponder(value -> refreshEntries(true));

        this.filterButton = this.addRenderableWidget(
            Button.builder(Component.literal("Filter: All"), button -> {
                this.filterMode = this.filterMode.next();
                refreshEntries(true);
            }).bounds(left, top + 24, 106, 20).build()
        );
        this.upButton = this.addRenderableWidget(
            Button.builder(Component.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshEntries(false);
                }
            }).bounds(left + 110, top + 24, 52, 20).build()
        );
        this.downButton = this.addRenderableWidget(
            Button.builder(Component.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshEntries(false);
                }
            }).bounds(left + 168, top + 24, 52, 20).build()
        );

        int listTop = top + 52;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            final int rowIndex = row;
            Button rowButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> selectRow(rowIndex)).bounds(left, listTop + row * ROW_HEIGHT, 220, 18).build()
            );
            this.rowButtons.add(rowButton);
        }

        this.addRenderableWidget(
            Button.builder(Component.literal("Return to Original"), button -> {
                Identity2Client.sendMorphRequest("");
                this.onClose();
            }).bounds(left, this.height - 52, 140, 20).build()
        );
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose()).bounds(left + 146, this.height - 52, 74, 20).build());

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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Identifier worldId = currentWorldId();
        if (this.cachedWorldId == null && worldId != null) {
            this.cachedWorldId = worldId;
        }
        if (this.cachedWorldId != null && worldId == null) {
            this.variantCache.clear();
            this.cachedWorldId = null;
        }
        if (this.cachedWorldId != null && worldId != null && !this.cachedWorldId.equals(worldId)) {
            this.variantCache.clear();
            this.cachedWorldId = worldId;
        }
        renderIdentityMenuBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawString(
            this.font,
            Component.literal("Unlocked: " + unlockedCount() + " / " + this.allEntries.size()),
            this.width / 2 - 110,
            this.height - 66,
            0xA0A0A0
        );
    }

    private void renderIdentityMenuBackground(GuiGraphics context) {
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

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            Identity2Client.sendMorphRequest(entry.id().toString());
            this.onClose();
            return;
        }

        List<IdentityVariant> variants = this.variantCache.computeIfAbsent(
            entry.id(),
            id -> IdentityVariantDiscovery.discover(BuiltInRegistries.ENTITY_TYPE.getValue(id), client.level)
        );
        if (variants.isEmpty()) {
            Identity2Client.sendMorphRequest(entry.id().toString());
            this.onClose();
            return;
        }

        if (variants.size() == 1) {
            IdentityVariant variant = variants.getFirst();
            Identity2Client.sendMorphRequest(entry.id().toString(), IdentityProgression.serializeVariantNbt(variant.variantNbt()));
            this.onClose();
            return;
        }

        client.setScreen(new IdentityVariantSelectionScreen(this, entry.id(), variants));
    }

    private void buildEntries() {
        this.allEntries.clear();
        Set<String> unlocked = readUnlockedIdentities();
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
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
        String query = this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);

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
            this.filterButton.setMessage(Component.literal("Filter: " + this.filterMode.label()));
        }

        for (int i = 0; i < this.rowButtons.size(); i++) {
            Button button = this.rowButtons.get(i);
            int index = this.scrollOffset + i;
            if (index >= 0 && index < this.filteredEntries.size()) {
                IdentityEntry entry = this.filteredEntries.get(index);
                boolean lockedForMorph = isLockedForMorph(entry);
                button.visible = true;
                button.active = !lockedForMorph;
                button.setMessage(Component.literal((entry.unlocked() ? "[Unlocked] " : "[Locked] ") + entry.id()));
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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return Set.of();
        }

        String csv = ((NbtComponentAccessor) (Object) ((EntityAccessor) client.player).getCustomData()).getNbt()
            .getStringOr(IdentityProgression.UNLOCKED_IDENTITIES_CACHE_KEY, "");
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

    @Override
    public void onClose() {
        this.variantCache.clear();
        super.onClose();
    }

    private Identifier currentWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        return client.level.dimension().identifier();
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
