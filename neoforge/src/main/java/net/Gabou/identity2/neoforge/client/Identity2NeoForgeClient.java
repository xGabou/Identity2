package net.Gabou.identity2.neoforge.client;

import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.client.Identity2ClientBootstrap;
import net.Gabou.identity2.identity.IdentityTraitTags;
import net.Gabou.identity2.identity.SilverfishBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class Identity2NeoForgeClient {
    private static boolean initialized = false;

    private Identity2NeoForgeClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        Identity2ClientBootstrap.initialize(new Identity2NeoForgeClientPlatform());
        NeoForge.EVENT_BUS.addListener(Identity2NeoForgeClient::onClientLogin);
        NeoForge.EVENT_BUS.addListener(Identity2NeoForgeClient::onRenderGuiOverlayPre);
        NeoForge.EVENT_BUS.addListener(Identity2NeoForgeClient::onRenderBlockScreenEffect);
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ModRegistries.captureIdentityAbilityRegistry(event.getPlayer().registryAccess());
    }

    // NeoForge's patched ScreenEffectRenderer.renderScreenEffect resolves the in-wall
    // overlay through its own getOverlayBlock(Player) and never calls the vanilla
    // getViewBlockingState that ScreenEffectRendererMixin injects into (that mixin
    // still handles Fabric). Cancel the overlay through the event instead so a
    // burrowed silverfish morph can see out of its block on NeoForge too.
    private static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
            return;
        }
        if (SilverfishBurrowManager.shouldSuppressBlockOverlay(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (event == null || event.getName() == null) {
            return;
        }
        if (!VanillaGuiLayers.AIR_LEVEL.equals(event.getName())) {
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
}
