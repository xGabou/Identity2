package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("identity")
                .then(
                    Commands.literal("morph")
                        .then(
                            Commands.argument("identity_id", ResourceLocationArgument.id())
                                .suggests(IdentityCommand::suggestUnlockedIdentities)
                                .executes(context -> morph(context.getSource(), ResourceLocationArgument.getId(context, "identity_id")))
                        )
                )
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(
                    Commands.literal("unlock")
                            .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                        .then(
                            Commands.argument("identity_id", ResourceLocationArgument.id())
                                .suggests(IdentityCommand::suggestMorphableIdentities)
                                .executes(context -> unlockIdentity(context.getSource(), ResourceLocationArgument.getId(context, "identity_id"), null))
                                .then(
                                    Commands.argument("target", EntityArgument.player())
                                        .executes(
                                            context -> unlockIdentity(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "identity_id"),
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
                                    Commands.argument("identity_id", ResourceLocationArgument.id())
                                        .suggests(IdentityCommand::suggestMorphableIdentities)
                                        .executes(context -> abilityInfo(context.getSource(), ResourceLocationArgument.getId(context, "identity_id")))
                                )
                        )
                        .then(Commands.literal("current").executes(context -> currentAbilityInfo(context.getSource())))
                )
                .then(
                    Commands.literal("config")
                            .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
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

    private static int morph(CommandSourceStack source, ResourceLocation identityId) {
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

        if (!IdentityProgression.morph(player, identityId)) {
            return 0;
        }
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

    private static int unlockIdentity(CommandSourceStack source, ResourceLocation identityId, ServerPlayer target) {
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
            source.sendSystemMessage(Component.literal(resolvedTarget.getName().getString() + " already has " + identityId));
            return 1;
        }

        String targetName = resolvedTarget.getName().getString();
        source.sendSuccess(
            () -> Component.literal("Unlocked identity " + identityId + " for " + targetName),
            true
        );
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
        source.sendSuccess(
            () -> Component.literal("Unlocked " + granted + " identities for " + targetName),
            true
        );
        return granted;
    }

    private static int listConfig(CommandSourceStack source) {
        if (CONFIG_FIELDS.isEmpty()) {
            source.sendSystemMessage(Component.literal("No editable config keys found."));
            return 1;
        }
        source.sendSystemMessage(Component.literal("Config keys (" + CONFIG_FIELDS.size() + "): " + String.join(", ", CONFIG_FIELDS.keySet())));
        return CONFIG_FIELDS.size();
    }

    private static int getConfig(CommandSourceStack source, String key) {
        Field field = CONFIG_FIELDS.get(key);
        if (field == null) {
            source.sendFailure(Component.literal("Unknown config key: " + key));
            return 0;
        }

        Object value = getConfigFieldValue(field);
        source.sendSystemMessage(Component.literal(key + " = " + formatConfigValue(value)));
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
        source.sendSuccess(() -> Component.literal("Set " + key + " = " + formatConfigValue(parsed)), true);
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
            source.sendSystemMessage(Component.literal("Value already present in " + key + ": " + value));
            return 1;
        }

        values.add(value);
        IdentityConfigManager.save();
        source.sendSuccess(() -> Component.literal("Added \"" + value + "\" to " + key), true);
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
            source.sendSystemMessage(Component.literal("Value not present in " + key + ": " + value));
            return 1;
        }

        IdentityConfigManager.save();
        source.sendSuccess(() -> Component.literal("Removed \"" + value + "\" from " + key), true);
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
        source.sendSuccess(() -> Component.literal("Cleared " + key + " (" + removed + " entries)"), true);
        return removed;
    }

    private static int listAbilities(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can list abilities."));
            return 0;
        }

        List<ResourceLocation> candidates = collectAbilityCandidates(source, player);
        List<String> lines = new ArrayList<>();
        int builtinCount = 0;
        int fallbackCount = 0;
        int noneCount = 0;

        for (ResourceLocation id : candidates) {
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
            source.sendSystemMessage(Component.literal("No abilities found for current identity set."));
            return 1;
        }

        source.sendSystemMessage(
            Component.literal(
                "Abilities (" + lines.size() + "): " + builtinCount + " specific, " + fallbackCount + " fallback, " + noneCount + " none"
            )
        );
        for (String line : lines) {
            source.sendSystemMessage(Component.literal(line));
        }
        return lines.size();
    }

    private static int abilityInfo(CommandSourceStack source, ResourceLocation identityId) {
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }

        AbilityInfo info = resolveAbilityInfo(identityId);
        if (!info.hasAny()) {
            source.sendSystemMessage(Component.literal("Ability for " + identityId + ": none"));
            return 1;
        }

        source.sendSystemMessage(Component.literal("Ability for " + identityId + ": " + info.summary()));
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
            source.sendSystemMessage(Component.literal("Current ability: none (not morphed)."));
            return 1;
        }

        ResourceLocation id = EntityType.getKey(current.getType());
        if (id == null) {
            source.sendSystemMessage(Component.literal("Current ability: none (unknown identity type)."));
            return 1;
        }
        return abilityInfo(source, id);
    }

    private static CompletableFuture<Suggestions> suggestUnlockedIdentities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return Suggestions.empty();
        }

        if (IdentitySettings.requireUnlockedIdentityForMorph && !isOperator(context.getSource())) {
            List<ResourceLocation> identifiers = IdentityProgression.getUnlockedIdentities(player).stream()
                .map(IdentityCommand::parseResourceLocation)
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
        return source.hasPermission(Commands.LEVEL_ADMINS);
    }

    private static boolean isMorphableIdentityString(String value) {
        try {
            return IdentityProgression.isMorphableIdentity(new ResourceLocation(value));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Optional<ResourceLocation> parseResourceLocation(String value) {
        try {
            return Optional.of(new ResourceLocation(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
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

    private static List<ResourceLocation> collectAbilityCandidates(CommandSourceStack source, ServerPlayer player) {
        if (!IdentitySettings.requireUnlockedIdentityForMorph || isOperator(source)) {
            return BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(IdentityProgression::isMorphableIdentity).sorted().toList();
        }

        return IdentityProgression.getUnlockedIdentities(player).stream()
            .map(IdentityCommand::parseResourceLocation)
            .flatMap(Optional::stream)
            .filter(IdentityProgression::isMorphableIdentity)
            .sorted()
            .toList();
    }

    private static AbilityInfo resolveAbilityInfo(ResourceLocation identityId) {
        if (identityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            return AbilityInfo.none();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(identityId);
        if (type == null) {
            return AbilityInfo.none();
        }

        IdentityAbilityDefinition definition = ModRegistries.resolveIdentityAbility(type);
        ResourceLocation predefFromDefinition = definition == null ? null : definition.bultinability();
        ResourceLocation predefKey = isNullResourceLocation(predefFromDefinition) ? identityId : predefFromDefinition;

        boolean hasSpecificPredef = hasSpecificPredef(predefKey);
        boolean hasFallback = PredefIdentityAbilities.hasFallbackAbility(identityId);
        if (definition == null && !hasSpecificPredef && !hasFallback) {
            return AbilityInfo.none();
        }

        int cooldown = definition == null ? 20 : definition.cooldown();
        int useDuration = definition == null ? 0 : definition.useduration();
        boolean overrideAttack = definition != null && definition.override_attack();
        String command = definition == null ? "" : definition.command();
        ResourceLocation predefLabel = hasSpecificPredef ? predefKey : null;
        boolean genericFallback = !hasSpecificPredef && hasFallback;

        return new AbilityInfo(cooldown, useDuration, overrideAttack, command, predefLabel, genericFallback);
    }

    private static boolean hasSpecificPredef(ResourceLocation prebuilt) {
        if (prebuilt == null || isNullResourceLocation(prebuilt)) {
            return false;
        }

        if (PredefIdentityAbilities.predef.containsKey(prebuilt)) {
            return true;
        }

        ResourceLocation minecraftAlias = new ResourceLocation("minecraft", prebuilt.getPath());
        if (PredefIdentityAbilities.predef.containsKey(minecraftAlias)) {
            return true;
        }

        return PredefIdentityAbilities.predef.containsKey(new ResourceLocation("identity2", prebuilt.getPath()));
    }

    private static boolean isNullResourceLocation(ResourceLocation id) {
        return id == null || "null".equals(id.getPath());
    }

    private record AbilityInfo(
        int cooldown,
        int useDuration,
        boolean overrideAttack,
        String command,
        ResourceLocation predef,
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
}
