package net.Gabou.identity2;

import net.Gabou.identity2.client.screen.IdentityProgressionScreen;
import net.Gabou.identity2.packets.OpenProgressionScreenS2CPacketPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.Gabou.identity2.client.transition.MorphAcquisitionEffectController;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.auth.ClientAuth;
import net.Gabou.identity2.auth.ClientLauncherGuards;
import net.Gabou.identity2.auth.S2CChallengePacket;
import net.Gabou.identity2.client.platform.ModClientPlatform;
import net.Gabou.identity2.client.screen.IdentitySelectionScreen;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariantRegistry;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacket;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.packets.IdentityUnlockSyncEntry;
import net.Gabou.identity2.packets.IdentityUnlockSyncS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityVariantDefinitionS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityVillagerTradeRequestC2SPacketPayload;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.packets.UnlockedIdentitySyncS2CPacketPayload;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.util.EnderDragonEntityRendererAccessor;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.packets.ProgressionChargeSyncRequestC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarSelectC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarStateS2CPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarTransferC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionPlayerChargesS2CPacketPayload;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

public final class Identity2Client {
    private static final Identity2Client INSTANCE = new Identity2Client();
    private static ModClientPlatform platform;
    private static boolean initialized = false;

    public static final ArrayList<BiFunction<Entity, Entity, Entity>> visualPatchValues = new ArrayList<>(0);
    public static final ArrayList<Identifier> visualPatchKeys = new ArrayList<>(0);
    private static final KeyMapping.Category IDENTITY_KEY_CATEGORY = KeyMapping.Category
            .register(Identifier.parse("category.identity2.test"));

