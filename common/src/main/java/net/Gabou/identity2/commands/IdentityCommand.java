package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class IdentityCommand {
    private IdentityCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("identity")
                .then(
                    CommandManager.literal("morph")
                        .then(
                            CommandManager.argument("identity_id", IdentifierArgumentType.identifier())
                                .suggests(IdentityCommand::suggestUnlockedIdentities)
                                .executes(context -> morph(context.getSource(), IdentifierArgumentType.getIdentifier(context, "identity_id")))
                        )
                )
                .then(CommandManager.literal("clear").executes(context -> clear(context.getSource())))
                .then(CommandManager.literal("list").executes(context -> list(context.getSource())))
        );
    }

    private static int morph(ServerCommandSource source, Identifier identityId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can morph."));
            return 0;
        }

        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendError(Text.literal("Unsupported identity: " + identityId));
            return 0;
        }

        if (!canSwap(source, player)) {
            source.sendError(Text.literal("Identity swapping is disabled."));
            return 0;
        }

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(source) && !IdentityProgression.isUnlocked(player, identityId)) {
            source.sendError(Text.literal("Identity not unlocked: " + identityId));
            return 0;
        }

        IdentityProgression.morph(player, identityId);
        source.sendFeedback(() -> Text.literal("Morphed into " + identityId), false);
        return 1;
    }

    private static int clear(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can clear morph."));
            return 0;
        }

        if (!canSwap(source, player)) {
            source.sendError(Text.literal("Identity swapping is disabled."));
            return 0;
        }

        IdentityProgression.clearMorph(player);
        source.sendFeedback(() -> Text.literal("Identity cleared."), false);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can list identities."));
            return 0;
        }

        List<String> unlocked = IdentityProgression.getUnlockedIdentities(player).stream()
            .filter(IdentityCommand::isMorphableIdentityString)
            .toList();
        if (unlocked.isEmpty()) {
            source.sendMessage(Text.literal("Unlocked identities: none"));
            return 1;
        }

        source.sendMessage(Text.literal("Unlocked identities (" + unlocked.size() + "): " + String.join(", ", unlocked)));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestUnlockedIdentities(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return Suggestions.empty();
        }

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(context.getSource())) {
            List<Identifier> identifiers = IdentityProgression.getUnlockedIdentities(player).stream()
                .map(IdentityCommand::parseIdentifier)
                .flatMap(Optional::stream)
                .filter(IdentityProgression::isMorphableIdentity)
                .toList();
            return CommandSource.suggestIdentifiers(identifiers, builder);
        }

        return CommandSource.suggestIdentifiers(
            net.minecraft.registry.Registries.ENTITY_TYPE.getIds().stream().filter(IdentityProgression::isMorphableIdentity),
            builder
        );
    }

    private static boolean canSwap(ServerCommandSource source, ServerPlayerEntity player) {
        if (IdentitySettings.enableSwaps) {
            return true;
        }

        if (isOperator(source)) {
            return true;
        }

        return IdentitySettings.allowedSwappers.contains(player.getName().getString());
    }

    private static boolean isOperator(ServerCommandSource source) {
        return CommandManager.ADMINS_CHECK.allows(source.getPermissions());
    }

    private static boolean isMorphableIdentityString(String value) {
        try {
            return IdentityProgression.isMorphableIdentity(Identifier.of(value));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Optional<Identifier> parseIdentifier(String value) {
        try {
            return Optional.of(Identifier.of(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
