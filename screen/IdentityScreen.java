package draylar.identity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.identity.Identity;
import draylar.identity.api.PlayerFavorites;
import draylar.identity.api.PlayerIdentity;
import draylar.identity.api.PlayerUnlocks;
import draylar.identity.api.variant.IdentityType;
import draylar.identity.mixin.accessor.ScreenAccessor;
import draylar.identity.screen.widget.EntityWidget;
import draylar.identity.screen.widget.HelpWidget;
import draylar.identity.screen.widget.PlayerWidget;
import draylar.identity.screen.widget.SearchWidget;
import net.Gabou.gaboulibs.util.CompatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdentityScreen extends Screen {

    // == State ==
    private final List<IdentityType<?>>                   unlocked        = new ArrayList<>();
    private final Map<IdentityType<?>, LivingEntity>     renderEntities  = new LinkedHashMap<>();
    private final List<EntityWidget>                     entityWidgets   = new ArrayList<>();

    // Saved villager display names keyed by synthetic IdentityType entries
    private final Map<IdentityType<?>, String>            villagerNames   = new LinkedHashMap<>();


    // header widgets
    private SearchWidget   searchBar;
    private PlayerWidget   playerButton;
    private ButtonWidget   helpButton;

    private String lastSearch = "";
    private int    scrollY    = 0;

    public IdentityScreen() {
        super(Text.literal(""));
    }
    public double getScaleFactor(){
        assert client != null;
        return client.getWindow().getScaleFactor();
    }

    public int getScrollY(){
        return scrollY;
    }


    @Override
    protected void init() {
        super.init();
        // 🔥 Fix here: clear old stuff
        ((ScreenAccessor) this).getSelectables().removeIf(w -> w instanceof EntityWidget);
        children().removeIf(w -> w instanceof EntityWidget);
        entityWidgets.clear();
        scrollY = 0;

        // instantiate header widgets
        searchBar    = createSearchBar();
        playerButton = createPlayerButton();
        helpButton   = createHelpButton();

        addDrawableChild(searchBar);
        addDrawableChild(playerButton);
        addDrawableChild(helpButton);

        ClientPlayerEntity player = client.player;
        if (player == null) {
            client.setScreen(null);
            return;
        }

        // preload entities

        loadEntities(client);




        // filter + sort unlocked
        unlocked.addAll(renderEntities.keySet().stream()
                .filter(t -> PlayerUnlocks.has(player, t) || player.isCreative())
                .collect(Collectors.toList()));
        unlocked.sort((a, b) -> PlayerFavorites.has(player, a) ? -1 : 1);

        populateEntities(player, unlocked);

        searchBar.setChangedListener(text -> {
            setFocused(searchBar);
            if (!lastSearch.equals(text)) {
                ((ScreenAccessor) this).getSelectables().removeIf(w -> w instanceof EntityWidget);
                children().removeIf(w -> w instanceof EntityWidget);
                entityWidgets.clear();

                String q = text.toLowerCase();
                List<IdentityType<?>> filtered = unlocked.stream()
                        .filter(t -> q.isEmpty()
                                || t.getEntityType().getTranslationKey().toLowerCase().contains(q)
                                || (isVillagerEntry(t) && villagerNames.getOrDefault(t, "").toLowerCase().contains(q)))
                        .collect(Collectors.toList());

                populateEntities(player, filtered);
                lastSearch = text;
                scrollY    = 0;
            }
        });
    }
    private void loadEntities(MinecraftClient client) {
        for (IdentityType<?> type : IdentityType.getAllTypes(client.world)) {
            // Check by type before instantiating
            if (CompatUtils.isBlacklistedEntityType(type.getEntityType().toString())) {
                continue;
            }

            try {
                LivingEntity e = (LivingEntity) type.create(client.world);
                renderEntities.put(type, e);
            } catch (Exception e) {
                CompatUtils.markIncompatibleEntityType(type.getEntityType().toString());
                Identity.LOGGER.warn("Failed to create identity " + type.getEntityType().getTranslationKey(), e);
            }
        }

        // Append saved Villager identities as separate entries with unique variants
        try {
            if (client.player != null) {
                draylar.identity.impl.PlayerDataProvider data = (draylar.identity.impl.PlayerDataProvider) client.player;
                java.util.List<String> keys = new java.util.ArrayList<>(data.getVillagerIdentities().keySet());
                java.util.Collections.sort(keys);
                int i = 0;
                for (String key : keys) {
                    net.minecraft.nbt.NbtCompound tag = data.getVillagerIdentities().get(key);
                    net.minecraft.nbt.NbtCompound copy = tag.copy();
                    // Ensure id present for loadEntity
                    copy.putString("id", net.minecraft.registry.Registries.ENTITY_TYPE.getId(net.minecraft.entity.EntityType.VILLAGER).toString());
                    net.minecraft.entity.Entity loaded = net.minecraft.entity.EntityType.loadEntityWithPassengers(copy, client.world, it -> it);
                    if (loaded instanceof LivingEntity living) {
                        // Use a special variant domain to differentiate saved villager entries
                        IdentityType<VillagerEntity> idType =
                                new IdentityType<>(EntityType.VILLAGER, 1_000_000 + i);
                        // Remember display name for search; prefer IdentityName tag, fallback to key
                        String display = tag.contains("IdentityName") ? tag.getString("IdentityName") : key;
                        villagerNames.put(idType, display == null ? key : display);
                        // Improve tooltip by setting custom entity display name
                        if (display != null && !display.isEmpty()) {
                            living.setCustomName(net.minecraft.text.Text.literal(display));
                        }
                        renderEntities.put(idType, living);
                        i++;
                    }
                }
                // also expose a generic Villager identity if none exists and user has at least one saved villager
                if (!keys.isEmpty()) {
                    IdentityType<VillagerEntity> baseVillager = new IdentityType<>(EntityType.VILLAGER);
                    if (!renderEntities.containsKey(baseVillager)) {
                        try {
                            LivingEntity e = (LivingEntity) baseVillager.create(client.world);
                            renderEntities.put(baseVillager, e);
                        } catch (Throwable ignoredToo) { }
                    }
                }
            }
        } catch (Throwable ignored) { }
    }
    private boolean isVillagerEntry(IdentityType<?> t) {
        return t.getEntityType() == EntityType.VILLAGER && t.getVariantData() >= 1_000_000;
    }


    private void populateEntities(ClientPlayerEntity player, List<IdentityType<?>> list) {
        final int perRow   = 7;
        final int marginX  = 15;
        final int startY = 0;
        Window win         = client.getWindow();
        float cellW        = (win.getScaledWidth() - marginX * 2f) / perRow;
        float cellH        = win.getScaledHeight() / 5f;

        IdentityType<LivingEntity> current = IdentityType.from(PlayerIdentity.getIdentity(player));

        List<IdentityType<?>> invalid = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            IdentityType<?> type = list.get(i);
            int xIdx = i % perRow, yIdx = i / perRow;
            int x = marginX + Math.round(cellW * xIdx);
            int y = startY  + Math.round(cellH * yIdx);

            boolean isCurr = current != null && current.equals(type);
            boolean fav    = PlayerFavorites.has(player, type);

            try {
                EntityWidget widget = new EntityWidget(
                        x, y,
                        Math.round(cellW), Math.round(cellH),
                        type,
                        renderEntities.get(type),
                        this,
                        fav,
                        isCurr
                );

                addDrawableChild(widget);
                entityWidgets.add(widget);
            } catch (Exception e) {
                Identity.LOGGER.warn("Failed to add identity " + type.getEntityType().getTranslationKey(), e);
                invalid.add(type);
            }
        }

        if (!invalid.isEmpty()) {
            unlocked.removeAll(invalid);
            renderEntities.keySet().removeAll(invalid);
        }
    }

    public int getHeaderHeight() {
        return (int)(searchBar.getY() + searchBar.getHeight() + 5);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Use texture background to avoid fullscreen blur affecting UI text
        ctx.fill(0, 0, width, height, 0xB0000000);

        renderEntityGrid(ctx, mx, my, delta);

        if (unlocked.isEmpty()) {
            String hint = Text.translatable("identity.menu_hint").getString();
            int w = client.textRenderer.getWidth(hint);
            int x = (client.getWindow().getWidth() - w) / 2;
            int y = client.getWindow().getHeight() / 2;
            ctx.drawText(client.textRenderer, hint, x, y, 0xFFFFFF, true);
        }

        // header on top
        searchBar.render(ctx, mx, my, delta);
        playerButton.render(ctx, mx, my, delta);
        helpButton.render(ctx, mx, my, delta);
    }


   protected void renderEntityGrid(DrawContext ctx, int mx, int my, float delta) {
       double sf       = client.getWindow().getScaleFactor();
       int    headerH  = getHeaderHeight();
       int    viewH    = this.height - headerH;
       int    scrollTop= scrollY;
       int    scrollBot= scrollY + viewH;
       // 1) Clip below the header
       RenderSystem.enableScissor(
               0,
               (int)(headerH * sf),
               (int)(width   * sf),
               (int)(viewH   * sf)
       );

       // 2) Push & translate into scroll‑space
       ctx.getMatrices().push();
       ctx.getMatrices().translate(0, headerH - scrollY, 0);

       // 3) Draw only visible widgets
       for(EntityWidget w : entityWidgets) {
           int wy = w.getY(), wh = w.getHeight();
           if(wy + wh < scrollTop || wy > scrollBot) continue;
           w.render(ctx, mx, my + scrollY - headerH, delta);
       }

       ctx.getMatrices().pop();
       RenderSystem.disableScissor();

       // 4) **Draw tooltip** at the **raw** mouse coords
       //    (we test against the **adjusted** Y, but render at the real Y)
       for(EntityWidget w : entityWidgets) {
           if(w.isMouseOver(mx, my + scrollY - headerH)) {
               ctx.drawTooltip(
                       client.textRenderer,
                       w.getHoverName(),
                       mx, my
               );
               break;
           }
       }
}


    // somewhere in your IdentityScreen (you already have getGuiScale() and getScaleFactor()):
    public double getEffectiveGuiScale() {
        int raw = client.options.getGuiScale().getValue();
        // 0 means “Auto” → use the window’s computed scaleFactor
        return raw == 0
                ? client.getWindow().getScaleFactor()
                : raw;
    }







    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        if (entityWidgets.isEmpty()) return false;

        int rowH   = entityWidgets.get(0).getHeight();
        int rows   = (int)Math.ceil(unlocked.size() / 7f);
        int totalH = rows * rowH;
        int viewH  = this.height - getHeaderHeight();
        // allow 10px of “empty” space at the bottom
        int bottomPadding = 10;
        int maxY = Math.max(0, totalH - viewH + bottomPadding);

        // Use vertical scroll delta instead of old "amount"
        scrollY = Math.max(0, Math.min(scrollY - (int)(vert * rowH), maxY));
        return true;
    }


    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);

        clearChildren();
        scrollY = 0;

        ClientPlayerEntity player = client.player;
        if (player != null) {
            // FULL FIX HERE:
            renderEntities.clear(); // 🔥 Clear old render entities!
            villagerNames.clear();  // clear cached villager names
            loadEntities(client);

            unlocked.clear();
            unlocked.addAll(renderEntities.keySet().stream()
                    .filter(t -> PlayerUnlocks.has(player, t) || player.isCreative())
                    .collect(Collectors.toList()));
            unlocked.sort((a, b) -> PlayerFavorites.has(player, a) ? -1 : 1);

            populateEntities(player, unlocked);
        }
    }




    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (entityWidgets.isEmpty()) return false;
        int hh = getHeaderHeight();
        // if we clicked below the header, first try our scrolled widgets:
        if (my >= hh) {
            double adjY = my + scrollY - hh;
            for (EntityWidget<?> w : entityWidgets) {
                if (w.mouseClicked(mx, adjY, button)) {
                    return true;
                }
            }
        }

        // otherwise fall back to header buttons or default
        if (my < hh) {
            return searchBar.mouseClicked(mx, my, button)
                    || playerButton.mouseClicked(mx, my, button)
                    || helpButton.mouseClicked(mx, my, button);
        }
        return super.mouseClicked(mx, my, button);
    }


    @Override
    public void close() {
        entityWidgets.forEach(
                EntityWidget::dispose);
        super.close();
    }

    @Override
    public void clearChildren() {
        // only remove our EntityWidgets, keep other important buttons
        ((ScreenAccessor) this).getSelectables().removeIf(w -> w instanceof EntityWidget);
        children().removeIf(w -> w instanceof EntityWidget);
        entityWidgets.clear();
        scrollY = 0;
    }


    @Override
    public boolean shouldPause() {
        return false;
    }

    public void disableAll() {
        for (EntityWidget w : entityWidgets) {
            w.setActive(false);
        }
    }

    // -- Header Factory Methods --

    private SearchWidget createSearchBar() {
        assert client != null;
        float w = client.getWindow().getScaledWidth() / 4f;
        return new SearchWidget(
                client.getWindow().getScaledWidth() / 2f - (w / 2f),
                5, w, 20f
        );
    }

    private PlayerWidget createPlayerButton() {
        assert client != null;
        float cx = client.getWindow().getScaledWidth() / 2f;
        return new PlayerWidget(
                cx + (client.getWindow().getScaledWidth() / 8f) + 5,
                7, 15, 15,
                this
        );
    }

    private ButtonWidget createHelpButton() {
        assert client != null;
        float cx = client.getWindow().getScaledWidth() / 2f;
        return new HelpWidget(
                (int)(cx - (client.getWindow().getScaledWidth() / 8f) - 5) - 30,
                5, 20, 20
        );
    }

    public int getGuiScale() {
        assert client != null;
        return client.options.getGuiScale().getValue();
    }
}