    private static final KeyMapping primaryAbilityKeyBinding = new KeyMapping(
            "key.identity2.primary_ability",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping secondaryAbilityKeyBinding = new KeyMapping(
            "key.identity2.secondary_ability",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping identityMenuKeyBinding = new KeyMapping(
            "key.identity2.identity_menu",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteMorphSlot1KeyBinding = new KeyMapping(
            "key.identity2.favorite_morph_1",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteMorphSlot2KeyBinding = new KeyMapping(
            "key.identity2.favorite_morph_2",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteMorphSlot3KeyBinding = new KeyMapping(
            "key.identity2.favorite_morph_3",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteSaveSlot1KeyBinding = new KeyMapping(
            "key.identity2.favorite_save_1",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F9,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteSaveSlot2KeyBinding = new KeyMapping(
            "key.identity2.favorite_save_2",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F10,
            IDENTITY_KEY_CATEGORY);
    private static final KeyMapping favoriteSaveSlot3KeyBinding = new KeyMapping(
            "key.identity2.favorite_save_3",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F11,
            IDENTITY_KEY_CATEGORY);

    private static final int fadingTickRequirement = 0;
    private static final int MAX_PENDING_PACKET_PROCESS_PER_TICK = 256;
    private static final int MAX_PENDING_PACKET_PROCESS_TICKS = 100;
    private static final int MAX_PENDING_PACKET_QUEUE_SIZE = 8192;
    private static final int MORPH_TRANSITION_PARTICLES_PER_TICK = 4;
    private static int lastCooldown = 0;
    private static int ticksSinceUpdate = 0;
    private static boolean isFading = false;
    private static int fadingProgress = 0;
    private static int pendingPacketProcessTicks = 0;
    private static final ArrayList<CustomEntityDataS2CPacketPayload> pendingDoubleDataPackets = new ArrayList<>(0);
    private static final ArrayList<CustomEntityStringDataS2CPacketPayload> pendingStringDataPackets = new ArrayList<>(
            0);
    private static final ArrayList<CustomEntityBoolDataS2CPacketPayload> pendingBoolDataPackets = new ArrayList<>(0);
    private static final ArrayList<IdentityUnlockSyncS2CPacketPayload> pendingUnlockSyncPackets = new ArrayList<>(0);
    private static final Map<Integer, Map<VariantDefinitionKey, net.minecraft.nbt.CompoundTag>> clientVariantDefinitions = new LinkedHashMap<>();
    private static final Map<VariantChunkKey, PendingVariantDefinition> pendingVariantDefinitions = new LinkedHashMap<>();
    private static final ArrayList<UnlockedIdentitySyncS2CPacketPayload> pendingUnlockedIdentityPackets = new ArrayList<>(0);
    private static final String[] favoriteIdentityIds = new String[] { "", "", "" };
    private static final String[] favoriteVariantNbt = new String[] { "", "", "" };

    static {
        addVisualPatch((identity, entity) -> {
            if (identity instanceof EnderDragon dragonIdentity) {
                dragonIdentity.yRotA += Mth.wrapDegrees(entity.getYRot() - identity.getYRot()) * 0.1F;
            }
            return identity;
        }, Identifier.parse("minecraft:ender_dragon"));
    }

    private Identity2Client() {
    }

    public static void initialize(ModClientPlatform platformImpl) {
        if (initialized) {
            return;
        }

        ClientLauncherGuards.enforce();
        platform = platformImpl;
        initialized = true;

        KeyMappingRegistry.register(primaryAbilityKeyBinding);
        KeyMappingRegistry.register(secondaryAbilityKeyBinding);
        KeyMappingRegistry.register(identityMenuKeyBinding);
        KeyMappingRegistry.register(favoriteMorphSlot1KeyBinding);
        KeyMappingRegistry.register(favoriteMorphSlot2KeyBinding);
        KeyMappingRegistry.register(favoriteMorphSlot3KeyBinding);
        KeyMappingRegistry.register(favoriteSaveSlot1KeyBinding);
        KeyMappingRegistry.register(favoriteSaveSlot2KeyBinding);
        KeyMappingRegistry.register(favoriteSaveSlot3KeyBinding);

        if (platform != null) {
            platform.logClientRegistries();
        }

        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                CustomEntityDataS2CPacketPayload.ID,
                CustomEntityDataS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                CustomEntityStringDataS2CPacketPayload.ID,
                CustomEntityStringDataS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                CustomEntityBoolDataS2CPacketPayload.ID,
                CustomEntityBoolDataS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onUpdateCustomData(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                IdentityUnlockSyncS2CPacketPayload.ID,
                IdentityUnlockSyncS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onUpdateUnlockedIdentities(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                IdentityVariantDefinitionS2CPacketPayload.ID,
                IdentityVariantDefinitionS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onVariantDefinitionData(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                UnlockedIdentitySyncS2CPacketPayload.ID,
                UnlockedIdentitySyncS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> INSTANCE.onUpdateUnlockedIdentities(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                MorphAcquisitionS2CPacketPayload.ID,
                MorphAcquisitionS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> onMorphAcquisition(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                OpenProgressionScreenS2CPacketPayload.ID,
                OpenProgressionScreenS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(Identity2Client::openProgressionScreen));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                ProgressionPlayerChargesS2CPacketPayload.ID,
                ProgressionPlayerChargesS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> IdentityProgressionScreen.onPlayerChargeSync(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                ProgressionJarStateS2CPacketPayload.ID,
                ProgressionJarStateS2CPacketPayload.CODEC,
                (payload, context) -> context.queue(() -> IdentityProgressionScreen.onJarStateSync(payload)));
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                S2CChallengePacket.ID,
                S2CChallengePacket.CODEC,
                (payload, context) -> context.queue(() -> ClientAuth.handleChallenge(payload)));

        ClientTickEvent.CLIENT_POST.register(Identity2Client::onClientTickEnd);
        ClientGuiEvent.RENDER_HUD.register(Identity2Client::renderIdentityCooldown);
    }

    public static void sendIdentityAbilityPacket(int entityId) {
        NetworkManager.sendToServer(new IdentityAbilityPacketPayload(entityId));
    }

    public static void sendMorphRequest(String identityId) {
        sendMorphRequest(identityId, "");
    }

    public static void sendMorphRequest(String identityId, String variantNbt) {
        String reference = "";
        try {
            if (identityId != null && !identityId.isBlank()) reference = IdentityVariantRegistry.stableId(Identifier.parse(identityId), IdentityProgression.parseVariantNbt(variantNbt));
        } catch (Exception ignored) { }
        NetworkManager.sendToServer(new IdentityMorphRequestC2SPacketPayload(identityId == null ? "" : identityId, reference));
    }

    public static void sendVillagerTradeRequest(UUID targetUuid) {
        if (targetUuid == null) {
            return;
        }
        NetworkManager.sendToServer(new IdentityVillagerTradeRequestC2SPacketPayload(targetUuid.toString()));
    }

    public static void requestProgressionChargeSync() {
        NetworkManager.sendToServer(new ProgressionChargeSyncRequestC2SPacketPayload());
    }

    public static void sendProgressionJarSelect(int slotIndex) {
        NetworkManager.sendToServer(new ProgressionJarSelectC2SPacketPayload(slotIndex));
    }

    public static void sendProgressionJarTransfer(int slotIndex, String identityId, int amount, boolean deposit) {
        if (identityId == null || identityId.isBlank() || amount <= 0) {
            return;
        }
        NetworkManager.sendToServer(new ProgressionJarTransferC2SPacketPayload(slotIndex, identityId, amount, deposit));
    }

    public static void addVisualPatch(BiFunction<Entity, Entity, Entity> value, Identifier id) {
        visualPatchKeys.ensureCapacity(visualPatchKeys.size() + 1);
        visualPatchValues.ensureCapacity(visualPatchValues.size() + 1);
        visualPatchKeys.add(id);
        visualPatchValues.add(value);
    }

    private static void openProgressionScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        client.gui.setScreen(new IdentityProgressionScreen());
    }

    private static void onClientTickEnd(Minecraft client) {
        processPendingCustomDataPackets(client);
        tickMorphTransitionEffects(client);
        MorphAcquisitionEffectController.tick(client);

        while (identityMenuKeyBinding.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) {
                if (!IdentitySettings.enableClientSwapMenu) {
                    continue;
                }
                client.gui.setScreen(new IdentitySelectionScreen());
            }
        }

        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        processFavoriteKeybinds(player);

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        boolean wardenHidden = WardenBurrowManager.isHidden(player);
        if (wardenHidden) {
            boolean exitRequested = identity2$shouldExitWardenBurrow(client);
            disableMovementInputs(client, player);
            player.noPhysics = true;
            player.setDeltaMovement(Vec3.ZERO);
            if (exitRequested) {
                sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_SECONDARY);
            }
            sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_PASSIVE);
            return;
        } else if (player.noPhysics) {
            player.noPhysics = false;
        }

        IdentityAbilityDefinition identityAbility = ModRegistries.resolveIdentityAbility(identity.getType());
        boolean hasFallbackPredefAbility = hasPredefFallback(identity);
        if (identityAbility == null && !hasFallbackPredefAbility) {
            return;
        }

        int primaryCooldown = resolvePrimaryCooldown(identity, identityAbility);
        int secondaryCooldown = resolveSecondaryCooldown(identity, identityAbility);
        int useDuration = identityAbility != null ? identityAbility.useduration() : 0;
        int usedPrimaryAbility = 0;

        while (primaryAbilityKeyBinding.consumeClick()) {
            if (((EntityAccessor) player).getAbilityCooldown() == 0) {
                ((EntityAccessor) player).setAbilityCooldown(primaryCooldown + useDuration);
                sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_PRIMARY);
                usedPrimaryAbility = 1;
            }
        }

        while (secondaryAbilityKeyBinding.consumeClick()) {
            if (((EntityAccessor) player).getSecondaryAbilityCooldown() == 0) {
                ((EntityAccessor) player).setSecondaryAbilityCooldown(secondaryCooldown);
                sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_SECONDARY);
            }
        }

        int primaryCd = ((EntityAccessor) player).getAbilityCooldown();
        if (primaryCd > primaryCooldown) {
            sendIdentityAbilityPacket(primaryCooldown + useDuration - primaryCd + 1);
        }

        if (usedPrimaryAbility == 1) {
            sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_PASSIVE_USED);
        } else {
            sendIdentityAbilityPacket(ModPackets.ABILITY_ACTION_PASSIVE);
        }

    }

    private void onUpdateCustomData(CustomEntityDataS2CPacketPayload packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            enqueuePendingPacket(pendingDoubleDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            CustomData n = ((EntityAccessor) entity).getCustomData();
            boolean shapeChanged = false;
            for (CustomEntityDataS2CPacket.Entry entry : packet.entries()) {
                ((NbtComponentAccessor) (Object) n).getNbt().putDouble(entry.key(), entry.value());
                if ("width_override".equals(entry.key()) || "height_override".equals(entry.key())) {
                    shapeChanged = true;
                }
            }
            if (shapeChanged) {
                entity.refreshDimensions();
                Entity identity = ((EntityAccessor) entity).getCurrentIdentity();
                if (identity != null) {
                    ((EntityAccessor) entity).setStandingEyeHeight(identity.getEyeHeight());
                }
            }
        }
    }

    private void onUpdateCustomData(CustomEntityStringDataS2CPacketPayload packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            enqueuePendingPacket(pendingStringDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null || !applyStringPacket(entity, packet, false)) enqueuePendingPacket(pendingStringDataPackets, packet);
    }

    private void onUpdateCustomData(CustomEntityBoolDataS2CPacketPayload packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            enqueuePendingPacket(pendingBoolDataPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            CustomData n = ((EntityAccessor) entity).getCustomData();
            for (CustomEntityDataS2CPacket.EntryBool entry : packet.entries()) {
                ((NbtComponentAccessor) (Object) n).getNbt().putBoolean(entry.key(), entry.value());
            }
        }
    }

    private void onUpdateUnlockedIdentities(UnlockedIdentitySyncS2CPacketPayload packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            enqueuePendingPacket(pendingUnlockedIdentityPackets, packet);
            return;
        }

        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity != null) {
            CustomData n = ((EntityAccessor) entity).getCustomData();
            IdentityProgression.storeUnlockedIdentityData(
                    ((NbtComponentAccessor) (Object) n).getNbt(),
                    packet.unlockedIdentityIds(),
                    toVariantUnlockMap(packet.unlockedVariantEntries())
            );
        }
    }

    private void onUpdateUnlockedIdentities(IdentityUnlockSyncS2CPacketPayload packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            enqueuePendingPacket(pendingUnlockSyncPackets, packet);
            return;
        }

        if (!tryApplyUnlockSync(client, packet)) {
            enqueuePendingPacket(pendingUnlockSyncPackets, packet);
        }
    }

