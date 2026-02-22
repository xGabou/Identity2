package draylar.identity.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import draylar.identity.api.PlayerIdentity;
import draylar.identity.api.PlayerUnlocks;
import draylar.identity.api.platform.IdentityConfig;
import draylar.identity.api.platform.IdentityPlatform;
import draylar.identity.api.variant.IdentityType;
import draylar.identity.screen.widget.EntityWidget;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class IdentityCommand {
    private static final Map<String, Consumer<Boolean>> BOOLEAN_SETTERS = new LinkedHashMap<>();
    private static final Map<String, IntConsumer> INT_SETTERS = new LinkedHashMap<>();
    private static final Map<String, Consumer<Float>> FLOAT_SETTERS = new LinkedHashMap<>();
    private static final List<String> STRING_OPTIONS = new ArrayList<>();

    private static final SuggestionProvider<ServerCommandSource> BOOLEAN_OPTION_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(BOOLEAN_SETTERS.keySet(), builder);
    private static final SuggestionProvider<ServerCommandSource> INT_OPTION_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(INT_SETTERS.keySet(), builder);
    private static final SuggestionProvider<ServerCommandSource> FLOAT_OPTION_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(FLOAT_SETTERS.keySet(), builder);
    private static final SuggestionProvider<ServerCommandSource> STRING_OPTION_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(STRING_OPTIONS, builder);

    private static final SuggestionProvider<ServerCommandSource> FORCED_IDENTITY_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("none");
        Registries.ENTITY_TYPE.getIds().forEach(id -> suggestions.add(id.toString()));
        return CommandSource.suggestMatching(suggestions, builder);
    };

    static {
        BOOLEAN_SETTERS.put("overlay_identity_unlocks", value -> IdentityConfig.getInstance().setOverlayIdentityUnlocks(value));
        BOOLEAN_SETTERS.put("overlay_identity_revokes", value -> IdentityConfig.getInstance().setOverlayIdentityRevokes(value));
        BOOLEAN_SETTERS.put("revoke_identity_on_death", value -> IdentityConfig.getInstance().setRevokeIdentityOnDeath(value));
        BOOLEAN_SETTERS.put("identities_equip_items", value -> IdentityConfig.getInstance().setIdentitiesEquipItems(value));
        BOOLEAN_SETTERS.put("identities_equip_armor", value -> IdentityConfig.getInstance().setIdentitiesEquipArmor(value));
        BOOLEAN_SETTERS.put("show_player_nametag", value -> IdentityConfig.getInstance().setShowPlayerNametag(value));
        BOOLEAN_SETTERS.put("render_own_nametag", value -> IdentityConfig.getInstance().setRenderOwnNameTag(value));
        BOOLEAN_SETTERS.put("hostiles_ignore_hostile_identity_player", value -> IdentityConfig.getInstance().setHostilesIgnoreHostileIdentityPlayer(value));
        BOOLEAN_SETTERS.put("hostiles_forget_new_hostile_identity_player", value -> IdentityConfig.getInstance().setHostilesForgetNewHostileIdentityPlayer(value));
        BOOLEAN_SETTERS.put("wolves_attack_identity_prey", value -> IdentityConfig.getInstance().setWolvesAttackIdentityPrey(value));
        BOOLEAN_SETTERS.put("owned_wolves_attack_identity_prey", value -> IdentityConfig.getInstance().setOwnedWolvesAttackIdentityPrey(value));
        BOOLEAN_SETTERS.put("villagers_run_from_identities", value -> IdentityConfig.getInstance().setVillagersRunFromIdentities(value));
        BOOLEAN_SETTERS.put("foxes_attack_identity_prey", value -> IdentityConfig.getInstance().setFoxesAttackIdentityPrey(value));
        BOOLEAN_SETTERS.put("use_identity_sounds", value -> IdentityConfig.getInstance().setUseIdentitySounds(value));
        BOOLEAN_SETTERS.put("play_ambient_sounds", value -> IdentityConfig.getInstance().setPlayAmbientSounds(value));
        BOOLEAN_SETTERS.put("hear_self_ambient", value -> IdentityConfig.getInstance().setHearSelfAmbient(value));
        BOOLEAN_SETTERS.put("enable_flight", value -> IdentityConfig.getInstance().setEnableFlight(value));
        BOOLEAN_SETTERS.put("enable_client_swap_menu", value -> IdentityConfig.getInstance().setEnableClientSwapMenu(value));
        BOOLEAN_SETTERS.put("enable_swaps", value -> IdentityConfig.getInstance().setEnableSwaps(value));
        BOOLEAN_SETTERS.put("allow_self_trading", value -> IdentityConfig.getInstance().setAllowSelfTrading(value));
        BOOLEAN_SETTERS.put("force_change_new", value -> IdentityConfig.getInstance().setForceChangeNew(value));
        BOOLEAN_SETTERS.put("force_change_always", value -> IdentityConfig.getInstance().setForceChangeAlways(value));
        BOOLEAN_SETTERS.put("log_commands", value -> IdentityConfig.getInstance().setLogCommands(value));
        BOOLEAN_SETTERS.put("kill_for_identity", value -> IdentityConfig.getInstance().setKillForIdentity(value));
        BOOLEAN_SETTERS.put("scaling_health", value -> IdentityConfig.getInstance().setScalingHealth(value));
        BOOLEAN_SETTERS.put("warden_is_blinded", value -> IdentityConfig.getInstance().setWardenIsBlinded(value));
        BOOLEAN_SETTERS.put("warden_blinds_nearby", value -> IdentityConfig.getInstance().setWardenBlindsNearby(value));

        INT_SETTERS.put("hostility_time", value -> IdentityConfig.getInstance().setHostilityTime(value));
        INT_SETTERS.put("max_health", value -> IdentityConfig.getInstance().setMaxHealth(value));
        INT_SETTERS.put("enderman_ability_teleport_distance", value -> IdentityConfig.getInstance().setEndermanAbilityTeleportDistance(value));
        INT_SETTERS.put("required_kills_for_identity", value -> IdentityConfig.getInstance().setRequiredKillsForIdentity(value));

        FLOAT_SETTERS.put("fly_speed", value -> IdentityConfig.getInstance().setFlySpeed(value));

        STRING_OPTIONS.add("forced_identity");
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createListCommand(CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<ServerCommandSource> listBuilder = CommandManager.literal("list");

        listBuilder.then(createStringListNode("allowed_swappers", () -> IdentityConfig.getInstance().allowedSwappers(), true, "player"));
        listBuilder.then(createStringListNode("advancements_required_for_flight", () -> IdentityConfig.getInstance().advancementsRequiredForFlight(), false, "advancement"));
        listBuilder.then(createEntityListNode("extra_aquatic_entities", () -> IdentityConfig.getInstance().extraAquaticEntities(), registryAccess));
        listBuilder.then(createEntityListNode("removed_aquatic_entities", () -> IdentityConfig.getInstance().removedAquaticEntities(), registryAccess));
        listBuilder.then(createEntityListNode("extra_flying_entities", () -> IdentityConfig.getInstance().extraFlyingEntities(), registryAccess));
        listBuilder.then(createEntityListNode("removed_flying_entities", () -> IdentityConfig.getInstance().removedFlyingEntities(), registryAccess));

        return listBuilder;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createMapCommand(CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<ServerCommandSource> mapBuilder = CommandManager.literal("map");

        mapBuilder.then(createAbilityCooldownCommands(registryAccess));
        mapBuilder.then(createRequiredKillCommands(registryAccess));

        return mapBuilder;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createStringListNode(String literal, Supplier<List<String>> listSupplier, boolean caseInsensitive, String valueArgumentName) {
        return CommandManager.literal(literal)
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument(valueArgumentName, StringArgumentType.string())
                                .executes(ctx -> addToList(ctx.getSource(), listSupplier, caseInsensitive, literal, StringArgumentType.getString(ctx, valueArgumentName)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument(valueArgumentName, StringArgumentType.string())
                                .suggests((context, builder) -> CommandSource.suggestMatching(new ArrayList<>(listSupplier.get()), builder))
                                .executes(ctx -> removeFromList(ctx.getSource(), listSupplier, caseInsensitive, literal, StringArgumentType.getString(ctx, valueArgumentName)))))
                .then(CommandManager.literal("clear")
                        .executes(ctx -> clearList(ctx.getSource(), listSupplier, literal)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createEntityListNode(String literal, Supplier<List<String>> listSupplier, CommandRegistryAccess registryAccess) {
        return CommandManager.literal(literal)
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("entity", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(ctx -> {
                                    Identifier id = RegistryEntryReferenceArgumentType.getSummonableEntityType(ctx, "entity").registryKey().getValue();
                                    return addToList(ctx.getSource(), listSupplier, false, literal, id.toString());
                                })))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("entity", StringArgumentType.string())
                                .suggests((context, builder) -> CommandSource.suggestMatching(new ArrayList<>(listSupplier.get()), builder))
                                .executes(ctx -> removeFromList(ctx.getSource(), listSupplier, false, literal, StringArgumentType.getString(ctx, "entity")))))
                .then(CommandManager.literal("clear")
                        .executes(ctx -> clearList(ctx.getSource(), listSupplier, literal)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createAbilityCooldownCommands(CommandRegistryAccess registryAccess) {
        return CommandManager.literal("ability_cooldowns")
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("entity", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .then(CommandManager.argument("cooldown", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Identifier id = RegistryEntryReferenceArgumentType.getSummonableEntityType(ctx, "entity").registryKey().getValue();
                                            int cooldown = IntegerArgumentType.getInteger(ctx, "cooldown");
                                            return setAbilityCooldown(ctx.getSource(), id.toString(), cooldown);
                                        }))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("entity", StringArgumentType.string())
                                .suggests((context, builder) -> CommandSource.suggestMatching(IdentityConfig.getInstance().getAbilityCooldownMap().keySet(), builder))
                                .executes(ctx -> removeAbilityCooldown(ctx.getSource(), StringArgumentType.getString(ctx, "entity")))))
                .then(CommandManager.literal("clear")
                        .executes(ctx -> clearAbilityCooldowns(ctx.getSource())));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createRequiredKillCommands(CommandRegistryAccess registryAccess) {
        return CommandManager.literal("required_kills")
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("entity", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .then(CommandManager.argument("kills", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Identifier id = RegistryEntryReferenceArgumentType.getSummonableEntityType(ctx, "entity").registryKey().getValue();
                                            int kills = IntegerArgumentType.getInteger(ctx, "kills");
                                            return setRequiredKillOverride(ctx.getSource(), id.toString(), kills);
                                        }))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("entity", StringArgumentType.string())
                                .suggests((context, builder) -> CommandSource.suggestMatching(IdentityConfig.getInstance().getRequiredKillsByType().keySet(), builder))
                                .executes(ctx -> removeRequiredKillOverride(ctx.getSource(), StringArgumentType.getString(ctx, "entity")))))
                .then(CommandManager.literal("clear")
                        .executes(ctx -> clearRequiredKillOverrides(ctx.getSource())));
    }

    private static int addToList(ServerCommandSource source, Supplier<List<String>> supplier, boolean caseInsensitive, String listName, String value) {
        List<String> list = supplier.get();
        boolean exists = caseInsensitive ? list.stream().anyMatch(entry -> entry.equalsIgnoreCase(value)) : list.contains(value);

        if (exists) {
            source.sendError(Text.literal(value + " is already present in " + formatKey(listName)));
            return 0;
        }

        list.add(value);
        persistConfig(source, Text.literal("Added " + value + " to " + formatKey(listName)));
        return 1;
    }

    private static int removeFromList(ServerCommandSource source, Supplier<List<String>> supplier, boolean caseInsensitive, String listName, String value) {
        List<String> list = supplier.get();
        boolean removed;

        if (caseInsensitive) {
            removed = list.removeIf(entry -> entry.equalsIgnoreCase(value));
        } else {
            removed = list.remove(value);
        }

        if (!removed) {
            source.sendError(Text.literal(value + " is not present in " + formatKey(listName)));
            return 0;
        }

        persistConfig(source, Text.literal("Removed " + value + " from " + formatKey(listName)));
        return 1;
    }

    private static int clearList(ServerCommandSource source, Supplier<List<String>> supplier, String listName) {
        List<String> list = supplier.get();

        if (list.isEmpty()) {
            source.sendFeedback(() -> Text.literal(formatKey(listName) + " is already empty"), false);
            return 0;
        }

        list.clear();
        persistConfig(source, Text.literal("Cleared " + formatKey(listName)));
        return 1;
    }

    private static int setAbilityCooldown(ServerCommandSource source, String entityId, int cooldown) {
        IdentityConfig.getInstance().getAbilityCooldownMap().put(entityId, cooldown);
        persistConfig(source, Text.literal("Set ability cooldown for " + entityId + " to " + cooldown));
        return 1;
    }

    private static int removeAbilityCooldown(ServerCommandSource source, String entityId) {
        Integer removed = IdentityConfig.getInstance().getAbilityCooldownMap().remove(entityId);
        if (removed == null) {
            source.sendError(Text.literal("No ability cooldown override exists for " + entityId));
            return 0;
        }

        persistConfig(source, Text.literal("Removed ability cooldown override for " + entityId));
        return 1;
    }

    private static int clearAbilityCooldowns(ServerCommandSource source) {
        Map<String, Integer> map = IdentityConfig.getInstance().getAbilityCooldownMap();
        if (map.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Ability cooldown overrides are already empty"), false);
            return 0;
        }

        map.clear();
        persistConfig(source, Text.literal("Cleared all ability cooldown overrides"));
        return 1;
    }

    private static int setRequiredKillOverride(ServerCommandSource source, String entityId, int kills) {
        IdentityConfig.getInstance().getRequiredKillsByType().put(entityId, kills);
        persistConfig(source, Text.literal("Set required kills for " + entityId + " to " + kills));
        return 1;
    }

    private static int removeRequiredKillOverride(ServerCommandSource source, String entityId) {
        Integer removed = IdentityConfig.getInstance().getRequiredKillsByType().remove(entityId);
        if (removed == null) {
            source.sendError(Text.literal("No required kill override exists for " + entityId));
            return 0;
        }

        persistConfig(source, Text.literal("Removed required kill override for " + entityId));
        return 1;
    }

    private static int clearRequiredKillOverrides(ServerCommandSource source) {
        Map<String, Integer> map = IdentityConfig.getInstance().getRequiredKillsByType();
        if (map.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Required kill overrides are already empty"), false);
            return 0;
        }

        map.clear();
        persistConfig(source, Text.literal("Cleared all required kill overrides"));
        return 1;
    }

    private static int setBooleanOption(ServerCommandSource source, String option, boolean value) {
        String key = option.toLowerCase(Locale.ROOT);
        Consumer<Boolean> setter = BOOLEAN_SETTERS.get(key);

        if (setter == null) {
            source.sendError(Text.literal("Unknown boolean option: " + option));
            return 0;
        }

        setter.accept(value);
        persistConfig(source, Text.literal("Set " + formatKey(key) + " to " + value));
        return 1;
    }

    private static int setIntegerOption(ServerCommandSource source, String option, int value) {
        String key = option.toLowerCase(Locale.ROOT);
        IntConsumer setter = INT_SETTERS.get(key);

        if (setter == null) {
            source.sendError(Text.literal("Unknown integer option: " + option));
            return 0;
        }

        if ("max_health".equals(key) && value < 1) {
            source.sendError(Text.literal("max health must be at least 1"));
            return 0;
        }

        setter.accept(value);
        persistConfig(source, Text.literal("Set " + formatKey(key) + " to " + value));
        return 1;
    }

    private static int setFloatOption(ServerCommandSource source, String option, float value) {
        String key = option.toLowerCase(Locale.ROOT);
        Consumer<Float> setter = FLOAT_SETTERS.get(key);

        if (setter == null) {
            source.sendError(Text.literal("Unknown float option: " + option));
            return 0;
        }

        if (value <= 0) {
            source.sendError(Text.literal("fly speed must be greater than 0"));
            return 0;
        }

        setter.accept(value);
        persistConfig(source, Text.literal("Set " + formatKey(key) + " to " + value));
        return 1;
    }

    private static int setStringOption(ServerCommandSource source, String option, String rawValue) {
        String key = option.toLowerCase(Locale.ROOT);

        if (!STRING_OPTIONS.contains(key)) {
            source.sendError(Text.literal("Unknown string option: " + option));
            return 0;
        }

        if ("forced_identity".equals(key)) {
            if (rawValue.equalsIgnoreCase("none") || rawValue.equalsIgnoreCase("null")) {
                IdentityConfig.getInstance().setForcedIdentity(null);
                persistConfig(source, Text.literal("Cleared forced identity"));
                return 1;
            }

            Identifier identifier = Identifier.tryParse(rawValue);
            if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
                source.sendError(Text.literal("Unknown entity: " + rawValue));
                return 0;
            }

            IdentityConfig.getInstance().setForcedIdentity(identifier.toString());
            persistConfig(source, Text.literal("Set forced identity to " + identifier));
            return 1;
        }

        return 0;
    }

    private static int reloadConfig(ServerCommandSource source) {
        if (IdentityPlatform.getReloader() == null) {
            source.sendError(Text.literal("No config reloader is registered"));
            return 0;
        }

        IdentityPlatform.getReloader().reloadConfig();
        source.sendFeedback(() -> Text.literal("Reloaded Identity config"), true);
        return 1;
    }

    private static void persistConfig(ServerCommandSource source, Text message) {
        source.sendFeedback(() -> message, true);

        if (IdentityPlatform.getReloader() != null) {
            IdentityPlatform.getReloader().saveConfig();
        } else {
            source.sendError(Text.literal("Unable to save config changes because no reloader is registered"));
        }
    }

    private static String formatKey(String key) {
        return key.replace('_', ' ');
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, b) -> {
            LiteralCommandNode<ServerCommandSource> rootNode = CommandManager
                    .literal("identity")
                    .requires(source -> source.hasPermissionLevel(2))
                    .build();

            /*
            Used to give the specified Identity to the specified Player.
             */
            LiteralCommandNode<ServerCommandSource> grantNode = CommandManager
                    .literal("grant")
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                            .then(CommandManager.literal("everything")
                                    .executes(context -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                        for (IdentityType<?> type : IdentityType.getAllTypes(player.getWorld())) {
                                            if(!PlayerUnlocks.has(player, type)) {
                                                PlayerUnlocks.unlock(player, type);
                                            }
                                        }

                                        return 1;
                                    })
                            )
                            .then(CommandManager.argument("identity", IdentifierArgumentType.identifier())
                                    .executes(context -> {
                                        grant(
                                                context.getSource().getPlayer(),
                                                EntityArgumentType.getPlayer(context, "player"),
                                                IdentifierArgumentType.getIdentifier(context, "identity"),
                                                null
                                        );
                                        return 1;
                                    })
                                    .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                            .executes(context -> {
                                                NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");

                                                grant(
                                                        context.getSource().getPlayer(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        IdentifierArgumentType.getIdentifier(context, "identity"),
                                                        nbt
                                                );

                                                return 1;
                                            })
                                    )
                            )
                    )
                    .build();

            LiteralCommandNode<ServerCommandSource> revokeNode = CommandManager
                    .literal("revoke")
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                            .then(CommandManager.literal("everything")
                                    .executes(context -> {
                                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                        for (IdentityType<?> type : IdentityType.getAllTypes(player.getWorld())) {
                                            if(PlayerUnlocks.has(player, type)) {
                                                PlayerUnlocks.revoke(player, type);
                                            }
                                        }

                                        return 1;
                                    })
                            )
                            .then(CommandManager.argument("identity", IdentifierArgumentType.identifier())
                                    .executes(context -> {
                                        revoke(
                                                context.getSource().getPlayer(),
                                                EntityArgumentType.getPlayer(context, "player"),
                                                IdentifierArgumentType.getIdentifier(context, "identity"),
                                                null
                                        );
                                        return 1;
                                    })
                                    .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                            .executes(context -> {
                                                NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");

                                                revoke(
                                                        context.getSource().getPlayer(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        IdentifierArgumentType.getIdentifier(context, "identity"),
                                                        nbt
                                                );

                                                return 1;
                                            })
                                    )
                            )
                    )
                    .build();

            LiteralCommandNode<ServerCommandSource> equip = CommandManager
                    .literal("equip")
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                            .then(CommandManager.argument("identity", IdentifierArgumentType.identifier())
                                    .executes(context -> {
                                        equip(context.getSource().getPlayer(),
                                                EntityArgumentType.getPlayer(context, "player"),
                                                IdentifierArgumentType.getIdentifier(context, "identity"),
                                                null);

                                        return 1;
                                    })
                                    .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                            .executes(context -> {
                                                NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");

                                                equip(context.getSource().getPlayer(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        IdentifierArgumentType.getIdentifier(context, "identity"),
                                                        nbt);

                                                return 1;
                                            })
                                    )
                            )
                    )
                    .build();

            LiteralCommandNode<ServerCommandSource> unequip = CommandManager
                    .literal("unequip")
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                            .executes(context -> {
                                unequip(
                                        context.getSource().getPlayer(),
                                        EntityArgumentType.getPlayer(context, "player")
                                );
                                return 1;
                            })
                    )
                    .build();

            LiteralCommandNode<ServerCommandSource> test = CommandManager
                    .literal("test")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.literal("not")
                                     .then(CommandManager.argument("identity", IdentifierArgumentType.identifier())
                                            .executes(context -> {
                                                return testNot(
                                                        context.getSource().getPlayer(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                         IdentifierArgumentType.getIdentifier(context, "identity")
                                             );
                                         })
                                     )
                             )
                             .then(CommandManager.argument("identity", IdentifierArgumentType.identifier())
                                     .executes(context -> {
                                         return test(
                                                 context.getSource().getPlayer(),
                                                 EntityArgumentType.getPlayer(context, "player"),
                                                 IdentifierArgumentType.getIdentifier(context, "identity")
                                         );
                                     })
                             )
                     )
                     .build();
            LiteralCommandNode<ServerCommandSource> offsetNode =
                    CommandManager.literal("offset")
                            .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        int v = IntegerArgumentType.getInteger(ctx, "value");
                                        EntityWidget.VERTICAL_OFFSET = v;
                                        ctx.getSource()
                                                .sendFeedback(
                                                        ()-> Text.literal("Entity‑grid Y‑offset set to §e" + v + "§r"),
                                                        false
                                                );
                                        return 1;
                                    })
                            ).build();

            LiteralCommandNode<ServerCommandSource> whitelistNode =
                    CommandManager.literal("whitelist")
                            .then(CommandManager.literal("enable")
                                    .executes(ctx -> {
                                        IdentityConfig.getInstance().setEnableSwaps(false);
                                        if (IdentityConfig.getInstance().logCommands()) {
                                            ctx.getSource().sendFeedback(() -> Text.literal("Enabled identity whitelist"), true);
                                        }
                                        return 1;
                                    }))
                            .then(CommandManager.literal("disable")
                                    .executes(ctx -> {
                                        IdentityConfig.getInstance().setEnableSwaps(true);
                                        if (IdentityConfig.getInstance().logCommands()) {
                                            ctx.getSource().sendFeedback(() -> Text.literal("Disabled identity whitelist"), true);
                                        }
                                        return 1;
                                    }))
                            .then(CommandManager.literal("add")
                                    .then(CommandManager.argument("player", StringArgumentType.string())
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "player");
                                                IdentityConfig.getInstance().allowedSwappers().add(name);
                                                if (IdentityConfig.getInstance().logCommands()) {
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Added " + name + " to identity whitelist"), true);
                                                }
                                                return 1;
                                            })))
                            .then(CommandManager.literal("remove")
                                    .then(CommandManager.argument("player", StringArgumentType.string())
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "player");
                                                IdentityConfig.getInstance().allowedSwappers().removeIf(n -> n.equalsIgnoreCase(name));
                                                if (IdentityConfig.getInstance().logCommands()) {
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Removed " + name + " from identity whitelist"), true);
                                                }
                                                return 1;
                                            })))
                            .build();
            LiteralCommandNode<ServerCommandSource> professionNode =
                    CommandManager.literal("identity_villager")
                            .requires(src -> true)
                            .then(CommandManager.literal("list")
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                        Map<String, NbtCompound> map = PlayerIdentity.getVillagerIdentities(player);
                                        if (map.isEmpty()) {
                                            player.sendMessage(Text.literal("You have no saved villager professions."), false);
                                            return 1;
                                        }
                                        player.sendMessage(Text.literal("Saved villager professions:"), false);
                                        map.forEach((name, tag) -> {
                                            String prof = tag.getString("ProfessionId");
                                            String dim = tag.getString("WorkstationDim");
                                            long posLong = tag.contains("WorkstationPos") ? tag.getLong("WorkstationPos") : Long.MIN_VALUE;
                                            net.minecraft.util.math.BlockPos blockPos = posLong == Long.MIN_VALUE ? null : net.minecraft.util.math.BlockPos.fromLong(posLong);
                                            String location = blockPos == null ? "?" : (blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
                                            player.sendMessage(Text.literal("- " + name + " -> " + prof + " @ " + dim + " " + location), false);
                                        });
                                        return 1;
                                    }))
                            .then(CommandManager.literal("show")
                                    .then(CommandManager.argument("name", StringArgumentType.string())
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                String name = StringArgumentType.getString(ctx, "name");
                                                Map<String, NbtCompound> map = PlayerIdentity.getVillagerIdentities(player);
                                                if (!map.containsKey(name)) {
                                                    player.sendMessage(Text.literal("No villager saved under name: " + name), false);
                                                    return 0;
                                                }
                                                NbtCompound tag = map.get(name);
                                                String prof = tag.getString("ProfessionId");
                                                String dim = tag.getString("WorkstationDim");
                                                long posLong = tag.contains("WorkstationPos") ? tag.getLong("WorkstationPos") : Long.MIN_VALUE;
                                                net.minecraft.util.math.BlockPos blockPos = posLong == Long.MIN_VALUE ? null : net.minecraft.util.math.BlockPos.fromLong(posLong);
                                                String location = blockPos == null ? "?" : (blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
                                                player.sendMessage(Text.literal("Villager '" + name + "' profession: " + prof + " @ " + dim + " " + location), false);
                                                return 1;
                                            })))
                            .then(CommandManager.literal("trade")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.literal("myself")
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                if (!IdentityConfig.getInstance().allowSelfTrading()) {
                                                    player.sendMessage(Text.translatable("identity.profession.trade.self_disabled"), false);
                                                    return 0;
                                                }

                                                LivingEntity identity = PlayerIdentity.getIdentity(player);
                                                if (!(identity instanceof VillagerEntity villager)) {
                                                    player.sendMessage(Text.translatable("identity.profession.trade.require_villager"), false);
                                                    return 0;
                                                }

                                                villager.interactMob(player, Hand.MAIN_HAND);
                                                return 1;
                                            })))
                            .build();
            LiteralArgumentBuilder<ServerCommandSource> configBuilder = CommandManager.literal("config")
                    .then(CommandManager.literal("boolean")
                            .then(CommandManager.argument("option", StringArgumentType.word()).suggests(BOOLEAN_OPTION_SUGGESTIONS)
                                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                                            .executes(ctx -> setBooleanOption(ctx.getSource(), StringArgumentType.getString(ctx, "option"), BoolArgumentType.getBool(ctx, "value"))))))
                    .then(CommandManager.literal("integer")
                            .then(CommandManager.argument("option", StringArgumentType.word()).suggests(INT_OPTION_SUGGESTIONS)
                                    .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                            .executes(ctx -> setIntegerOption(ctx.getSource(), StringArgumentType.getString(ctx, "option"), IntegerArgumentType.getInteger(ctx, "value"))))))
                    .then(CommandManager.literal("float")
                            .then(CommandManager.argument("option", StringArgumentType.word()).suggests(FLOAT_OPTION_SUGGESTIONS)
                                    .then(CommandManager.argument("value", FloatArgumentType.floatArg())
                                            .executes(ctx -> setFloatOption(ctx.getSource(), StringArgumentType.getString(ctx, "option"), FloatArgumentType.getFloat(ctx, "value"))))))
                    .then(CommandManager.literal("string")
                            .then(CommandManager.argument("option", StringArgumentType.word()).suggests(STRING_OPTION_SUGGESTIONS)
                                    .then(CommandManager.argument("value", StringArgumentType.greedyString()).suggests(FORCED_IDENTITY_SUGGESTIONS)
                                            .executes(ctx -> setStringOption(ctx.getSource(), StringArgumentType.getString(ctx, "option"), StringArgumentType.getString(ctx, "value"))))))
                    .then(createListCommand(registryAccess))
                    .then(createMapCommand(registryAccess))
                    .then(CommandManager.literal("reload")
                            .executes(ctx -> reloadConfig(ctx.getSource())));

            rootNode.addChild(grantNode);
            rootNode.addChild(revokeNode);
            rootNode.addChild(equip);
            rootNode.addChild(unequip);
            rootNode.addChild(test);
            rootNode.addChild(offsetNode);
            rootNode.addChild(whitelistNode);
            rootNode.addChild(professionNode);
            rootNode.addChild(configBuilder.build());

            dispatcher.getRoot().addChild(rootNode);
        });
    }

    private static int test(ServerPlayerEntity source, ServerPlayerEntity player, Identifier identity) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(identity);

        if(PlayerIdentity.getIdentity(player) != null && PlayerIdentity.getIdentity(player).getType().equals(type)) {
            if(IdentityConfig.getInstance().logCommands()) {
                source.sendMessage(Text.translatable("identity.test_positive", player.getDisplayName(), Text.translatable(type.getTranslationKey())), true);
            }

            return 1;
        }

        if(IdentityConfig.getInstance().logCommands()) {
            source.sendMessage(Text.translatable("identity.test_failed", player.getDisplayName(), Text.translatable(type.getTranslationKey())), true);
        }

        return 0;
    }

    private static int testNot(ServerPlayerEntity source, ServerPlayerEntity player, Identifier identity) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(identity);

        if(PlayerIdentity.getIdentity(player) != null && !PlayerIdentity.getIdentity(player).getType().equals(type)) {
            if(IdentityConfig.getInstance().logCommands()) {
                source.sendMessage(Text.translatable("identity.test_failed", player.getDisplayName(), Text.translatable(type.getTranslationKey())), true);
            }

            return 1;
        }

        if(IdentityConfig.getInstance().logCommands()) {
            source.sendMessage(Text.translatable("identity.test_positive", player.getDisplayName(), Text.translatable(type.getTranslationKey())), true);
        }

        return 0;
    }

    private static void grant(ServerPlayerEntity source, ServerPlayerEntity player, Identifier id, @Nullable NbtCompound nbt) {
        IdentityType<LivingEntity> type = new IdentityType(Registries.ENTITY_TYPE.get(id));
        Text name = Text.translatable(type.getEntityType().getTranslationKey());

        // If the specified granting NBT is not null, change the IdentityType to reflect potential variants.
        if(nbt != null) {
            NbtCompound copy = nbt.copy();
            copy.putString("id", id.toString());
            ServerWorld serverWorld = source.getServerWorld();
            Entity loaded = EntityType.loadEntityWithPassengers(copy, serverWorld, it -> it);
            if(loaded instanceof LivingEntity living) {
                type = new IdentityType<>(living);
                name = type.createTooltipText(living);
            }
        }

        if(!PlayerUnlocks.has(player, type)) {
            boolean result = PlayerUnlocks.unlock(player, type);

            if(result && IdentityConfig.getInstance().logCommands()) {
                player.sendMessage(Text.translatable("identity.unlock_entity", name), true);
                source.sendMessage(Text.translatable("identity.grant_success", name, player.getDisplayName()), true);
            }
        } else {
            if(IdentityConfig.getInstance().logCommands()) {
                source.sendMessage(Text.translatable("identity.already_has", player.getDisplayName(), name), true);
            }
        }
    }

    private static void revoke(ServerPlayerEntity source, ServerPlayerEntity player, Identifier id, @Nullable NbtCompound nbt) {
        IdentityType<LivingEntity> type = new IdentityType(Registries.ENTITY_TYPE.get(id));
        Text name = Text.translatable(type.getEntityType().getTranslationKey());

        // If the specified granting NBT is not null, change the IdentityType to reflect potential variants.
        if(nbt != null) {
            NbtCompound copy = nbt.copy();
            copy.putString("id", id.toString());
            ServerWorld serverWorld = source.getServerWorld();
            Entity loaded = EntityType.loadEntityWithPassengers(copy, serverWorld, it -> it);
            if(loaded instanceof LivingEntity living) {
                type = new IdentityType<>(living);
                name = type.createTooltipText(living);
            }
        }

        if(PlayerUnlocks.has(player, type)) {
            PlayerUnlocks.revoke(player, type);

            if(IdentityConfig.getInstance().logCommands()) {
                player.sendMessage(Text.translatable("identity.revoke_entity", name), true);
                source.sendMessage(Text.translatable("identity.revoke_success", name, player.getDisplayName()), true);
            }
        } else {
            if(IdentityConfig.getInstance().logCommands()) {
                source.sendMessage(Text.translatable("identity.does_not_have", player.getDisplayName(), name), true);
            }
        }
    }

    private static void equip(ServerPlayerEntity source, ServerPlayerEntity player, Identifier identity, @Nullable NbtCompound nbt) {
        Entity created;

        if(nbt != null) {
            NbtCompound copy = nbt.copy();
            copy.putString("id", identity.toString());
            ServerWorld serverWorld = source.getServerWorld();
            created = EntityType.loadEntityWithPassengers(copy, serverWorld, it -> it);
        } else {
            EntityType<?> entity = Registries.ENTITY_TYPE.get(identity);
            created = entity.create(player.getWorld());
        }

        if(created instanceof LivingEntity living) {
            @Nullable IdentityType<?> defaultType = IdentityType.from(living);

            if(defaultType != null) {
                boolean result = PlayerIdentity.updateIdentity(player, defaultType, (LivingEntity) created);
                if(result && IdentityConfig.getInstance().logCommands()) {
                    source.sendMessage(Text.translatable("identity.equip_success", Text.translatable(created.getType().getTranslationKey()), player.getDisplayName()), true);
                }
            }
        }
    }

    private static void unequip(ServerPlayerEntity source, ServerPlayerEntity player) {
        boolean result = PlayerIdentity.updateIdentity(player, null, null);

        if(result && IdentityConfig.getInstance().logCommands()) {
            source.sendMessage(Text.translatable("identity.unequip_success", player.getDisplayName()), false);
        }
    }
}

