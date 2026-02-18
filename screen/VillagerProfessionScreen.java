package draylar.identity.screen;

import draylar.identity.network.impl.VillagerProfessionPackets;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VillagerProfessionScreen extends Screen {

    private final Identifier professionId;
    private final net.minecraft.util.math.BlockPos pos;
    private final Identifier worldId;
    private final String originalName;
    private final String existingProfessionId;
    private TextFieldWidget nameField;
    private ButtonWidget deleteButton;

    public VillagerProfessionScreen(Identifier professionId, net.minecraft.util.math.BlockPos pos, Identifier worldId, String originalName, String existingProfessionId) {
        super(Text.translatable("identity.profession.title"));
        this.professionId = professionId;
        this.pos = pos;
        this.worldId = worldId;
        this.originalName = originalName;
        this.existingProfessionId = existingProfessionId;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        nameField = new TextFieldWidget(textRenderer, centerX - 100, centerY - 10, 200, 20, Text.empty());
        if (originalName != null) {
            nameField.setText(originalName);
        }
        addSelectableChild(nameField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("identity.profession.confirm"), button -> {
            VillagerProfessionPackets.sendSetProfession(professionId, nameField.getText(), false, pos, worldId, originalName);
            close();
        }).dimensions(centerX - 100, centerY + 20, 98, 20).build());
        deleteButton = ButtonWidget.builder(Text.translatable("identity.profession.delete"), button -> {
            MinecraftClient.getInstance().setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    VillagerProfessionPackets.sendSetProfession(professionId, nameField.getText(), true, pos, worldId, originalName);
                }
                MinecraftClient.getInstance().setScreen(null);
            }, Text.translatable("identity.profession.delete"), Text.translatable("identity.profession.delete_confirm")));
        }).dimensions(centerX + 2, centerY + 20, 98, 20).build();
        deleteButton.active = originalName != null;
        addDrawableChild(deleteButton);
        setInitialFocus(nameField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int titleY = height / 2 - 50;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, titleY, 0xFFFFFF);
        int infoY = titleY + 15;
        if (originalName != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("identity.profession.current_name", originalName), width / 2, infoY, 0xAAAAAA);
            infoY += 12;
            if (existingProfessionId != null && !existingProfessionId.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("identity.profession.current_profession", resolveProfessionName(existingProfessionId)), width / 2, infoY, 0xAAAAAA);
                infoY += 12;
            }
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("identity.profession.prompt"), width / 2, infoY, 0xAAAAAA);
            infoY += 12;
        }
        nameField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }

    private Text resolveProfessionName(String professionKey) {
        Identifier id = Identifier.tryParse(professionKey);
        return id != null ? Text.literal(id.toString()) : Text.literal(professionKey);
    }
}