    private void onVariantDefinitionData(IdentityVariantDefinitionS2CPacketPayload packet) {
        if (packet.reset()) {
            clientVariantDefinitions.remove(packet.entityId());
            pendingVariantDefinitions.keySet().removeIf(key -> key.entityId == packet.entityId());
        }
        if (packet.chunkCount() == 0 && packet.identityId().isBlank() && packet.variantId().isBlank()) return;
        if (packet.chunkCount() <= 0 || packet.chunkCount() > IdentityVariantRegistry.MAX_DEFINITION_CHUNKS || packet.chunkIndex() < 0 || packet.chunkIndex() >= packet.chunkCount() || packet.data().length > IdentityVariantDefinitionS2CPacketPayload.MAX_CHUNK_BYTES) return;
        Identifier identity; try { identity = Identifier.parse(packet.identityId()); UUID.fromString(packet.variantId()); } catch (Exception ignored) { return; }
        VariantChunkKey key = new VariantChunkKey(packet.entityId(), identity, packet.variantId());
        PendingVariantDefinition pending = pendingVariantDefinitions.computeIfAbsent(key, ignored -> new PendingVariantDefinition(packet.chunkCount()));
        if (pending.chunkCount != packet.chunkCount() || !pending.add(packet.chunkIndex(), packet.data())) { pendingVariantDefinitions.remove(key); return; }
        if (!pending.complete()) return;
        pendingVariantDefinitions.remove(key);
        net.minecraft.nbt.CompoundTag definition = IdentityProgression.parseVariantNbt(new String(pending.join(), StandardCharsets.UTF_8));
        if (!packet.variantId().equals(IdentityVariantRegistry.stableId(identity, definition))) return;
        clientVariantDefinitions.computeIfAbsent(packet.entityId(), ignored -> new LinkedHashMap<>()).put(new VariantDefinitionKey(identity, packet.variantId()), definition);
    }

