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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class IdentityCommand {
    private IdentityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("identity")
                .then(
                    Commands.literal("morph")
                        .then(
                            Commands.argument("identity_id", IdentifierArgument.id())
                                .suggests(IdentityCommand::suggestUnlockedIdentities)
                                .executes(context -> morph(context.getSource(), IdentifierArgument.getId(context, "identity_id")))
                        )
                )
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
        );
    }

    private static int morph(CommandSourceStack source, Identifier identityId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can morph."));
            return 0;
        }

        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            if (IdentityProgression.isIdentityTemporarilyDisabled(identityId)) {
                String reason = IdentityProgression.getDisabledIdentityReason(identityId);
                source.sendFailure(
                    Component.literal("Identity disabled after load failure: " + identityId + (reason.isBlank() ? "" : " (" + reason + ")"))
                );
                return 0;
            }
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }

        if (!canSwap(source, player)) {
            source.sendFailure(Component.literal("Identity swapping is disabled."));
            return 0;
        }

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(source) && !IdentityProgression.isUnlocked(player, identityId)) {
            source.sendFailure(Component.literal("Identity not unlocked: " + identityId));
            return 0;
        }

        IdentityProgression.morph(player, identityId);
        source.sendSuccess(() -> Component.literal("Morphed into " + identityId), false);
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can clear morph."));
            return 0;
        }

        if (!canSwap(source, player)) {
            source.sendFailure(Component.literal("Identity swapping is disabled."));
            return 0;
        }

        IdentityProgression.clearMorph(player);
        source.sendSuccess(() -> Component.literal("Identity cleared."), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can list identities."));
            return 0;
        }

        List<String> unlocked = IdentityProgression.getUnlockedIdentities(player).stream()
            .filter(IdentityCommand::isMorphableIdentityString)
            .toList();
        if (unlocked.isEmpty()) {
            source.sendSystemMessage(Component.literal("Unlocked identities: none"));
            return 1;
        }

        source.sendSystemMessage(Component.literal("Unlocked identities (" + unlocked.size() + "): " + String.join(", ", unlocked)));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestUnlockedIdentities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return Suggestions.empty();
        }

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(context.getSource())) {
            List<Identifier> identifiers = IdentityProgression.getUnlockedIdentities(player).stream()
                .map(IdentityCommand::parseIdentifier)
                .flatMap(Optional::stream)
                .filter(IdentityProgression::isMorphableIdentity)
                .toList();
            return SharedSuggestionProvider.suggestResource(identifiers, builder);
        }

        return SharedSuggestionProvider.suggestResource(
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(IdentityProgression::isMorphableIdentity),
            builder
        );
    }

    private static boolean canSwap(CommandSourceStack source, ServerPlayer player) {
        if (IdentitySettings.enableSwaps) {
            return true;
        }

        if (isOperator(source)) {
            return true;
        }

        return IdentitySettings.allowedSwappers.contains(player.getName().getString());
    }

    private static boolean isOperator(CommandSourceStack source) {
        return Commands.LEVEL_ADMINS.check(source.permissions());
    }

    private static boolean isMorphableIdentityString(String value) {
        try {
            return IdentityProgression.isMorphableIdentity(Identifier.parse(value));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Optional<Identifier> parseIdentifier(String value) {
        try {
            return Optional.of(Identifier.parse(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
