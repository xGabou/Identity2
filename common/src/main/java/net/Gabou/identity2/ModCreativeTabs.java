package net.Gabou.identity2;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Identity2.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> IDENTITY2_TAB = TABS.register("identity2", () ->
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.identity2"))
            .icon(() -> new ItemStack(ModItems.SOUL_CATALYST.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.IDENTITY_BOOK.get());
                output.accept(ModItems.MUD_JAR.get());
                output.accept(ModItems.GLASS_JAR.get());
                output.accept(ModItems.REINFORCED_JAR.get());
                output.accept(ModItems.ENDGAME_JAR.get());
                output.accept(ModItems.SOUL_CATALYST.get());
            })
            .build()
    );

    private ModCreativeTabs() {
    }

    public static void initialize() {
        TABS.register();
    }
}
