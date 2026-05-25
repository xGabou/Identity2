package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.ModRegistries;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.config.IdentityConfigManager;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.IdentityAbilityDefinition;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;

public final class IdentityCommand {
    private static final Set<String> HIDDEN_CONFIG_KEYS = Set.of(
            "EnableMorphCharges",
            "EnableSoulJars",
            "EnablePermanentJarMorphs",
            "EnableSoulAbsorption",
            "disableMorphLossOnDeath"
    );
    private static final Map<String, Field> CONFIG_FIELDS = createConfigFieldMap();

    private IdentityCommand() {
    }

    private static void identity2$sendCommandFeedback(CommandSourceStack source, Component message) {
        ServerPlayer player = source.getPlayer();
        if (IdentitySettings.logCommands && player != null) {
            player.displayClientMessage(message, true);
            return;
        }
        source.sendSystemMessage(message);
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
                                                        .then(
                                                                Commands.argument("variant", StringArgumentType.greedyString())
                                                                        .executes(
                                                                                context -> morph(
                                                                                        context.getSource(),
                                                                                        IdentifierArgument.getId(context, "identity_id"),
                                                                                        StringArgumentType.getString(context, "variant")
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(
                                Commands.literal("unlock")
                                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                        .then(
                                                Commands.argument("identity_id", IdentifierArgument.id())
                                                        .suggests(IdentityCommand::suggestMorphableIdentities)
                                                        .executes(context -> unlockIdentity(context.getSource(), IdentifierArgument.getId(context, "identity_id"), null))
                                                        .then(
                                                                Commands.argument("target", EntityArgument.player())
                                                                        .executes(
                                                                                context -> unlockIdentity(
                                                                                        context.getSource(),
                                                                                        IdentifierArgument.getId(context, "identity_id"),
                                                                                        EntityArgument.getPlayer(context, "target")
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("all")
                                                        .executes(context -> unlockAll(context.getSource(), null))
                                                        .then(
                                                                Commands.argument("target", EntityArgument.player())
                                                                        .executes(context -> unlockAll(context.getSource(), EntityArgument.getPlayer(context, "target")))
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("ability")
                                        .then(Commands.literal("list").executes(context -> listAbilities(context.getSource())))
                                        .then(
                                                Commands.literal("info")
                                                        .then(
                                                                Commands.argument("identity_id", IdentifierArgument.id())
                                                                        .suggests(IdentityCommand::suggestMorphableIdentities)
                                                                        .executes(context -> abilityInfo(context.getSource(), IdentifierArgument.getId(context, "identity_id")))
                                                        )
                                        )
                                        .then(Commands.literal("current").executes(context -> currentAbilityInfo(context.getSource())))
                        )
                        .then(
                                Commands.literal("config")
                                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                        .then(Commands.literal("list").executes(context -> listConfig(context.getSource())))
                                        .then(
                                                Commands.literal("get")
                                                        .then(
                                                                Commands.argument("key", StringArgumentType.word())
                                                                        .suggests(IdentityCommand::suggestConfigKeys)
                                                                        .executes(context -> getConfig(context.getSource(), StringArgumentType.getString(context, "key")))
                                                        )
                                        )
                                        .then(
                                                Commands.literal("set")
                                                        .then(
                                                                Commands.argument("key", StringArgumentType.word())
                                                                        .suggests(IdentityCommand::suggestConfigKeys)
                                                                        .then(
                                                                                Commands.argument("value", StringArgumentType.greedyString())
                                                                                        .executes(
                                                                                                context -> setConfig(
                                                                                                        context.getSource(),
                                                                                                        StringArgumentType.getString(context, "key"),
                                                                                                        StringArgumentType.getString(context, "value")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("add")
                                                        .then(
                                                                Commands.argument("key", StringArgumentType.word())
                                                                        .suggests(IdentityCommand::suggestConfigKeys)
                                                                        .then(
                                                                                Commands.argument("value", StringArgumentType.greedyString())
                                                                                        .executes(
                                                                                                context -> addConfigListValue(
                                                                                                        context.getSource(),
                                                                                                        StringArgumentType.getString(context, "key"),
                                                                                                        StringArgumentType.getString(context, "value")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument("key", StringArgumentType.word())
                                                                        .suggests(IdentityCommand::suggestConfigKeys)
                                                                        .then(
                                                                                Commands.argument("value", StringArgumentType.greedyString())
                                                                                        .executes(
                                                                                                context -> removeConfigListValue(
                                                                                                        context.getSource(),
                                                                                                        StringArgumentType.getString(context, "key"),
                                                                                                        StringArgumentType.getString(context, "value")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("clear")
                                                        .then(
                                                                Commands.argument("key", StringArgumentType.word())
                                                                        .suggests(IdentityCommand::suggestConfigKeys)
                                                                        .executes(context -> clearConfigList(context.getSource(), StringArgumentType.getString(context, "key")))
                                                        )
                                        )
                        )
                        .then(IdentityProgressionCommand.progressionSubcommand())
        );

        dispatcher.register(
                Commands.literal("identity_villager")
                        .then(
                                Commands.literal("trade")
                                        .then(Commands.literal("myself").executes(context -> tradeVillagerMyself(context.getSource())))
                                        .then(
                                                Commands.argument("target", EntityArgument.player())
                                                        .executes(context -> tradeVillagerTarget(context.getSource(), EntityArgument.getPlayer(context, "target")))
                                        )
                        )
        );
    }

    private static int tradeVillagerMyself(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can use villager trade."));
            return 0;
        }
        if (!IdentitySettings.canTradeWithHimSelf) {
            source.sendFailure(Component.literal("Self villager trading is disabled."));
            return 0;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity instanceof Villager villagerIdentity) {
            villagerIdentity.mobInteract(player, InteractionHand.MAIN_HAND);
            return 1;
        }
        if (identity instanceof WanderingTrader traderIdentity) {
            traderIdentity.mobInteract(player, InteractionHand.MAIN_HAND);
            return 1;
        }

        source.sendFailure(Component.literal("You must be morphed as a villager to self-trade."));
        return 0;
    }

    private static int tradeVillagerTarget(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer requester = source.getPlayer();
        if (requester == null) {
            source.sendFailure(Component.literal("Only players can use villager trade."));
            return 0;
        }
        if (target == null) {
            source.sendFailure(Component.literal("Target player not found."));
            return 0;
        }
        if (target == requester) {
            return tradeVillagerMyself(source);
        }
        if (target.level() != requester.level()) {
            source.sendFailure(Component.literal("Target player is in another dimension."));
            return 0;
        }

        Entity identity = ((EntityAccessor) target).getCurrentIdentity();
        if (identity instanceof Villager villagerIdentity) {
            villagerIdentity.mobInteract(requester, InteractionHand.MAIN_HAND);
            return 1;
        }
        if (identity instanceof WanderingTrader traderIdentity) {
            traderIdentity.mobInteract(requester, InteractionHand.MAIN_HAND);
            return 1;
        }

        source.sendFailure(Component.literal("Target is not morphed as a villager."));
        return 0;
    }

    private static int morph(CommandSourceStack source, Identifier identityId) {
        return morph(source, identityId, "");
    }

    private static int morph(CommandSourceStack source, Identifier identityId, String rawVariant) {
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

        if (IdentityProgression.shouldEnforceIdentityUnlocksForMorph() && !isOperator(source) && !IdentityProgression.isUnlocked(player, identityId)) {
            source.sendFailure(Component.literal("Identity not unlocked: " + identityId));
            return 0;
        }

        MorphVariantParseResult variantResult = resolveMorphVariant(identityId, rawVariant);
        if (variantResult.error() != null) {
            source.sendFailure(Component.literal(variantResult.error()));
            return 0;
        }

        CompoundTag variantNbt = variantResult.variantNbt();
        if (
                IdentityProgression.shouldEnforceIdentityUnlocksForMorph()
                        && !isOperator(source)
                        && !IdentityProgression.isVariantUnlocked(player, identityId, variantNbt)
        ) {
            source.sendFailure(Component.literal("Variant not unlocked for " + identityId + ": " + variantResult.variantLabel()));
            return 0;
        }

        if (!IdentityProgression.morph(player, identityId, variantNbt)) {
            return 0;
        }
        String suffix = variantResult.isDefaultVariant() ? "" : " (" + variantResult.variantLabel() + ")";
        identity2$sendCommandFeedback(source, Component.literal("Morphed into " + identityId + suffix));
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
        identity2$sendCommandFeedback(source, Component.literal("Identity cleared."));
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
            identity2$sendCommandFeedback(source, Component.literal("Unlocked identities: none"));
            return 1;
        }

        identity2$sendCommandFeedback(source, Component.literal("Unlocked identities (" + unlocked.size() + "): " + String.join(", ", unlocked)));
        return 1;
    }

    private static int unlockIdentity(CommandSourceStack source, Identifier identityId, ServerPlayer target) {
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

        ServerPlayer resolvedTarget = target;
        if (resolvedTarget == null) {
            resolvedTarget = source.getPlayer();
        }
        if (resolvedTarget == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }

        boolean granted = IdentityProgression.grantIdentity(resolvedTarget, identityId);
        if (!granted) {
            identity2$sendCommandFeedback(source, Component.literal(resolvedTarget.getName().getString() + " already has " + identityId));
            return 1;
        }

        String targetName = resolvedTarget.getName().getString();
        identity2$sendCommandFeedback(source, Component.literal("Unlocked identity " + identityId + " for " + targetName));
        return 1;
    }

    private static int unlockAll(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer resolvedTarget = target;
        if (resolvedTarget == null) {
            resolvedTarget = source.getPlayer();
        }
        if (resolvedTarget == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }

        int granted = IdentityProgression.grantAllMorphableIdentities(resolvedTarget);
        String targetName = resolvedTarget.getName().getString();
        identity2$sendCommandFeedback(source, Component.literal("Unlocked " + granted + " identities for " + targetName));
        return granted;
    }

    private static int listConfig(CommandSourceStack source) {
        if (CONFIG_FIELDS.isEmpty()) {
            identity2$sendCommandFeedback(source, Component.literal("No editable config keys found."));
            return 1;
        }
        identity2$sendCommandFeedback(source, Component.literal("Config keys (" + CONFIG_FIELDS.size() + "): " + String.join(", ", CONFIG_FIELDS.keySet())));
        return CONFIG_FIELDS.size();
    }

    private static int getConfig(CommandSourceStack source, String key) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        Object value = getConfigFieldValue(field);
        identity2$sendCommandFeedback(source, Component.literal(key + " = " + formatConfigValue(value)));
        return 1;
    }

    private static int setConfig(CommandSourceStack source, String key, String rawValue) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        Class<?> type = field.getType();
        if (List.class.isAssignableFrom(type)) {
            source.sendFailure(Component.literal("Config key " + key + " is a list. Use /identity config add/remove/clear."));
            return 0;
        }

        Object parsed;
        try {
            parsed = parseScalarConfigValue(type, rawValue);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Invalid value for " + key + ": " + exception.getMessage()));
            return 0;
        }

        try {
            field.set(null, parsed);
        } catch (IllegalAccessException exception) {
            source.sendFailure(Component.literal("Failed to set " + key + ": " + exception.getMessage()));
            return 0;
        }

        identity2$normalizeAliasedConfigAfterSet(key);
        IdentityConfigManager.save();
        identity2$sendCommandFeedback(source, Component.literal("Set " + key + " = " + formatConfigValue(parsed) + " and saved it to the server config"));
        return 1;
    }

    private static int addConfigListValue(CommandSourceStack source, String key, String value) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        if (!List.class.isAssignableFrom(field.getType())) {
            source.sendFailure(Component.literal("Config key " + key + " is not a list."));
            return 0;
        }

        List<String> values = getOrCreateConfigStringList(field);
        if (values.contains(value)) {
            identity2$sendCommandFeedback(source, Component.literal("Value already present in " + key + ": " + value));
            return 1;
        }

        values.add(value);
        IdentityConfigManager.save();
        identity2$sendCommandFeedback(source, Component.literal("Added \"" + value + "\" to " + key + " and saved it to the server config"));
        return 1;
    }

    private static int removeConfigListValue(CommandSourceStack source, String key, String value) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        if (!List.class.isAssignableFrom(field.getType())) {
            source.sendFailure(Component.literal("Config key " + key + " is not a list."));
            return 0;
        }

        List<String> values = getOrCreateConfigStringList(field);
        if (!values.remove(value)) {
            identity2$sendCommandFeedback(source, Component.literal("Value not present in " + key + ": " + value));
            return 1;
        }

        IdentityConfigManager.save();
        identity2$sendCommandFeedback(source, Component.literal("Removed \"" + value + "\" from " + key + " and saved it to the server config"));
        return 1;
    }

    private static int clearConfigList(CommandSourceStack source, String key) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        if (!List.class.isAssignableFrom(field.getType())) {
            source.sendFailure(Component.literal("Config key " + key + " is not a list."));
            return 0;
        }

        List<String> values = getOrCreateConfigStringList(field);
        int removed = values.size();
        values.clear();
        IdentityConfigManager.save();
        identity2$sendCommandFeedback(source, Component.literal("Cleared " + key + " (" + removed + " entries) and saved it to the server config"));
        return removed;
    }

    private static int listAbilities(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can list abilities."));
            return 0;
        }

        List<Identifier> candidates = collectAbilityCandidates(source, player);
        List<String> lines = new ArrayList<>();
        int builtinCount = 0;
        int fallbackCount = 0;
        int noneCount = 0;

        for (Identifier id : candidates) {
            AbilityInfo info = resolveAbilityInfo(id);
            if (!info.hasAny()) {
                noneCount++;
                continue;
            }
            if (info.usesGenericFallback()) {
                fallbackCount++;
            } else {
                builtinCount++;
            }
            lines.add(id + " -> " + info.summary());
        }

        if (lines.isEmpty()) {
            identity2$sendCommandFeedback(source, Component.literal("No abilities found for current identity set."));
            return 1;
        }

        identity2$sendCommandFeedback(
                source,
                Component.literal(
                        "Abilities (" + lines.size() + "): " + builtinCount + " specific, " + fallbackCount + " fallback, " + noneCount + " none"
                )
        );
        for (String line : lines) {
            identity2$sendCommandFeedback(source, Component.literal(line));
        }
        return lines.size();
    }

    private static int abilityInfo(CommandSourceStack source, Identifier identityId) {
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }

        AbilityInfo info = resolveAbilityInfo(identityId);
        if (!info.hasAny()) {
            identity2$sendCommandFeedback(source, Component.literal("Ability for " + identityId + ": none"));
            return 1;
        }

        identity2$sendCommandFeedback(source, Component.literal("Ability for " + identityId + ": " + info.summary()));
        return 1;
    }

    private static int currentAbilityInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can query current ability."));
            return 0;
        }

        Entity current = ((EntityAccessor) player).getCurrentIdentity();
        if (current == null) {
            identity2$sendCommandFeedback(source, Component.literal("Current ability: none (not morphed)."));
            return 1;
        }

        Identifier id = EntityType.getKey(current.getType());
        if (id == null) {
            identity2$sendCommandFeedback(source, Component.literal("Current ability: none (unknown identity type)."));
            return 1;
        }
        return abilityInfo(source, id);
    }

    private static CompletableFuture<Suggestions> suggestUnlockedIdentities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return Suggestions.empty();
        }

        if (IdentityProgression.shouldEnforceIdentityUnlocksForMorph() && !isOperator(context.getSource())) {
            List<Identifier> identifiers = IdentityProgression.getUnlockedIdentities(player).stream()
                    .map(IdentityCommand::parseIdentifier)
                    .flatMap(Optional::stream)
                    .filter(IdentityProgression::isMorphableIdentity)
                    .toList();
            return SharedSuggestionProvider.suggestResource(identifiers, builder);
        }

        return SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(IdentityProgression::isMorphableIdentity),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestMorphableIdentities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(IdentityProgression::isMorphableIdentity),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestConfigKeys(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(CONFIG_FIELDS.keySet(), builder);
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

    private static MorphVariantParseResult resolveMorphVariant(Identifier identityId, String rawVariant) {
        String trimmed = rawVariant == null ? "" : rawVariant.trim();
        if (trimmed.isEmpty()) {
            return MorphVariantParseResult.defaultVariant();
        }

        if (trimmed.startsWith("{")) {
            try {
                CompoundTag parsed = CompoundTagArgument.compoundTag().parse(new StringReader(trimmed));
                return new MorphVariantParseResult(parsed, trimmed, false, null);
            } catch (Exception exception) {
                return new MorphVariantParseResult(new CompoundTag(), trimmed, false, "Invalid variant NBT: " + trimmed);
            }
        }

        String normalized = normalizeVariantToken(trimmed);
        if (normalized.isEmpty() || normalized.equals("default") || normalized.equals("base") || normalized.equals("normal") || normalized.equals("none")) {
            return MorphVariantParseResult.defaultVariant();
        }

        CompoundTag variantNbt = new CompoundTag();
        List<String> labelParts = new ArrayList<>();
        for (String token : trimmed.split("[,\\s]+")) {
            String part = token == null ? "" : token.trim();
            if (part.isEmpty()) {
                continue;
            }
            String normalizedPart = normalizeVariantToken(part);
            if (normalizedPart.isEmpty()) {
                continue;
            }

            if (applySimpleVariantToken(identityId, normalizedPart, variantNbt, labelParts)) {
                continue;
            }

            int separator = part.indexOf('=');
            if (separator < 0) {
                separator = part.indexOf(':');
            }
            if (separator > 0 && separator < part.length() - 1) {
                String key = normalizeVariantToken(part.substring(0, separator));
                String value = part.substring(separator + 1).trim();
                if (applyKeyValueVariant(identityId, key, value, variantNbt, labelParts)) {
                    continue;
                }
            }

            return new MorphVariantParseResult(new CompoundTag(), trimmed, false, "Unknown variant token for " + identityId + ": " + part);
        }

        if (variantNbt.isEmpty()) {
            return new MorphVariantParseResult(new CompoundTag(), trimmed, false, "Unknown variant for " + identityId + ": " + trimmed);
        }

        String label = labelParts.isEmpty() ? trimmed : String.join(" ", labelParts);
        return new MorphVariantParseResult(variantNbt, label, false, null);
    }

    private static boolean applySimpleVariantToken(
            Identifier identityId,
            String token,
            CompoundTag variantNbt,
            List<String> labelParts
    ) {
        if ("baby".equals(token)) {
            variantNbt.putBoolean("IsBaby", true);
            variantNbt.putInt("Age", -24000);
            labelParts.add("baby");
            return true;
        }
        if ("adult".equals(token)) {
            variantNbt.putBoolean("IsBaby", false);
            variantNbt.putInt("Age", 0);
            labelParts.add("adult");
            return true;
        }

        DyeColor color = parseDyeColor(token);
        if (color != null && isColorMorph(identityId)) {
            variantNbt.putByte("Color", (byte) color.getId());
            labelParts.add(color.getName());
            return true;
        }
        return false;
    }

    private static boolean applyKeyValueVariant(
            Identifier identityId,
            String key,
            String value,
            CompoundTag variantNbt,
            List<String> labelParts
    ) {
        if (key.isEmpty() || value == null || value.isBlank()) {
            return false;
        }

        String normalizedValue = normalizeVariantToken(value);
        if ("baby".equals(key)) {
            boolean baby = parseBooleanVariantValue(normalizedValue);
            variantNbt.putBoolean("IsBaby", baby);
            variantNbt.putInt("Age", baby ? -24000 : 0);
            labelParts.add(baby ? "baby" : "adult");
            return true;
        }

        if ("age".equals(key)) {
            try {
                int age = Integer.parseInt(value.trim());
                variantNbt.putInt("Age", age);
                variantNbt.putBoolean("IsBaby", age < 0);
                labelParts.add("age=" + age);
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        if ("color".equals(key) && isColorMorph(identityId)) {
            DyeColor color = parseDyeColor(normalizedValue);
            if (color == null) {
                return false;
            }
            variantNbt.putByte("Color", (byte) color.getId());
            labelParts.add(color.getName());
            return true;
        }

        if ("variant".equals(key) || "type".equals(key)) {
            Integer numeric = parseIntegerVariantValue(value);
            if (numeric != null) {
                variantNbt.putInt("Variant", numeric);
                labelParts.add(key + "=" + numeric);
                return true;
            }
            if ("minecraft:frog".equals(identityId.toString())) {
                variantNbt.putString("FrogVariant", normalizeVariantResourceValue(value));
                labelParts.add(value.trim());
                return true;
            }
            if ("minecraft:cat".equals(identityId.toString())) {
                variantNbt.putString("CatVariant", normalizeVariantResourceValue(value));
                labelParts.add(value.trim());
                return true;
            }
            if ("minecraft:wolf".equals(identityId.toString())) {
                variantNbt.putString("WolfVariant", normalizeVariantResourceValue(value));
                labelParts.add(value.trim());
                return true;
            }
        }

        return false;
    }

    private static boolean isColorMorph(Identifier identityId) {
        return identityId != null && "minecraft:sheep".equals(identityId.toString());
    }

    private static DyeColor parseDyeColor(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = normalizeVariantToken(token);
        for (DyeColor color : DyeColor.values()) {
            if (normalizeVariantToken(color.getName()).equals(normalized)) {
                return color;
            }
        }
        return null;
    }

    private static boolean parseBooleanVariantValue(String value) {
        return switch (value) {
            case "true", "1", "yes", "on", "enabled", "baby" -> true;
            default -> false;
        };
    }

    private static Integer parseIntegerVariantValue(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeVariantResourceValue(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (normalized.indexOf(':') >= 0) {
            return normalized;
        }
        return "minecraft:" + normalized;
    }

    private static String normalizeVariantToken(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Map<String, Field> createConfigFieldMap() {
        List<Field> fields = new ArrayList<>();
        for (Field field : IdentitySettings.class.getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.isSynthetic()) {
                continue;
            }
            if (HIDDEN_CONFIG_KEYS.contains(field.getName())) {
                continue;
            }
            fields.add(field);
        }
        fields.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<String, Field> out = new LinkedHashMap<>();
        for (Field field : fields) {
            out.put(field.getName(), field);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void identity2$normalizeAliasedConfigAfterSet(String key) {
        if ("enableMorphChargeSystem".equals(key) || "EnableMorphCharges".equals(key)) {
            IdentitySettings.EnableMorphCharges = IdentitySettings.enableMorphChargeSystem;
            return;
        }
        if ("enableSoulJarSystem".equals(key) || "EnableSoulJars".equals(key)) {
            IdentitySettings.EnableSoulJars = IdentitySettings.enableSoulJarSystem;
            return;
        }
        if ("enablePermanentMorphs".equals(key) || "EnablePermanentJarMorphs".equals(key)) {
            IdentitySettings.EnablePermanentJarMorphs = IdentitySettings.enablePermanentMorphs;
            return;
        }
        if ("enableSoulAbsorption".equals(key) || "EnableSoulAbsorption".equals(key)) {
            IdentitySettings.EnableSoulAbsorption = IdentitySettings.enableSoulAbsorption;
        }
    }

    private static Object parseScalarConfigValue(Class<?> type, String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (type == boolean.class || type == Boolean.class) {
            String normalized = trimmed.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "true", "1", "yes", "on", "enabled" -> true;
                case "false", "0", "no", "off", "disabled" -> false;
                default -> throw new IllegalArgumentException("expected boolean (true/false/on/off/1/0)");
            };
        }
        if (type == int.class || type == Integer.class) {
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("expected integer");
            }
        }
        if (type == float.class || type == Float.class) {
            try {
                return Float.parseFloat(trimmed);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("expected float");
            }
        }
        if (type == double.class || type == Double.class) {
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("expected decimal number");
            }
        }
        if (type == long.class || type == Long.class) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("expected long integer");
            }
        }
        if (type == String.class) {
            return "null".equalsIgnoreCase(trimmed) ? null : rawValue;
        }
        if (type.isEnum()) {
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("expected enum value");
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            Class<? extends Enum> enumType = (Class<? extends Enum>) type;

            // Accept case insensitive values and allow hyphen or space as underscore
            String candidate = trimmed
                    .trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);

            try {
                return Enum.valueOf(enumType, candidate);
            } catch (IllegalArgumentException exception) {
                StringBuilder allowed = new StringBuilder();
                Object[] constants = enumType.getEnumConstants();
                for (int i = 0; i < constants.length; i++) {
                    if (i > 0) allowed.append(", ");
                    allowed.append(((Enum<?>) constants[i]).name().toLowerCase(Locale.ROOT));
                }
                throw new IllegalArgumentException("expected one of: " + allowed);
            }
        }
        throw new IllegalArgumentException("unsupported type: " + type.getSimpleName());
    }

    private static Object getConfigFieldValue(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException exception) {
            return "<inaccessible>";
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getOrCreateConfigStringList(Field field) {
        try {
            Object value = field.get(null);
            if (value instanceof List<?> existing) {
                return (List<String>) existing;
            }
            List<String> created = new ArrayList<>();
            field.set(null, created);
            return created;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access config list field " + field.getName(), exception);
        }
    }

    private static String formatConfigValue(Object value) {
        if (value instanceof List<?> list) {
            return list.toString();
        }
        return String.valueOf(value);
    }

    private static List<Identifier> collectAbilityCandidates(CommandSourceStack source, ServerPlayer player) {
        if (!IdentityProgression.shouldEnforceIdentityUnlocksForMorph() || isOperator(source)) {
            return BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(IdentityProgression::isMorphableIdentity).sorted().toList();
        }

        return IdentityProgression.getUnlockedIdentities(player).stream()
                .map(IdentityCommand::parseIdentifier)
                .flatMap(Optional::stream)
                .filter(IdentityProgression::isMorphableIdentity)
                .sorted()
                .toList();
    }

    private static AbilityInfo resolveAbilityInfo(Identifier identityId) {
        if (identityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            return AbilityInfo.none();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identityId);
        if (type == null) {
            return AbilityInfo.none();
        }

        IdentityAbilityDefinition definition = ModRegistries.resolveIdentityAbility(type);
        Identifier predefFromDefinition = definition == null ? null : definition.bultinability();
        Identifier predefKey = isNullIdentifier(predefFromDefinition) ? identityId : predefFromDefinition;

        boolean hasSpecificPredef = hasSpecificPredef(predefKey);
        boolean hasFallback = PredefIdentityAbilities.hasFallbackAbility(identityId);
        if (definition == null && !hasSpecificPredef && !hasFallback) {
            return AbilityInfo.none();
        }

        int cooldown = definition == null ? 20 : definition.cooldown();
        int useDuration = definition == null ? 0 : definition.useduration();
        boolean overrideAttack = definition != null && definition.override_attack();
        String command = definition == null ? "" : definition.command();
        Identifier predefLabel = hasSpecificPredef ? predefKey : null;
        boolean genericFallback = !hasSpecificPredef && hasFallback;

        return new AbilityInfo(cooldown, useDuration, overrideAttack, command, predefLabel, genericFallback);
    }

    private static boolean hasSpecificPredef(Identifier prebuilt) {
        if (prebuilt == null || isNullIdentifier(prebuilt)) {
            return false;
        }

        if (PredefIdentityAbilities.predef.containsKey(prebuilt)) {
            return true;
        }

        Identifier minecraftAlias = Identifier.fromNamespaceAndPath("minecraft", prebuilt.getPath());
        if (PredefIdentityAbilities.predef.containsKey(minecraftAlias)) {
            return true;
        }

        return PredefIdentityAbilities.predef.containsKey(Identifier.fromNamespaceAndPath("identity2", prebuilt.getPath()));
    }

    private static boolean isNullIdentifier(Identifier id) {
        return id == null || "null".equals(id.getPath());
    }

    private record AbilityInfo(
            int cooldown,
            int useDuration,
            boolean overrideAttack,
            String command,
            Identifier predef,
            boolean genericFallback
    ) {
        static AbilityInfo none() {
            return new AbilityInfo(0, 0, false, "", null, false);
        }

        boolean hasAny() {
            return this.predef != null || this.genericFallback || (this.command != null && !this.command.isBlank());
        }

        boolean usesGenericFallback() {
            return this.genericFallback;
        }

        String summary() {
            String source = this.genericFallback ? "generic_fallback" : (this.predef != null ? "predef:" + this.predef : "command_only");
            String commandText = (this.command != null && !this.command.isBlank()) ? " command=\"" + this.command + "\"" : "";
            String overrideText = this.overrideAttack ? " override_attack=true" : "";
            return source + " cooldown=" + this.cooldown + " use_duration=" + this.useDuration + overrideText + commandText;
        }
    }

    private record MorphVariantParseResult(
            CompoundTag variantNbt,
            String variantLabel,
            boolean isDefaultVariant,
            String error
    ) {
        static MorphVariantParseResult defaultVariant() {
            return new MorphVariantParseResult(new CompoundTag(), "default", true, null);
        }
    }
}
