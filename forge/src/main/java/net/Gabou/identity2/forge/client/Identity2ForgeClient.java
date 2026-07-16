package net.Gabou.identity2.forge.client;

import net.Gabou.identity2.client.Identity2ClientBootstrap;
import net.Gabou.identity2.identity.IdentityTraitTags;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

public final class Identity2ForgeClient {
    private static boolean initialized = false;

    private Identity2ForgeClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        Identity2ClientBootstrap.initialize(new Identity2ForgeClientPlatform());
        MinecraftForge.EVENT_BUS.addListener(Identity2ForgeClient::onRenderGuiOverlayPre);
        MinecraftForge.EVENT_BUS.addListener(Identity2ForgeClient::onRenderBlockScreenEffect);
    }

    private static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event == null || event.getOverlay() == null) {
            return;
        }
        if (!VanillaGuiOverlay.AIR_LEVEL.id().equals(event.getOverlay().id())) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        Player player = client.getCameraEntity() instanceof Player cameraPlayer ? cameraPlayer : client.player;
        if (!identity2$isAquaticMorph(player)) {
            return;
        }
        if (!player.isEyeInFluid(FluidTags.WATER)) {
            return;
        }

        event.setCanceled(true);
    }

    private static boolean identity2$isAquaticMorph(Player player) {
        if (player == null) {
            return false;
        }
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        return identity != null && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(identity.getType()));
    }

    private static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        Entity identity = ((EntityAccessor) event.getPlayer()).getCurrentIdentity();
        if (identity != null && identity.getBbHeight() < 1.0F) {
            event.setCanceled(true);
        }
    }
}