    private static void processPendingCustomDataPackets(Minecraft client) {
        if (client.level == null) {
            return;
        }

        if (pendingDoubleDataPackets.isEmpty() && pendingStringDataPackets.isEmpty()
                && pendingBoolDataPackets.isEmpty()
                && pendingUnlockSyncPackets.isEmpty()
                && pendingUnlockedIdentityPackets.isEmpty()) {
            pendingPacketProcessTicks = 0;
            return;
        }

        pendingPacketProcessTicks++;
        processPendingDoublePackets(client);
        processPendingStringPackets(client);
        processPendingBoolPackets(client);
        processPendingUnlockSyncPackets(client);
        processPendingUnlockedIdentityPackets(client);

        // Avoid an unbounded per-tick scan if some queued packets can never resolve.
        if (pendingPacketProcessTicks > MAX_PENDING_PACKET_PROCESS_TICKS) {
            pendingDoubleDataPackets.clear();
            pendingStringDataPackets.clear();
            pendingBoolDataPackets.clear();
            pendingUnlockSyncPackets.clear();
            pendingUnlockedIdentityPackets.clear();
            pendingPacketProcessTicks = 0;
        }
    }

    private static void processPendingDoublePackets(Minecraft client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingDoubleDataPackets.size());
        for (int i = 0; i < max;) {
            if (INSTANCE.tryApplyCustomData(client, pendingDoubleDataPackets.get(i))) {
                pendingDoubleDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingStringPackets(Minecraft client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingStringDataPackets.size());
        for (int i = 0; i < max;) {
            if (INSTANCE.tryApplyCustomData(client, pendingStringDataPackets.get(i))) {
                pendingStringDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingBoolPackets(Minecraft client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingBoolDataPackets.size());
        for (int i = 0; i < max;) {
            if (INSTANCE.tryApplyCustomData(client, pendingBoolDataPackets.get(i))) {
                pendingBoolDataPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingUnlockedIdentityPackets(Minecraft client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingUnlockedIdentityPackets.size());
        for (int i = 0; i < max;) {
            if (INSTANCE.tryApplyUnlockedIdentities(client, pendingUnlockedIdentityPackets.get(i))) {
                pendingUnlockedIdentityPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static void processPendingUnlockSyncPackets(Minecraft client) {
        int max = Math.min(MAX_PENDING_PACKET_PROCESS_PER_TICK, pendingUnlockSyncPackets.size());
        for (int i = 0; i < max;) {
            if (INSTANCE.tryApplyUnlockSync(client, pendingUnlockSyncPackets.get(i))) {
                pendingUnlockSyncPackets.remove(i);
                max--;
            } else {
                i++;
            }
        }
    }

    private static Map<String, java.util.List<String>> toVariantUnlockMap(
            java.util.List<UnlockedIdentitySyncS2CPacketPayload.VariantEntry> entries
    ) {
        Map<String, java.util.List<String>> result = new LinkedHashMap<>();
        for (UnlockedIdentitySyncS2CPacketPayload.VariantEntry entry : entries) {
            if (entry == null || entry.identityId() == null || entry.identityId().isBlank()) {
                continue;
            }
            java.util.List<String> tokens = new ArrayList<>();
            java.util.List<net.minecraft.nbt.CompoundTag> variantData = entry.variantData();
            if (variantData != null) {
                for (net.minecraft.nbt.CompoundTag data : variantData) {
                    tokens.add(IdentityProgression.toVariantUnlockToken(
                            IdentityProgression.normalizeVariantForUnlock(data)
                    ));
                }
            }
            result.put(entry.identityId(), tokens);
        }
        return result;
    }

    private boolean tryApplyCustomData(Minecraft client, CustomEntityDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        CustomData n = ((EntityAccessor) entity).getCustomData();
        boolean shapeChanged = false;
        for (CustomEntityDataS2CPacket.Entry entry : packet.entries()) {
            ((NbtComponentAccessor) (Object) n).getNbt().putDouble(entry.key(), entry.value());
            if ("width_override".equals(entry.key()) || "height_override".equals(entry.key())) {
                shapeChanged = true;
            }
        }
        if (shapeChanged) {
            entity.refreshDimensions();
            Entity identity = ((EntityAccessor) entity).getCurrentIdentity();
            if (identity != null) {
                ((EntityAccessor) entity).setStandingEyeHeight(identity.getEyeHeight());
            }
        }
        return true;
    }

    private boolean tryApplyCustomData(Minecraft client, CustomEntityStringDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        return entity != null && applyStringPacket(entity, packet, true);
    }

    private boolean applyStringPacket(Entity entity, CustomEntityStringDataS2CPacketPayload packet, boolean clearTransitionCache) {
        Map<String, String> values = new LinkedHashMap<>();
        for (CustomEntityDataS2CPacket.EntryString entry : packet.entries()) values.put(entry.key(), entry.value());
        String selectedType = values.getOrDefault(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, values.getOrDefault("model_override", ""));
        String previousType = values.getOrDefault(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY, "");
        String selected = resolveReference(packet.entityid(), selectedType, values.get(IdentityProgression.SELECTED_IDENTITY_VARIANT_REF_KEY));
        String previous = resolveReference(packet.entityid(), previousType, values.get(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_REF_KEY));
        if (selected == null || previous == null) return false;
        CustomData data = ((EntityAccessor)entity).getCustomData();
        net.minecraft.nbt.CompoundTag nbt = ((NbtComponentAccessor)(Object)data).getNbt();
        for (Map.Entry<String,String> entry: values.entrySet()) if (!IdentityProgression.SELECTED_IDENTITY_VARIANT_REF_KEY.equals(entry.getKey()) && !IdentityProgression.PREVIOUS_IDENTITY_VARIANT_REF_KEY.equals(entry.getKey())) nbt.putString(entry.getKey(), entry.getValue());
        if (values.containsKey(IdentityProgression.SELECTED_IDENTITY_VARIANT_REF_KEY)) nbt.putString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, selected);
        if (values.containsKey(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_REF_KEY)) nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY, previous);
        if (clearTransitionCache) MorphTransitionHelper.clearCachedPreviousIdentity(entity.getId());
        applyIdentityFromCustomData(entity);
        return true;
    }

    private static String resolveReference(int entityId, String type, String reference) {
        if (reference == null) return ""; // legacy raw packet
        if (reference.isBlank() || type == null || type.isBlank() || IdentityProgression.BASE_PLAYER_TRANSITION_SENTINEL.equals(type)) return "";
        try { net.minecraft.nbt.CompoundTag nbt = clientVariantDefinitions.getOrDefault(entityId, Map.of()).get(new VariantDefinitionKey(Identifier.parse(type), reference)); return nbt == null ? null : IdentityProgression.serializeVariantNbt(nbt); }
        catch (Exception ignored) { return ""; }
    }

    private boolean tryApplyCustomData(Minecraft client, CustomEntityBoolDataS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        CustomData n = ((EntityAccessor) entity).getCustomData();
        for (CustomEntityDataS2CPacket.EntryBool entry : packet.entries()) {
            ((NbtComponentAccessor) (Object) n).getNbt().putBoolean(entry.key(), entry.value());
        }
        return true;
    }

    private boolean tryApplyUnlockedIdentities(Minecraft client, UnlockedIdentitySyncS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        CustomData n = ((EntityAccessor) entity).getCustomData();
        IdentityProgression.storeUnlockedIdentityData(
                ((NbtComponentAccessor) (Object) n).getNbt(),
                packet.unlockedIdentityIds(),
                toVariantUnlockMap(packet.unlockedVariantEntries())
        );
        return true;
    }

    private boolean tryApplyUnlockSync(Minecraft client, IdentityUnlockSyncS2CPacketPayload packet) {
        Entity entity = resolvePacketTarget(client, packet.entityid());
        if (entity == null) {
            return false;
        }

        CustomData customData = ((EntityAccessor) entity).getCustomData();
        net.minecraft.nbt.CompoundTag nbt = ((NbtComponentAccessor) (Object) customData).getNbt();
        java.util.LinkedHashSet<String> unlocked = new java.util.LinkedHashSet<>();
        Map<String, java.util.List<String>> variants = new LinkedHashMap<>();
        if (!packet.replaceAll()) {
            for (String identityId : IdentityProgression.readUnlockedIdentityIdSet(nbt)) {
                unlocked.add(identityId);
            }
            for (Map.Entry<String, Set<String>> entry : IdentityProgression.readUnlockedIdentityVariantTokenSet(nbt).entrySet()) {
                variants.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        for (IdentityUnlockSyncEntry entry : packet.entries()) {
            if (entry == null || entry.identityId() == null) {
                continue;
            }
            String identityId = entry.identityId().toString();
            unlocked.add(identityId);
            java.util.List<String> tokens = new ArrayList<>();
            if (entry.variantTokens() != null) for (String reference : entry.variantTokens()) {
                net.minecraft.nbt.CompoundTag definition = clientVariantDefinitions.getOrDefault(packet.entityid(), Map.of())
                    .get(new VariantDefinitionKey(entry.identityId(), reference));
                if (definition == null) return false;
                tokens.add(IdentityProgression.toVariantUnlockToken(IdentityProgression.normalizeVariantForUnlock(definition)));
            }
            if (entry.replaceTokens()) {
                if (tokens.isEmpty()) {
                    variants.remove(identityId);
                } else {
                    variants.put(identityId, new ArrayList<>(tokens));
                }
                continue;
            }
            if (!tokens.isEmpty()) {
                java.util.List<String> existing = variants.computeIfAbsent(identityId, ignored -> new ArrayList<>());
                existing.addAll(tokens);
            }
        }

        IdentityProgression.storeUnlockedIdentityData(nbt, new ArrayList<>(unlocked), variants);
        return true;
    }

    private static Entity resolvePacketTarget(Minecraft client, int entityId) {
        if (client.level != null) {
            Entity entity = client.level.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        if (client.player != null && client.player.getId() == entityId) {
            return client.player;
        }
        return null;
    }

    private static int resolvePrimaryCooldown(Entity identity, IdentityAbilityDefinition identityAbility) {
        if (identityAbility != null) {
            return Math.max(0, identityAbility.cooldown());
        }
        return 20;
    }

    private static int resolveSecondaryCooldown(Entity identity, IdentityAbilityDefinition identityAbility) {
        if (identity != null && identity.getType() == net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN) {
            return Math.max(0, IdentitySettings.elderGuardianMiningFatigueCooldownTicks);
        }
        if (identity != null && identity.getType() == net.minecraft.world.entity.EntityTypes.SHULKER) {
            return Math.max(0, IdentitySettings.shulkerTeleportCooldownTicks);
        }
        if (identityAbility != null) {
            return Math.max(0, identityAbility.cooldown());
        }
        return 20;
    }

    private static void disableMovementInputs(Minecraft client, LocalPlayer player) {
        if (client == null || player == null) {
            return;
        }
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        client.options.keySprint.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUse.setDown(false);
    }

    private static boolean identity2$shouldExitWardenBurrow(Minecraft client) {
        if (client == null) {
            return false;
        }
        return client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyJump.isDown()
                || client.options.keyShift.isDown()
                || client.options.keySprint.isDown()
                || client.options.keyAttack.isDown()
                || client.options.keyUse.isDown()
                || primaryAbilityKeyBinding.consumeClick()
                || secondaryAbilityKeyBinding.consumeClick();
    }

    private static boolean hasPredefFallback(Entity identity) {
        if (identity == null) {
            return false;
        }
        Identifier identityTypeId = net.minecraft.world.entity.EntityType.getKey(identity.getType());
        if (identityTypeId == null) {
            return false;
        }
        return PredefIdentityAbilities.predef.containsKey(identityTypeId)
                || PredefIdentityAbilities.predef
                        .containsKey(Identifier.fromNamespaceAndPath("minecraft", identityTypeId.getPath()))
                || PredefIdentityAbilities.predef
                        .containsKey(Identifier.fromNamespaceAndPath(Identity2.MOD_ID, identityTypeId.getPath()))
                || PredefIdentityAbilities.hasFallbackAbility(identityTypeId);
    }

    private static ItemStack resolveAbilityIconStack(Entity identity, IdentityAbilityDefinition identityAbility) {
        if (identityAbility != null && identityAbility.icon() != null) {
            ItemStack fromDefinition = new ItemStack(identityAbility.icon());
            if (!fromDefinition.isEmpty() && !fromDefinition.is(Items.AIR)) {
                return fromDefinition;
            }
        }
        if (identity != null) {
            ItemStack spawnEggFallback = SpawnEggItem.byId(identity.getType())
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!spawnEggFallback.isEmpty() && !spawnEggFallback.is(Items.AIR)) {
                return spawnEggFallback;
            }
        }
        return new ItemStack(Items.NETHER_STAR);
    }

    private static void processFavoriteKeybinds(LocalPlayer player) {
        while (favoriteSaveSlot1KeyBinding.consumeClick()) {
            saveCurrentMorphToFavoriteSlot(player, 0);
        }
        while (favoriteSaveSlot2KeyBinding.consumeClick()) {
            saveCurrentMorphToFavoriteSlot(player, 1);
        }
        while (favoriteSaveSlot3KeyBinding.consumeClick()) {
            saveCurrentMorphToFavoriteSlot(player, 2);
        }

        while (favoriteMorphSlot1KeyBinding.consumeClick()) {
            morphFavoriteSlot(player, 0);
        }
        while (favoriteMorphSlot2KeyBinding.consumeClick()) {
            morphFavoriteSlot(player, 1);
        }
        while (favoriteMorphSlot3KeyBinding.consumeClick()) {
            morphFavoriteSlot(player, 2);
        }
    }

    private static void tickMorphTransitionEffects(Minecraft client) {
        if (client.level == null || !IdentitySettings.enableMorphTransitionParticles) {
            return;
        }

        for (Entity entity : client.level.players()) {
            if (!MorphTransitionHelper.isTransitionActive(entity, 0.0F)) {
                continue;
            }

            float progress = MorphTransitionHelper.getTransitionProgress(entity, 0.0F);
            double radius = 0.35D + (0.25D * Math.sin(progress * Math.PI));
            for (int i = 0; i < MORPH_TRANSITION_PARTICLES_PER_TICK; i++) {
                double angle = entity.getRandom().nextDouble() * (Math.PI * 2.0D);
                double y = entity.getY() + 0.2D
                        + entity.getRandom().nextDouble() * Math.max(0.2D, entity.getBbHeight() - 0.2D);
                double x = entity.getX() + Math.cos(angle) * radius;
                double z = entity.getZ() + Math.sin(angle) * radius;
                double vx = (entity.getRandom().nextDouble() - 0.5D) * 0.04D;
                double vy = 0.02D + entity.getRandom().nextDouble() * 0.03D;
                double vz = (entity.getRandom().nextDouble() - 0.5D) * 0.04D;
                client.level.addParticle(ParticleTypes.POOF, x, y, z, vx, vy, vz);
            }
        }
    }

    private static void onMorphAcquisition(MorphAcquisitionS2CPacketPayload payload) {
        MorphAcquisitionEffectController.enqueue(payload);
    }

    private static void saveCurrentMorphToFavoriteSlot(LocalPlayer player, int slot) {
        if (slot < 0 || slot >= favoriteIdentityIds.length) {
            return;
        }
        String type = readCurrentIdentityType(player);
        if (type == null || type.isBlank()) {
            player.sendOverlayMessage(Component.literal("No active identity to save in favorite " + (slot + 1)));
            return;
        }
        String variant = readCurrentIdentityVariant(player);
        favoriteIdentityIds[slot] = type;
        favoriteVariantNbt[slot] = variant == null ? "" : variant;
        player.sendOverlayMessage(Component.literal("Favorite " + (slot + 1) + " set to " + type));
    }

    private static void morphFavoriteSlot(LocalPlayer player, int slot) {
        if (slot < 0 || slot >= favoriteIdentityIds.length) {
            return;
        }
        String id = favoriteIdentityIds[slot];
        if (id == null || id.isBlank()) {
            player.sendOverlayMessage(Component.literal("Favorite " + (slot + 1) + " is empty"));
            return;
        }
        String variant = favoriteVariantNbt[slot];
        sendMorphRequest(id, variant == null ? "" : variant);
    }

    private static String readCurrentIdentityType(LocalPlayer player) {
        return ((NbtComponentAccessor) (Object) ((EntityAccessor) player).getCustomData()).getNbt()
                .getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
    }

    private static String readCurrentIdentityVariant(LocalPlayer player) {
        return ((NbtComponentAccessor) (Object) ((EntityAccessor) player).getCustomData()).getNbt()
                .getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
    }

    public static String getFavoriteLabel(int slot) {
        if (slot < 0 || slot >= favoriteIdentityIds.length) {
            return "";
        }
        String id = favoriteIdentityIds[slot];
        if (id == null || id.isBlank()) {
            return "(empty)";
        }
        return id;
    }

    private void applyIdentityFromCustomData(Entity entity) {
        CustomData n = ((EntityAccessor) entity).getCustomData();
        String type = ((NbtComponentAccessor) (Object) n).getNbt()
                .getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (type.isBlank()) {
            type = ((NbtComponentAccessor) (Object) n).getNbt().getStringOr("model_override", "");
        }
        if (type.isBlank()) {
            ((EntityAccessor) entity).setCurrentIdentity("");
            entity.refreshDimensions();
            return;
        }
        String variantRaw = ((NbtComponentAccessor) (Object) n).getNbt()
                .getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        ((EntityAccessor) entity).setCurrentIdentity(type, IdentityProgression.parseVariantNbt(variantRaw));
    }

    private static <T> void enqueuePendingPacket(ArrayList<T> list, T packet) {
        if (list.size() >= MAX_PENDING_PACKET_QUEUE_SIZE) {
            list.remove(0);
        }
        list.add(packet);
    }

    private static void renderIdentityCooldown(GuiGraphicsExtractor matrices, DeltaTracker deltax) {
        float delta = deltax.getGameTimeDeltaPartialTick(false);
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        Window window = client.getWindow();
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = ModRegistries.resolveIdentityAbility(identity.getType());
        boolean hasFallbackPredefAbility = hasPredefFallback(identity);
        if (identityAbility == null && !hasFallbackPredefAbility) {
            return;
        }

        if (client.gui.screen() instanceof ChatScreen) {
            return;
        }

        int cd = ((EntityAccessor) player).getAbilityCooldown();
        int max = Math.max(1, resolvePrimaryCooldown(identity, identityAbility)
                + (identityAbility != null ? identityAbility.useduration() : 0));
        float cooldownScale = Mth.clamp(1 - cd / (float) max, 0.0F, 1.0F);
        double guiScale = client.getWindow().getGuiScale();
        int iconwidth = 17;
        ticksSinceUpdate = 0;
        isFading = false;
        fadingProgress = 0;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        matrices.pose().pushMatrix();
        boolean scissorEnabled = false;
        if (cooldownScale < 1.0F) {
            matrices.enableScissor(
                    0,
                    (int) ((double) height * .92 + iconwidth * (1 - cooldownScale)),
                    (int) ((double) width * guiScale),
                    (int) ((double) height * guiScale));
            scissorEnabled = true;
        }
        ItemStack stack = resolveAbilityIconStack(identity, identityAbility);
        if (stack.isEmpty()) {
            if (scissorEnabled) {
                matrices.disableScissor();
            }
            matrices.pose().popMatrix();
            return;
        }
        matrices.item(stack, (int) (width * .95f), (int) (height * .92f));
        if (scissorEnabled) {
            matrices.disableScissor();
        }

        matrices.pose().popMatrix();

        lastCooldown = Math.round(Mth.lerpInt(delta, cd - 1, cd));
    }

    private static Field getFieldFromClassHeirarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }

    public static EntityModel getModel(Entity e) {
        EntityRenderer idrenderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager()
                .getRenderer(e);

        EntityModel eModel = null;
        if (idrenderer instanceof LivingEntityRenderer) {
            try {
                eModel = ((LivingEntityRenderer) idrenderer).getModel();
            } catch (Exception f) {
                try {
                    eModel = (EntityModel) getFieldFromClassHeirarchy(eModel.getClass(), "model").get((Object) eModel);
                } catch (Exception g) {
                    int x = 0;
                }
            }
        }
        if (idrenderer instanceof EnderDragonRenderer) {
            eModel = ((EnderDragonEntityRendererAccessor) (EnderDragonRenderer) idrenderer).getModel();
        }
        return eModel;
    }

    private record VariantDefinitionKey(Identifier identityId, String variantId) {}
    private record VariantChunkKey(int entityId, Identifier identityId, String variantId) {}
    private static final class PendingVariantDefinition {
        private final byte[][] chunks;
        private final int chunkCount;
        private int received;
        private int totalBytes;
        private PendingVariantDefinition(int count) { this.chunkCount = count; this.chunks = new byte[count][]; }
        private boolean add(int index, byte[] data) {
            if (chunks[index] != null) return true;
            totalBytes += data.length;
            if (totalBytes > IdentityVariantRegistry.MAX_DEFINITION_BYTES) return false;
            chunks[index] = data; received++; return true;
        }
        private boolean complete() { return received == chunks.length; }
        private byte[] join() { byte[] result = new byte[totalBytes]; int offset=0; for(byte[] chunk:chunks) { System.arraycopy(chunk,0,result,offset,chunk.length); offset += chunk.length; } return result; }
    }
}
