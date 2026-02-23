package net.Gabou.identity2;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.packets.IdentityVillagerTradeRequestC2SPacketPayload;
import net.Gabou.identity2.packets.MorphAcquisitionS2CPacketPayload;
import net.Gabou.identity2.packets.OpenProgressionScreenS2CPacketPayload;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
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

public final class ModPackets {
    public static final Identifier CUSTOM_STRING_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_string");
    public static final Identifier CUSTOM_DOUBLE_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_double");
    public static final Identifier CUSTOM_BOOL_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_bool");
    public static final Identifier MORPH_ACQUISITION_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "morph_acquisition");
    public static final Identifier IDENTITY_ABILITY_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "entity_ability");
    public static final Identifier IDENTITY_MORPH_REQUEST_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "identity_morph_request");
    public static final Identifier IDENTITY_VILLAGER_TRADE_REQUEST_PACKET_ID = Identifier.fromNamespaceAndPath(
        Identity2.MOD_ID,
        "identity_villager_trade_request"
    );
    public static final Identifier OPEN_PROGRESSION_SCREEN_PACKET_ID = Identifier.fromNamespaceAndPath(
        Identity2.MOD_ID,
        "open_progression_screen"
    );
    public static final int ABILITY_ACTION_PRIMARY = 0;
    public static final int ABILITY_ACTION_SECONDARY = -4;
    public static final int ABILITY_ACTION_OVERRIDE_ATTACK = -3;
    public static final int ABILITY_ACTION_PASSIVE = -1;
    public static final int ABILITY_ACTION_PASSIVE_USED = -2;

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
            NetworkManager.registerS2CPayloadType(MorphAcquisitionS2CPacketPayload.ID, MorphAcquisitionS2CPacketPayload.CODEC);
            NetworkManager.registerS2CPayloadType(OpenProgressionScreenS2CPacketPayload.ID, OpenProgressionScreenS2CPacketPayload.CODEC);
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

        PredefIdentityAbilities.IdentityAbility predefAbility = resolvePredefAbility(prebuilt, EntityType.getKey(identity.getType()));
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
            predefAbility.passivetick(player, payload.entityid() == ABILITY_ACTION_PASSIVE_USED);
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

    private static PredefIdentityAbilities.IdentityAbility resolvePredefAbility(Identifier prebuilt, Identifier identityTypeId) {
        if (prebuilt == null || "null".equals(prebuilt.getPath())) {
            return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
        }

        PredefIdentityAbilities.IdentityAbility exact = PredefIdentityAbilities.predef.get(prebuilt);
        if (exact != null) {
            return exact;
        }

        Identifier minecraftAlias = Identifier.fromNamespaceAndPath("minecraft", prebuilt.getPath());
        PredefIdentityAbilities.IdentityAbility minecraft = PredefIdentityAbilities.predef.get(minecraftAlias);
        if (minecraft != null) {
            return minecraft;
        }

        PredefIdentityAbilities.IdentityAbility identity2Alias = PredefIdentityAbilities.predef.get(
            Identifier.fromNamespaceAndPath(Identity2.MOD_ID, prebuilt.getPath())
        );
        if (identity2Alias != null) {
            return identity2Alias;
        }

        return PredefIdentityAbilities.resolveFallbackAbility(identityTypeId);
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

        Identifier identityId;
        try {
            identityId = Identifier.parse(requested);
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
        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(player)) {
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
