package draylar.identity.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.identity.Identity;
import draylar.identity.api.variant.IdentityType;
import draylar.identity.network.impl.FavoritePackets;
import draylar.identity.network.impl.SwapPackets;
import draylar.identity.screen.IdentityScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntityWidget<T extends LivingEntity> extends PressableWidget {

    public static int VERTICAL_OFFSET = 30;
    private static int BASE_Y_OFFSET = 10;

    private IdentityType<T> type;
    private T entity;
    private int size;
    private boolean active;
    private boolean starred;
    private IdentityScreen parent;

    public EntityWidget(
            int x, int y, int width, int height,
            IdentityType<T> type,
            T entity,
            IdentityScreen parent,
            boolean starred,
            boolean current
    ) {
        super(x, y, width, height, Text.of(""));
        this.type = type;
        this.entity = entity;
        this.parent = parent;
        this.starred = starred;
        this.active = current;

        float baseSizeFactor = (float) (25F / Math.max(entity.getWidth(), entity.getHeight()));
        double scaleFactor = parent.getScaleFactor();
        int guiScale = parent.getGuiScale();
        double finalScale = scaleFactor / (guiScale == 0 ? 1 : guiScale);
        this.size = Math.max(1, (int) (baseSizeFactor * finalScale));

        entity.setGlowing(true);
        setTooltip(Tooltip.of(entity.getDisplayName()));
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) { }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean hit = mx >= getX() && mx < getX() + getWidth()
                && my >= getY() && my < getY() + getHeight();

        if (hit) {
            if (button == 0) {
                SwapPackets.sendSwapRequest(type);
                parent.disableAll();
                active = true;
            } else if (button == 1) {
                starred = !starred;
                FavoritePackets.sendFavoriteRequest(type, starred);
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    public Text getHoverName() { return entity.getDisplayName(); }


    // 1.21.1: do not override render, override renderWidget instead
    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 2) clamp GUI-scale to [1..5], default Auto→3
        int rawGui = parent.getGuiScale();    // 0 == Auto, otherwise 1–5
        int clampedGui = (rawGui == 0) ? 3 : Math.min(rawGui, 5);

        // 3) compute how “big” a block unit is in pixels
        float baseSizePerBlock = 25F / Math.max(entity.getWidth(), entity.getHeight());

        // 4) apply inverse scaling by GUI-scale
        double windowScale = parent.getScaleFactor();
        double effectiveScale = windowScale / clampedGui;

        // 5) final pixel size for our model
        int scale = Math.max(1, (int)(baseSizePerBlock * effectiveScale));

        // 6) pixel height of entity
        int pixelHeight = (int)(entity.getHeight() * scale);

        // 7) old-style bottom anchoring
        int slotCX  = getX() + getWidth()  / 2;
        int slotCY  = getY() + getHeight() / 2;
        int bottomY = slotCY + (pixelHeight / 2);

        // Relative mouse movement vs entity anchor
        float relMouseX = (mouseX - slotCX);
        float relMouseY = (mouseY - bottomY);

        try {
            drawEntityCompat(
                    ctx,
                    slotCX, bottomY,
                    scale,
                    relMouseX, relMouseY,
                    entity
            );
        } catch (Exception e) {
            Identity.LOGGER.warn("Failed to render " + type.getEntityType().getTranslationKey(), e);
        }

        // overlays
        if (starred) {
            ctx.drawTexture(
                    Identity.id("textures/gui/star.png"),
                    getX(), getY(), 0, 0, 15, 15, 15, 15
            );
        }
        if (active) {
            ctx.drawTexture(
                    Identity.id("textures/gui/selected.png"),
                    getX(), getY(), getWidth(), getHeight(),
                    0, 0, 48, 32, 48, 32
            );
        }
    }

//    public static void drawEntityCompat(
//            DrawContext context,
//            int slotCX, int bottomY, int scale,
//            float relMouseX, float relMouseY,
//            LivingEntity entity
//    ) {
//        // Old quaternions (exactly like 1.20.1)
//        float f = (float)Math.atan(relMouseX / 40.0F);
//        float g = (float)Math.atan(relMouseY / 40.0F);
//        Quaternionf q1 = new Quaternionf().rotateZ((float)Math.PI);
//        Quaternionf q2 = new Quaternionf().rotateX(g * 20.0F * ((float)Math.PI / 180F));
//        q1.mul(q2);
//
//        // Entity-space anchor (this replaces bottomY)
//        float p = entity.getScale();
//        Vector3f anchor = new Vector3f(0.0F, entity.getHeight() / 2.0F, 0.0F);
//
//        float normalizedScale = (float)scale / p;
//
//        InventoryScreen.drawEntity(context, slotCX, bottomY, normalizedScale, anchor, q1, q2, entity);
//    }
    public static void drawEntityCompat(
            DrawContext context,
            int slotCX, int bottomY, int scale,
            float relMouseX, float relMouseY,
            LivingEntity entity
    ) {


        // Centerpoint
        float f = (float)Math.atan(-relMouseX / 40.0F);
        float g = (float)Math.atan(-relMouseY / 40.0F);
        Quaternionf q1 = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf q2 = new Quaternionf().rotateX(g * 20.0F * ((float)Math.PI / 180F));
        q1.mul(q2);

        // Save old rotation
        float prevBodyYaw = entity.bodyYaw;
        float prevYaw = entity.getYaw();
        float prevPitch = entity.getPitch();
        float prevHeadYaw = entity.prevHeadYaw;
        float prevHeadYaw2 = entity.headYaw;

        // Apply facing
        entity.bodyYaw = 180.0F + f * 20.0F;
        entity.setYaw(180.0F + f * 40.0F);
        entity.setPitch(-g * 20.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();

        float p = entity.getScale();
        Vector3f anchor = new Vector3f(0.0F, entity.getHeight() / 2.0F, 0.0F);

        float normalizedScale = (float)scale / p;

        // Render entity without anchor
        InventoryScreen.drawEntity(context, slotCX, bottomY, normalizedScale, anchor, q1, q2, entity);

        // Restore rotation
        entity.bodyYaw = prevBodyYaw;
        entity.setYaw(prevYaw);
        entity.setPitch(prevPitch);
        entity.prevHeadYaw = prevHeadYaw;
        entity.headYaw = prevHeadYaw2;
    }











    private int getSize() {
        // Size the entity to a fraction of the cell size, normalized by entity bounding box.
        // This naturally responds to GUI scale because the widget dimensions are in scaled pixels.
        int cell = Math.max(1, Math.min(getWidth(), getHeight()));
        float unit = Math.max(0.6f, Math.max(entity.getWidth(), entity.getHeight()));

        // Fit entity to ~70% of the cell's smaller dimension
        float target = cell * 0.70f;
        int size = Math.max(8, Math.round(target / unit));
        return size;
    }

    @Override
    public void onPress() { }

    public void setActive(boolean a) {
        this.active = a;
    }

    public void dispose() {
        if (entity != null) {
            try {
                entity.discard();
            } catch (Exception ignored) { }
            entity = null;
        }
        type = null;
        parent = null;
    }
}
