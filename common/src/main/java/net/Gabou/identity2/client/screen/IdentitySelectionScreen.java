package net.Gabou.identity2.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class IdentitySelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int MIN_ROWS = 6;
    private static final int MAX_ROWS = 14;

    private final List<IdentityEntry> allEntries = new ArrayList<>();
    private final List<IdentityEntry> filteredEntries = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private final Map<Identifier, List<IdentityVariant>> variantCache = new HashMap<>();
    private final Map<Identifier, LivingEntity> previewEntityCache = new HashMap<>();
    private Set<String> unlockedIdentityIds = Set.of();
    private Map<String, Set<String>> unlockedVariantTokens = Map.of();
    private FilterMode filterMode = FilterMode.ALL;
    private EditBox searchField;
    private Button filterButton;
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

    public IdentitySelectionScreen() {
        super(Component.literal("Identity Selection"));
    }

    @Override
    protected void init() {
        this.cachedWorldId = currentWorldId();
        this.variantCache.clear();
        clearPreviewEntities();
        buildEntries();

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

        int searchLeft = this.listLeft;
        int searchTop = this.panelTop + 22;
        int searchWidth = this.listWidth;

        this.searchField = this.addRenderableWidget(new EditBox(this.font, searchLeft, searchTop, searchWidth, 20, Component.literal("Search")));
        this.searchField.setHint(Component.literal("Search identity id"));
        this.searchField.setResponder(value -> refreshEntries(true));

        int controlsTop = searchTop + 24;
        this.filterButton = this.addRenderableWidget(
            Button.builder(Component.literal("Filter: All"), button -> {
                this.filterMode = this.filterMode.next();
                refreshEntries(true);
            }).bounds(searchLeft, controlsTop, 120, 20).build()
        );
        this.upButton = this.addRenderableWidget(
            Button.builder(Component.literal("Up"), button -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    refreshEntries(false);
                }
            }).bounds(searchLeft + 126, controlsTop, 56, 20).build()
        );
        this.downButton = this.addRenderableWidget(
            Button.builder(Component.literal("Down"), button -> {
                if (this.scrollOffset < maxScrollOffset()) {
                    this.scrollOffset++;
                    refreshEntries(false);
                }
            }).bounds(searchLeft + 188, controlsTop, 60, 20).build()
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
                    .bounds(searchLeft, this.listTop + row * ROW_HEIGHT, this.listWidth, ROW_HEIGHT - 2)
                    .build()
            );
            this.rowButtons.add(rowButton);
        }

        int footerButtonWidth = (this.panelWidth - 24 - 8) / 2;
        this.addRenderableWidget(
            Button.builder(Component.literal("Return to Original"), button -> {
                Identity2Client.sendMorphRequest("");
                this.onClose();
            }).bounds(searchLeft, footerY, footerButtonWidth, 20).build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(searchLeft + footerButtonWidth + 8, footerY, footerButtonWidth, 20)
                .build()
        );

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
            clearPreviewEntities();
            this.cachedWorldId = worldId;
        }

        renderIdentityMenuBackground(context);
        renderPanelSections(context);
        super.render(context, mouseX, mouseY, delta);
        renderRowEntityPreviews(context, delta);
        renderPreviewPane(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, 0xF3FBF8);
        context.drawString(
            this.font,
            Component.literal("Unlocked: " + unlockedCount() + " / " + this.allEntries.size()),
            this.listLeft,
            this.panelTop + this.panelHeight - 40,
            0xBCD4D9
        );
        context.drawString(
            this.font,
            Component.literal(
                "Fav1: " + Identity2Client.getFavoriteLabel(0)
                    + "  Fav2: " + Identity2Client.getFavoriteLabel(1)
                    + "  Fav3: " + Identity2Client.getFavoriteLabel(2)
            ),
            this.listLeft,
            this.panelTop + this.panelHeight - 52,
            0x92B6BE
        );
        context.drawString(
            this.font,
            Component.literal("Showing: " + this.filteredEntries.size()),
            this.previewLeft + 10,
            this.panelTop + this.panelHeight - 40,
            0x9EC4C9
        );
    }

    private void renderIdentityMenuBackground(GuiGraphics context) {
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
            if (index < 0 || index >= this.filteredEntries.size()) {
                continue;
            }

            IdentityEntry entry = this.filteredEntries.get(index);
            LivingEntity preview = resolvePreviewEntity(entry.id());
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
        IdentityEntry focused = hoveredEntry(mouseX, mouseY);
        if (focused == null) {
            focused = firstVisibleEntry();
        }

        int textX = this.previewLeft + 10;
        int textY = this.previewTop + 8;
        context.drawString(this.font, Component.literal("Preview"), textX, textY, 0xD3F9EB);
        if (focused == null) {
            context.drawString(this.font, Component.literal("No identities found"), textX, textY + 14, 0xA6C3C8);
            return;
        }

        context.drawString(this.font, Component.literal(focused.displayName()), textX, textY + 14, 0xEDF7F7);
        context.drawString(this.font, Component.literal(focused.id().toString()), textX, textY + 26, 0x9CB6BB);
        context.drawString(
            this.font,
            Component.literal(focused.unlocked() ? "Unlocked" : "Locked"),
            textX,
            textY + 38,
            focused.unlocked() ? 0x72D9AD : 0xD68E8E
        );

        int boxLeft = this.previewLeft + 8;
        int boxTop = this.previewTop + 52;
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

        LivingEntity previewEntity = resolvePreviewEntity(focused.id());
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

    private IdentityEntry hoveredEntry(int mouseX, int mouseY) {
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
            if (index >= 0 && index < this.filteredEntries.size()) {
                return this.filteredEntries.get(index);
            }
        }
        return null;
    }

    private IdentityEntry firstVisibleEntry() {
        if (this.filteredEntries.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(this.scrollOffset, 0, this.filteredEntries.size() - 1);
        return this.filteredEntries.get(index);
    }

    private LivingEntity resolvePreviewEntity(Identifier id) {
        Minecraft client = Minecraft.getInstance();
        LivingEntity existing = this.previewEntityCache.get(id);
        if (existing != null && existing.level() == client.level) {
            return existing;
        }
        if (existing != null) {
            IdentityMenuRenderHelper.disposeEntity(existing);
            this.previewEntityCache.remove(id);
        }

        LivingEntity created = IdentityMenuRenderHelper.buildPreviewEntity(id, null);
        if (created != null) {
            this.previewEntityCache.put(id, created);
        }
        return created;
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
            id -> discoverVariants(id, client)
        );
        if (variants.isEmpty()) {
            Identity2Client.sendMorphRequest(entry.id().toString());
            this.onClose();
            return;
        }

        if (variants.size() == 1) {
            IdentityVariant variant = variants.getFirst();
            if (isVariantLockedForMorph(entry.id(), variant)) {
                return;
            }
            Identity2Client.sendMorphRequest(entry.id().toString(), IdentityProgression.serializeVariantNbt(variant.variantNbt()));
            this.onClose();
            return;
        }

        Set<String> unlockedTokensForType = this.unlockedVariantTokens.get(entry.id().toString());
        boolean wildcardUnlocked = !IdentitySettings.requireUnlockedIdentityForMorph || unlockedTokensForType == null;
        client.setScreen(
            new IdentityVariantSelectionScreen(
                this,
                entry.id(),
                variants,
                unlockedTokensForType == null ? Set.of() : unlockedTokensForType,
                wildcardUnlocked,
                IdentitySettings.requireUnlockedIdentityForMorph
            )
        );
    }

    private List<IdentityVariant> discoverVariants(Identifier id, Minecraft client) {
        if (IdentityProgression.PLAYER_IDENTITY_ID.equals(id)) {
            return discoverUnlockedPlayerSkinVariants();
        }
        return IdentityVariantDiscovery.discover(BuiltInRegistries.ENTITY_TYPE.getValue(id), client.level);
    }

    private List<IdentityVariant> discoverUnlockedPlayerSkinVariants() {
        Set<String> tokens = this.unlockedVariantTokens.get(IdentityProgression.PLAYER_IDENTITY_ID.toString());
        if (tokens == null || tokens.isEmpty()) {
            return List.of(new IdentityVariant(IdentityProgression.PLAYER_IDENTITY_ID, "Current Player Skin", new CompoundTag()));
        }

        List<IdentityVariant> variants = new ArrayList<>();
        for (String token : tokens) {
            CompoundTag variantNbt = IdentityProgression.fromVariantUnlockToken(token);
            String name = variantNbt.getStringOr(IdentityProgression.PLAYER_SKIN_NAME_VARIANT_KEY, "").trim();
            String uuid = variantNbt.getStringOr(IdentityProgression.PLAYER_SKIN_UUID_VARIANT_KEY, "").trim();
            String display = !name.isEmpty() ? "Skin: " + name : (!uuid.isEmpty() ? "Skin: " + uuid : "Player Skin");
            variants.add(new IdentityVariant(IdentityProgression.PLAYER_IDENTITY_ID, display, variantNbt));
        }
        variants.sort(Comparator.comparing(IdentityVariant::displayName));
        return variants;
    }

    private void buildEntries() {
        this.allEntries.clear();
        this.unlockedIdentityIds = readUnlockedIdentities();
        this.unlockedVariantTokens = readUnlockedVariantTokens();
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!IdentityProgression.isMorphableIdentity(id)) {
                continue;
            }
            String text = id.toString();
            boolean isUnlocked = this.unlockedIdentityIds.contains(text);
            this.allEntries.add(new IdentityEntry(id, isUnlocked, text.toLowerCase(Locale.ROOT), formatDisplayName(id)));
        }
        this.allEntries.sort(Comparator.comparing(IdentityEntry::displayName));
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
                button.setMessage(Component.literal(formatEntryLabel(entry)));
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
        return Math.max(0, this.filteredEntries.size() - this.rowsPerPage);
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

    private boolean isVariantLockedForMorph(Identifier identityId, IdentityVariant variant) {
        if (!IdentitySettings.requireUnlockedIdentityForMorph) {
            return false;
        }
        if (!this.unlockedIdentityIds.contains(identityId.toString())) {
            return true;
        }
        Set<String> tokens = this.unlockedVariantTokens.get(identityId.toString());
        if (tokens == null) {
            return false;
        }
        return !tokens.contains(IdentityProgression.toVariantUnlockToken(variant.variantNbt()));
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

    private static Map<String, Set<String>> readUnlockedVariantTokens() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return Map.of();
        }

        String serialized = ((NbtComponentAccessor) (Object) ((EntityAccessor) client.player).getCustomData()).getNbt()
            .getStringOr(IdentityProgression.UNLOCKED_IDENTITY_VARIANTS_CACHE_KEY, "");
        if (serialized == null || serialized.isBlank()) {
            return Map.of();
        }

        Map<String, Set<String>> result = new HashMap<>();
        for (String entry : serialized.split(",")) {
            String trimmed = entry == null ? "" : entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex >= trimmed.length() - 1) {
                continue;
            }

            String identityId = trimmed.substring(0, equalsIndex).trim();
            String tokenData = trimmed.substring(equalsIndex + 1).trim();
            if (identityId.isEmpty() || tokenData.isEmpty()) {
                continue;
            }

            Set<String> tokens = new HashSet<>();
            for (String token : tokenData.split("\\|")) {
                String normalized = token == null ? "" : token.trim();
                if (!normalized.isEmpty()) {
                    tokens.add(normalized);
                }
            }
            if (!tokens.isEmpty()) {
                result.put(identityId, tokens);
            }
        }
        return result;
    }

    @Override
    public void onClose() {
        clearPreviewEntities();
        this.variantCache.clear();
        super.onClose();
    }

    @Override
    public void removed() {
        clearPreviewEntities();
        super.removed();
    }

    private Identifier currentWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        return client.level.dimension().identifier();
    }

    private static String formatEntryLabel(IdentityEntry entry) {
        String prefix = entry.unlocked() ? "[U] " : "[L] ";
        return "      " + prefix + entry.displayName();
    }

    private static String formatDisplayName(Identifier id) {
        String path = id.getPath();
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String rawPart : parts) {
            if (rawPart == null || rawPart.isBlank()) {
                continue;
            }
            String part = rawPart.toLowerCase(Locale.ROOT);
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        if (builder.isEmpty()) {
            builder.append(id.toString());
        }
        if (!"minecraft".equals(id.getNamespace())) {
            builder.append(" [").append(id.getNamespace()).append("]");
        }
        return builder.toString();
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

    private record IdentityEntry(Identifier id, boolean unlocked, String searchableId, String displayName) {
    }
}
