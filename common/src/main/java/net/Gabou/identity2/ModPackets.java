package net.Gabou.identity2;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.Gabou.identity2.packets.CustomEntityBoolDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityDataS2CPacketPayload;
import net.Gabou.identity2.packets.CustomEntityStringDataS2CPacketPayload;
import net.Gabou.identity2.packets.IdentityAbilityPacketPayload;
import net.Gabou.identity2.packets.IdentityMorphRequestC2SPacketPayload;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class ModPackets {
    public static final Identifier CUSTOM_STRING_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_string");
    public static final Identifier CUSTOM_DOUBLE_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_double");
    public static final Identifier CUSTOM_BOOL_DATA_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "set_custom_data_bool");
    public static final Identifier IDENTITY_ABILITY_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "entity_ability");
    public static final Identifier IDENTITY_MORPH_REQUEST_PACKET_ID = Identifier.fromNamespaceAndPath(Identity2.MOD_ID, "identity_morph_request");

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
    }

    private static void handleIdentityAbilityPacket(ServerPlayer player, IdentityAbilityPacketPayload payload) {
        Registry<IdentityAbilityDefinition> identityAbilityRegistry = ModRegistries.getIdentityAbilityRegistry();
        if (identityAbilityRegistry == null) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        IdentityAbilityDefinition identityAbility = identityAbilityRegistry.getValue(EntityType.getKey(identity.getType()));
        if (identityAbility == null) {
            return;
        }

        Identifier prebuilt = identityAbility.bultinability();
        if (payload.entityid() == 0) {
            String command = identityAbility.command();
            if (!command.isEmpty() && player.level().getServer() != null) {
                player.level().getServer().getCommands().performPrefixedCommand(
                    player.level().getServer().createCommandSourceStack().withEntity(player),
                    command
                );
            }
            if (!"null".equals(prebuilt.getPath()) && PredefIdentityAbilities.predef.containsKey(prebuilt)) {
                PredefIdentityAbilities.predef.get(prebuilt).execute(player);
            }
            return;
        }

        if ("null".equals(prebuilt.getPath()) || !PredefIdentityAbilities.predef.containsKey(prebuilt)) {
            return;
        }

        if (payload.entityid() == -1 || payload.entityid() == -2) {
            PredefIdentityAbilities.predef.get(prebuilt).passivetick(player, payload.entityid() == -2);
        } else if (payload.entityid() == -3) {
            PredefIdentityAbilities.predef.get(prebuilt).overrideAttack(player);
        } else {
            PredefIdentityAbilities.predef.get(prebuilt).tick(player, payload.entityid());
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

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(player) && !IdentityProgression.isUnlocked(player, identityId)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Identity not unlocked: " + identityId), false);
            return;
        }

        CompoundTag variantNbt = IdentityProgression.parseVariantNbt(payload.variantNbt());
        IdentityProgression.morph(player, identityId, variantNbt);
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
