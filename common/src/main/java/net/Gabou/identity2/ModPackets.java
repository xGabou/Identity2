package net.Gabou.identity2;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.packets.IdentityUnlockSyncS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityVillagerTradeRequestC2SPacketPayload;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.packets.UnlockedIdentitySyncS2CPacketPayload;
import net.Gabou.identity2.api.ability.BuiltinIdentityAbility;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityVariantRegistry;
import net.Gabou.identity2.packets.IdentityVariantDefinitionS2CPacketPayload;
import net.Gabou.identity2.identity.WardenBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ModPackets {
    public static final Identifier CUSTOM_STRING_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_string");
    public static final Identifier CUSTOM_DOUBLE_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_double");
    public static final Identifier CUSTOM_BOOL_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_bool");
    public static final Identifier MORPH_ACQUISITION_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "morph_acquisition");
    public static final Identifier IDENTITY_ABILITY_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "entity_ability");
    public static final Identifier IDENTITY_MORPH_REQUEST_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "identity_morph_request");
    public static final Identifier UNLOCK_SYNC_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "unlock_sync");
    public static final Identifier IDENTITY_VARIANT_DEFINITION_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "identity_variant_definition");
    public static final Identifier IDENTITY_VILLAGER_TRADE_REQUEST_PACKET_ID = Identifier.fromNamespaceAndPath(
        Identity2.MOD_ID,
        "identity_villager_trade_request"
    );
    public static final Identifier UNLOCKED_IDENTITY_SYNC_PACKET_ID = Identifier.fromNamespaceAndPath(
        Identity2.MOD_ID,
        "unlocked_identity_sync"
    );
    public static final int ABILITY_ACTION_PRIMARY = 0;
    public static final int ABILITY_ACTION_SECONDARY = -4;
    public static final int ABILITY_ACTION_OVERRIDE_ATTACK = -3;
    public static final int ABILITY_ACTION_PASSIVE = -1;
    public static final int ABILITY_ACTION_PASSIVE_USED = -2;
    private static final long SLOW_ABILITY_PACKET_THRESHOLD_NS = TimeUnit.MILLISECONDS.toNanos(2L);

    private static boolean initialized = false;

    private ModPackets() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(CustomEntityDataS2CPacketPayload.ID, CustomEntityDataS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(CustomEntityStringDataS2CPacketPayload.ID, CustomEntityStringDataS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(CustomEntityBoolDataS2CPacketPayload.ID, CustomEntityBoolDataS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(IdentityUnlockSyncS2CPacketPayload.ID, IdentityUnlockSyncS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(IdentityVariantDefinitionS2CPacketPayload.ID, IdentityVariantDefinitionS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(MorphAcquisitionS2CPacketPayload.ID, MorphAcquisitionS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(UnlockedIdentitySyncS2CPacketPayload.ID, UnlockedIdentitySyncS2CPacketPayload.CODEC);
        }


        NetworkManager.registerReceiver(
            NetworkManager.c2s(),
            IdentityAbilityPacketPayload.ID,
            IdentityAbilityPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleIdentityAbilityPacket(player, payload);
                }
            })
        );

        NetworkManager.registerReceiver(
            NetworkManager.c2s(),
            IdentityMorphRequestC2SPacketPayload.ID,
            IdentityMorphRequestC2SPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleMorphRequestPacket(player, payload);
                }
            })
        );

        NetworkManager.registerReceiver(
            NetworkManager.c2s(),
            IdentityVillagerTradeRequestC2SPacketPayload.ID,
            IdentityVillagerTradeRequestC2SPacketPayload.CODEC,
            (payload, context) -> context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleVillagerTradeRequestPacket(player, payload);
                }
            })
        );

    }

    private static void handleIdentityAbilityPacket(ServerPlayer player, IdentityAbilityPacketPayload payload) {
        long startNanos = System.nanoTime();
        try {
            Entity identity = ((EntityAccessor) player).getCurrentIdentity();
            if (identity == null) {
                return;
            }

            IdentityAbilityDefinition identityAbility = ModRegistries.resolveIdentityAbility(identity.getType());
            String command = "";
            Identifier prebuilt = EntityType.getKey(identity.getType());
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
                EntityAccessor accessor = (EntityAccessor) player;
                if (accessor.getSecondaryAbilityCooldown() > 0 && !WardenBurrowManager.isHidden(player)) {
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
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            if (elapsedNanos >= SLOW_ABILITY_PACKET_THRESHOLD_NS && Identity2.LOGGER.isDebugEnabled()) {
                Entity identity = ((EntityAccessor) player).getCurrentIdentity();
                Identity2.LOGGER.debug(
                    "Slow identity ability packet: player={}, identity={}, action={}, took {} us",
                    player.getGameProfile().name(),
                    identity == null ? "null" : EntityType.getKey(identity.getType()),
                    payload.entityid(),
                    elapsedNanos / 1_000L
                );
            }
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
            if (identity.getType() == net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN) {
                return Math.max(0, IdentitySettings.elderGuardianMiningFatigueCooldownTicks);
            }
            if (identity.getType() == net.minecraft.world.entity.EntityTypes.SHULKER) {
                return Math.max(0, IdentitySettings.shulkerTeleportCooldownTicks);
            }
        }
        if (identityAbility != null) {
            return Math.max(0, identityAbility.cooldown());
        }
        return 20;
    }

    private static BuiltinIdentityAbility resolvePredefAbility(Identifier prebuilt, Identifier identityTypeId) {
        if (prebuilt == null || "null".equals(prebuilt.getPath())) {
            return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
        }

        BuiltinIdentityAbility exact = PredefIdentityAbilities.predef.get(prebuilt);
        if (exact != null) {
            return exact;
        }

        Identifier minecraftAlias = Identifier.fromNamespaceAndPath("minecraft", prebuilt.getPath());
        BuiltinIdentityAbility minecraft = PredefIdentityAbilities.predef.get(minecraftAlias);
        if (minecraft != null) {
            return minecraft;
        }

        BuiltinIdentityAbility identity2Alias = PredefIdentityAbilities.predef.get(
            Identifier.fromNamespaceAndPath(Identity2.MOD_ID, prebuilt.getPath())
        );
        if (identity2Alias != null) {
            return identity2Alias;
        }

        return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
    }

    private static void handleMorphRequestPacket(ServerPlayer player, IdentityMorphRequestC2SPacketPayload payload) {
        if (!canSwap(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Identity swapping is disabled."));
            return;
        }

        String requested = payload.identityId();
        if (requested == null || requested.isBlank()) {
            IdentityProgression.clearMorph(player);
            return;
        }

        Identifier identityId;
        try {
            identityId = Identifier.parse(requested);
        } catch (Exception exception) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown identity: " + requested));
            return;
        }

        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            if (IdentityProgression.isIdentityTemporarilyDisabled(identityId)) {
                String reason = IdentityProgression.getDisabledIdentityReason(identityId);
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "Identity disabled after load failure: " + identityId + (reason.isBlank() ? "" : " (" + reason + ")")
                    )
                );
                return;
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unsupported identity: " + identityId));
            return;
        }

        CompoundTag variantNbt = IdentityVariantRegistry.resolve(player, identityId, payload.variantId());
        if (variantNbt == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unknown identity variant reference."));
            IdentityProgression.syncUnlockedIdentities(player);
            return;
        }
        if (IdentityProgression.shouldEnforceIdentityUnlocksForMorph() && !isOperator(player)) {
            if (!IdentityProgression.isUnlocked(player, identityId)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Identity not unlocked: " + identityId));
                return;
            }
            if (!IdentityProgression.isVariantUnlocked(player, identityId, variantNbt)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Identity variant not unlocked: " + identityId));
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
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("Self villager trading is disabled."));
            return;
        }

        if (target.level() != level) {
            requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("Target player is in another dimension."));
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
        requester.sendSystemMessage(net.minecraft.network.chat.Component.literal("Target is not morphed as a villager."));
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
        return Commands.LEVEL_ADMINS.check(player.createCommandSourceStack().permissions());
    }
}
