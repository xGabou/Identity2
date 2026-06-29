package net.Gabou.identity2;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.Gabou.gaboulibs.auth.C2SChallengeReplyPacket;
import net.Gabou.gaboulibs.auth.ServerAuth;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.packets.IdentityUnlockSyncS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityVillagerTradeRequestC2SPacketPayload;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.packets.OpenProgressionScreenS2CPacketPayload;
import net.Gabou.identity2.packets.ProgressionChargeSyncRequestC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarSelectC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarStateS2CPacketPayload;
import net.Gabou.identity2.packets.ProgressionJarTransferC2SPacketPayload;
import net.Gabou.identity2.packets.ProgressionPlayerChargesS2CPacketPayload;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.progression.MorphChargeManager;
import net.Gabou.identity2.progression.ProgressionUiSync;
import net.Gabou.identity2.progression.SoulJarChargeStorage;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.Gabou.identity2.util.NetworkCompat;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModPackets {
    public static final ResourceLocation CUSTOM_STRING_DATA_ID = new ResourceLocation(Identity2.MOD_ID, "set_custom_data_string");
    public static final ResourceLocation CUSTOM_DOUBLE_DATA_ID = new ResourceLocation(Identity2.MOD_ID, "set_custom_data_double");
    public static final ResourceLocation CUSTOM_BOOL_DATA_ID = new ResourceLocation(Identity2.MOD_ID, "set_custom_data_bool");
    public static final ResourceLocation MORPH_ACQUISITION_PACKET_ID = new ResourceLocation(Identity2.MOD_ID, "morph_acquisition");
    public static final ResourceLocation IDENTITY_ABILITY_PACKET_ID = new ResourceLocation(Identity2.MOD_ID, "entity_ability");
    public static final ResourceLocation IDENTITY_MORPH_REQUEST_PACKET_ID = new ResourceLocation(Identity2.MOD_ID, "identity_morph_request");
    public static final ResourceLocation UNLOCK_SYNC_PACKET_ID = new ResourceLocation(Identity2.MOD_ID, "unlock_sync");
    public static final ResourceLocation IDENTITY_VILLAGER_TRADE_REQUEST_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "identity_villager_trade_request"
    );
    public static final ResourceLocation OPEN_PROGRESSION_SCREEN_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "open_progression_screen"
    );
    public static final ResourceLocation PROGRESSION_CHARGE_SYNC_REQUEST_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "progression_charge_sync_request"
    );
    public static final ResourceLocation PROGRESSION_JAR_SELECT_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "progression_jar_select"
    );
    public static final ResourceLocation PROGRESSION_JAR_TRANSFER_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "progression_jar_transfer"
    );
    public static final ResourceLocation PROGRESSION_PLAYER_CHARGES_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "progression_player_charges"
    );
    public static final ResourceLocation PROGRESSION_JAR_STATE_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "progression_jar_state"
    );
    public static final ResourceLocation AUTH_CHALLENGE_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "auth_challenge"
    );
    public static final ResourceLocation AUTH_CHALLENGE_REPLY_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "auth_challenge_reply"
    );
    public static final ResourceLocation UNLOCKED_IDENTITY_SYNC_PACKET_ID = new ResourceLocation(
        Identity2.MOD_ID,
        "unlocked_identity_sync"
    );
    public static final int ABILITY_ACTION_PRIMARY = 0;
    public static final int ABILITY_ACTION_SECONDARY = -4;
    public static final int ABILITY_ACTION_OVERRIDE_ATTACK = -3;
    public static final int ABILITY_ACTION_PASSIVE = -1;
    public static final int ABILITY_ACTION_PASSIVE_USED = -2;

    private static final Set<String> loggedResolvedPredefDebug = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedMissingPredefWarnings = ConcurrentHashMap.newKeySet();
    private static boolean initialized = false;

    private ModPackets() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (Platform.getEnvironment() == Env.SERVER) {
        }

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            C2SChallengeReplyPacket.ID,
            C2SChallengeReplyPacket::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    ServerAuth.handleChallengeReply(player, payload);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            IdentityAbilityPacketPayload.ID,
            IdentityAbilityPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleIdentityAbilityPacket(player, payload);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            IdentityMorphRequestC2SPacketPayload.ID,
            IdentityMorphRequestC2SPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleMorphRequestPacket(player, payload);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            IdentityVillagerTradeRequestC2SPacketPayload.ID,
            IdentityVillagerTradeRequestC2SPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleVillagerTradeRequestPacket(player, payload);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            ProgressionChargeSyncRequestC2SPacketPayload.ID,
            ProgressionChargeSyncRequestC2SPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    ProgressionUiSync.sendPlayerCharges(player);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            ProgressionJarSelectC2SPacketPayload.ID,
            ProgressionJarSelectC2SPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleProgressionJarSelect(player, payload);
                }
            })
        );

        NetworkCompat.registerReceiver(
            NetworkManager.c2s(),
            ProgressionJarTransferC2SPacketPayload.ID,
            ProgressionJarTransferC2SPacketPayload::decode,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleProgressionJarTransfer(player, payload);
                }
            })
        );
    }

    private static void handleIdentityAbilityPacket(ServerPlayer player, IdentityAbilityPacketPayload payload) {
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = ModRegistries.resolveIdentityAbility(identity.getType());
        String command = "";
        ResourceLocation prebuilt = EntityType.getKey(identity.getType());
        if (identityAbility != null) {
            command = identityAbility.command();
            prebuilt = identityAbility.bultinability();
        }

        BuiltinIdentityAbility predefAbility = resolvePredefAbility(prebuilt, EntityType.getKey(identity.getType()));
        if (payload.entityid() == ABILITY_ACTION_PRIMARY) {
            int configuredCooldown = resolvePrimaryAbilityCooldown(identity, identityAbility);
            EntityAccessor accessor = (EntityAccessor) player;
            if (accessor.getAbilityCooldown() > 0) {
                return;
            }
            accessor.setAbilityCooldown(configuredCooldown);
            if (!command.isEmpty() && player.level().getServer() != null) {
                player.level().getServer().getCommands().performPrefixedCommand(
                    player.level().getServer().createCommandSourceStack().withEntity(player),
                    command
                );
            }
            if (predefAbility != null) {
                predefAbility.execute(player);
            }
            return;
        }

        if (payload.entityid() == ABILITY_ACTION_SECONDARY) {
            if (predefAbility == null) {
                return;
            }
            if (identity.getType() == EntityType.WARDEN) {
                return;
            }
            EntityAccessor accessor = (EntityAccessor) player;
            if (accessor.getSecondaryAbilityCooldown() > 0) {
                return;
            }
            accessor.setSecondaryAbilityCooldown(resolveSecondaryAbilityCooldown(identity, identityAbility));
            predefAbility.executeSecondary(player);
            return;
        }

        if (predefAbility == null) {
            return;
        }

        if (payload.entityid() == ABILITY_ACTION_PASSIVE || payload.entityid() == ABILITY_ACTION_PASSIVE_USED) {
            predefAbility.passiveTick(player, payload.entityid() == ABILITY_ACTION_PASSIVE_USED);
        } else if (payload.entityid() == ABILITY_ACTION_OVERRIDE_ATTACK) {
            predefAbility.overrideAttack(player);
        } else {
            predefAbility.tick(player, payload.entityid());
        }
    }

    private static int resolvePrimaryAbilityCooldown(Entity identity, IdentityAbilityDefinition identityAbility) {
        if (identityAbility != null) {
            return Math.max(0, identityAbility.cooldown() + identityAbility.useduration());
        }
        return 20;
    }

    private static int resolveSecondaryAbilityCooldown(Entity identity, IdentityAbilityDefinition identityAbility) {
        if (identity != null) {
            if (identity.getType() == EntityType.ELDER_GUARDIAN) {
                return Math.max(0, IdentitySettings.elderGuardianMiningFatigueCooldownTicks);
            }
            if (identity.getType() == EntityType.SHULKER) {
                return Math.max(0, IdentitySettings.shulkerTeleportCooldownTicks);
            }
        }
        if (identityAbility != null) {
            return Math.max(0, identityAbility.cooldown());
        }
        return 20;
    }

    private static BuiltinIdentityAbility resolvePredefAbility(ResourceLocation prebuilt, ResourceLocation identityTypeId) {
        if (prebuilt == null || "null".equals(prebuilt.getPath())) {
            return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
        }

        BuiltinIdentityAbility exact = PredefIdentityAbilities.predef.get(prebuilt);
        if (exact != null) {
            logResolvedPredef(identityTypeId, prebuilt, prebuilt);
            return exact;
        }

        ResourceLocation minecraftAlias = new ResourceLocation("minecraft", prebuilt.getPath());
        BuiltinIdentityAbility minecraft = PredefIdentityAbilities.predef.get(minecraftAlias);
        if (minecraft != null) {
            logResolvedPredef(identityTypeId, prebuilt, minecraftAlias);
            return minecraft;
        }

        ResourceLocation identity2AliasId = new ResourceLocation(Identity2.MOD_ID, prebuilt.getPath());
        BuiltinIdentityAbility identity2Alias = PredefIdentityAbilities.predef.get(identity2AliasId);
        if (identity2Alias != null) {
            logResolvedPredef(identityTypeId, prebuilt, identity2AliasId);
            return identity2Alias;
        }

        logMissingPredef(identityTypeId, prebuilt);
        return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
    }

    private static void logResolvedPredef(ResourceLocation identityTypeId, ResourceLocation requestedPredefId, ResourceLocation resolvedPredefId) {
        String key = identityTypeId + "->" + requestedPredefId + "->" + resolvedPredefId;
        if (loggedResolvedPredefDebug.add(key)) {
            Identity2.LOGGER.debug(
                "Resolved builtin identity ability for {} using predef {} via {}.",
                identityTypeId,
                requestedPredefId,
                resolvedPredefId
            );
        }
    }

    private static void logMissingPredef(ResourceLocation identityTypeId, ResourceLocation requestedPredefId) {
        String key = identityTypeId + "->" + requestedPredefId;
        if (loggedMissingPredefWarnings.add(key)) {
            Identity2.LOGGER.warn(
                "No builtin identity ability is registered for predef {} while resolving {}. Falling back to the generic identity ability.",
                requestedPredefId,
                identityTypeId
            );
        }
    }

    private static void handleMorphRequestPacket(ServerPlayer player, IdentityMorphRequestC2SPacketPayload payload) {
        if (!canSwap(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Identity swapping is disabled."), false);
            return;
        }

        String requested = payload.identityId();
        if (requested == null || requested.isBlank()) {
            IdentityProgression.clearMorph(player);
            return;
        }

        ResourceLocation identityId;
        try {
            identityId = new ResourceLocation(requested);
        } catch (Exception exception) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unknown identity: " + requested), false);
            return;
        }

        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            if (IdentityProgression.isIdentityTemporarilyDisabled(identityId)) {
                String reason = IdentityProgression.getDisabledIdentityReason(identityId);
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "Identity disabled after load failure: " + identityId + (reason.isBlank() ? "" : " (" + reason + ")")
                    ),
                    false
                );
                return;
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unsupported identity: " + identityId), false);
            return;
        }

        CompoundTag variantNbt = IdentityProgression.parseVariantNbt(payload.variantNbt());
        if (IdentityProgression.shouldEnforceIdentityUnlocksForMorph() && !isOperator(player)) {
            if (!IdentityProgression.isUnlocked(player, identityId)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Identity not unlocked: " + identityId), false);
                return;
            }
            if (!IdentityProgression.isVariantUnlocked(player, identityId, variantNbt)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Identity variant not unlocked: " + identityId), false);
                return;
            }
        }

        IdentityProgression.morph(player, identityId, variantNbt);
    }

    private static void handleVillagerTradeRequestPacket(ServerPlayer requester, IdentityVillagerTradeRequestC2SPacketPayload payload) {
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(payload.targetUuid());
        } catch (Exception ignored) {
            return;
        }

        Level level = requester.level();
        if(level == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) return;

        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target == null) return;

        if (target == requester && !IdentitySettings.canTradeWithHimSelf) {
            requester.displayClientMessage(net.minecraft.network.chat.Component.literal("Self villager trading is disabled."), false);
            return;
        }

        if (target.level() != level) {
            requester.displayClientMessage(net.minecraft.network.chat.Component.literal("Target player is in another dimension."), false);
            return;
        }

        Entity identity = ((EntityAccessor) target).getCurrentIdentity();
        if (identity instanceof Villager villagerIdentity) {
            villagerIdentity.mobInteract(requester, InteractionHand.MAIN_HAND);
            return;
        }
        if (identity instanceof WanderingTrader traderIdentity) {
            traderIdentity.mobInteract(requester, InteractionHand.MAIN_HAND);
            return;
        }
        requester.displayClientMessage(net.minecraft.network.chat.Component.literal("Target is not morphed as a villager."), false);
    }

    private static void handleProgressionJarSelect(ServerPlayer player, ProgressionJarSelectC2SPacketPayload payload) {
        if (player == null) {
            return;
        }

        int slotIndex = payload.slotIndex();
        if (slotIndex < 0) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Jar removed.");
            return;
        }

        Inventory inventory = player.getInventory();
        if (slotIndex >= inventory.getContainerSize()) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Invalid inventory slot.");
            return;
        }

        ItemStack stack = inventory.getItem(slotIndex);
        SoulJarChargeStorage.JarSnapshot snapshot = SoulJarChargeStorage.ensureInitialized(
            stack,
            "jar_" + player.getUUID().toString().substring(0, 8) + "_" + slotIndex,
            ""
        );
        if (snapshot == null) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Selected item is not a Soul Jar.");
            return;
        }
        inventory.setChanged();

        ProgressionUiSync.sendJarState(player, slotIndex, snapshot.jarId(), snapshot.tier(), snapshot.charges(), "Jar selected.");
    }

    private static void handleProgressionJarTransfer(ServerPlayer player, ProgressionJarTransferC2SPacketPayload payload) {
        if (player == null) {
            return;
        }
        int slotIndex = payload.slotIndex();
        int amount = payload.amount();
        if (slotIndex < 0 || amount <= 0) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Invalid transfer request.");
            return;
        }

        ResourceLocation identityId;
        try {
            identityId = new ResourceLocation(payload.identityId());
        } catch (Exception exception) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Invalid morph id: " + payload.identityId());
            return;
        }
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Unsupported morph: " + identityId);
            return;
        }

        Inventory inventory = player.getInventory();
        if (slotIndex >= inventory.getContainerSize()) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Invalid inventory slot.");
            return;
        }

        ItemStack stack = inventory.getItem(slotIndex);
        SoulJarChargeStorage.JarSnapshot snapshot = SoulJarChargeStorage.ensureInitialized(
            stack,
            "jar_" + player.getUUID().toString().substring(0, 8) + "_" + slotIndex,
            ""
        );
        if (snapshot == null) {
            ProgressionUiSync.sendJarState(player, -1, "", "", Map.of(), "Selected item is not a Soul Jar.");
            return;
        }

        Map<String, Integer> jarCharges = new HashMap<>(snapshot.charges());
        String key = identityId.toString();
        int jarAvailable = Math.max(0, jarCharges.getOrDefault(key, 0));

        if (payload.deposit()) {
            if (!MorphChargeManager.tryRemoveCharges(player, identityId, amount)) {
                ProgressionUiSync.sendJarState(player, slotIndex, snapshot.jarId(), snapshot.tier(), jarCharges, "Not enough player charges.");
                return;
            }
            jarCharges.put(key, jarAvailable + amount);
            if (!SoulJarChargeStorage.writeCharges(stack, jarCharges)) {
                MorphChargeManager.addCharges(player, identityId, amount);
                ProgressionUiSync.sendJarState(player, slotIndex, snapshot.jarId(), snapshot.tier(), snapshot.charges(), "Could not write jar charges.");
                return;
            }
            inventory.setChanged();
            ProgressionUiSync.sendJarState(
                player,
                slotIndex,
                snapshot.jarId(),
                snapshot.tier(),
                jarCharges,
                "Deposited " + amount + " charge(s) into jar."
            );
            return;
        }

        if (jarAvailable < amount) {
            ProgressionUiSync.sendJarState(player, slotIndex, snapshot.jarId(), snapshot.tier(), jarCharges, "Jar does not have enough charges.");
            return;
        }

        int next = jarAvailable - amount;
        if (next > 0) {
            jarCharges.put(key, next);
        } else {
            jarCharges.remove(key);
        }
        if (!SoulJarChargeStorage.writeCharges(stack, jarCharges)) {
            ProgressionUiSync.sendJarState(player, slotIndex, snapshot.jarId(), snapshot.tier(), snapshot.charges(), "Could not write jar charges.");
            return;
        }
        inventory.setChanged();
        MorphChargeManager.addCharges(player, identityId, amount);
        ProgressionUiSync.sendJarState(
            player,
            slotIndex,
            snapshot.jarId(),
            snapshot.tier(),
            jarCharges,
            "Withdrew " + amount + " charge(s) from jar."
        );
    }

    private static boolean canSwap(ServerPlayer player) {
        if (IdentitySettings.enableSwaps) {
            return true;
        }
        if (isOperator(player)) {
            return true;
        }
        return IdentitySettings.allowedSwappers.contains(player.getName().getString());
    }

    private static boolean isOperator(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(Commands.LEVEL_ADMINS);
    }
}

