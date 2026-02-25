package net.Gabou.identity2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import dev.architectury.networking.NetworkManager;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.packets.OpenProgressionScreenS2CPacketPayload;
import net.Gabou.identity2.progression.MorphChargeManager;
import net.Gabou.identity2.progression.SoulAbsorptionManager;
import net.Gabou.identity2.progression.SoulJarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

public final class IdentityProgressionCommand {
    private IdentityProgressionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildProgressionLiteral("identity_progression"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> progressionSubcommand() {
        return buildProgressionLiteral("progression");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildProgressionLiteral(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
            .then(
                Commands.literal("ui")
                    .executes(context -> openUi(context.getSource(), null))
                    .then(
                        Commands.argument("target", EntityArgument.player())
                            .executes(context -> openUi(context.getSource(), EntityArgument.getPlayer(context, "target")))
                    )
            )
            .then(
                Commands.literal("charges")
                    .then(
                        Commands.literal("get")
                            .then(
                                Commands.argument("identity_id", ResourceLocationArgument.id())
                                    .executes(context -> getCharges(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "identity_id"),
                                        null
                                    ))
                                    .then(
                                        Commands.argument("target", EntityArgument.player())
                                            .executes(context -> getCharges(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "identity_id"),
                                                EntityArgument.getPlayer(context, "target")
                                            ))
                                    )
                            )
                    )
                    .then(
                        Commands.literal("add")
                            .then(
                                Commands.argument("identity_id", ResourceLocationArgument.id())
                                    .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(context -> addCharges(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "identity_id"),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> addCharges(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(context, "identity_id"),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
            )
            .then(
                Commands.literal("jar")
                    .then(
                        Commands.literal("list")
                            .executes(context -> listJars(context.getSource(), null))
                            .then(
                                Commands.argument("target", EntityArgument.player())
                                    .executes(context -> listJars(context.getSource(), EntityArgument.getPlayer(context, "target")))
                            )
                    )
                    .then(
                        Commands.literal("create")
                            .then(
                                Commands.argument("jar_id", StringArgumentType.word())
                                    .then(
                                        Commands.argument("tier", StringArgumentType.word())
                                            .executes(context -> createJar(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jar_id"),
                                                StringArgumentType.getString(context, "tier"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> createJar(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jar_id"),
                                                        StringArgumentType.getString(context, "tier"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("upgrade")
                            .then(
                                Commands.argument("jar_id", StringArgumentType.word())
                                    .then(
                                        Commands.argument("tier", StringArgumentType.word())
                                            .executes(context -> upgradeJar(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jar_id"),
                                                StringArgumentType.getString(context, "tier"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> upgradeJar(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jar_id"),
                                                        StringArgumentType.getString(context, "tier"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("store")
                            .then(
                                Commands.argument("jar_id", StringArgumentType.word())
                                    .then(
                                        Commands.argument("identity_id", ResourceLocationArgument.id())
                                            .executes(context -> storeMorph(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jar_id"),
                                                ResourceLocationArgument.getId(context, "identity_id"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> storeMorph(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jar_id"),
                                                        ResourceLocationArgument.getId(context, "identity_id"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("remove")
                            .then(
                                Commands.argument("jar_id", StringArgumentType.word())
                                    .then(
                                        Commands.argument("identity_id", ResourceLocationArgument.id())
                                            .executes(context -> removeMorph(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jar_id"),
                                                ResourceLocationArgument.getId(context, "identity_id"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> removeMorph(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jar_id"),
                                                        ResourceLocationArgument.getId(context, "identity_id"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("absorb")
                            .then(
                                Commands.argument("jar_id", StringArgumentType.word())
                                    .then(
                                        Commands.argument("identity_id", ResourceLocationArgument.id())
                                            .executes(context -> absorbMorph(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jar_id"),
                                                ResourceLocationArgument.getId(context, "identity_id"),
                                                null
                                            ))
                                            .then(
                                                Commands.argument("target", EntityArgument.player())
                                                    .executes(context -> absorbMorph(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jar_id"),
                                                        ResourceLocationArgument.getId(context, "identity_id"),
                                                        EntityArgument.getPlayer(context, "target")
                                                    ))
                                            )
                                    )
                            )
                    )
            );
    }

    private static int openUi(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        NetworkManager.sendToPlayer(player, new OpenProgressionScreenS2CPacketPayload());
        source.sendSuccess(() -> Component.literal("Opened progression UI for " + player.getName().getString()), false);
        return 1;
    }

    private static int getCharges(CommandSourceStack source, ResourceLocation identityId, ServerPlayer target) {
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }

        int charges = MorphChargeManager.getCharges(player, identityId);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " has " + charges + " charges for " + identityId), false);
        return 1;
    }

    private static int addCharges(CommandSourceStack source, ResourceLocation identityId, int amount, ServerPlayer target) {
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }

        MorphChargeManager.addCharges(player, identityId, amount);
        int charges = MorphChargeManager.getCharges(player, identityId);
        source.sendSuccess(
            () -> Component.literal("Added " + amount + " charges to " + identityId + " for " + player.getName().getString() + " (now " + charges + ")"),
            true
        );
        return 1;
    }

    private static int listJars(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }

        List<SoulJarManager.SoulJarData> jars = SoulJarManager.getSoulJars(player);
        if (jars.isEmpty()) {
            source.sendSystemMessage(Component.literal("No soul jars for " + player.getName().getString()));
            return 1;
        }

        source.sendSystemMessage(Component.literal("Soul jars for " + player.getName().getString() + ":"));
        for (SoulJarManager.SoulJarData jar : jars) {
            int capacity = SoulJarManager.getJarCapacity(jar.tier());
            source.sendSystemMessage(
                Component.literal("- " + jar.jarId() + " [" + jar.tier() + "] " + jar.morphs().size() + "/" + capacity)
            );
            for (SoulJarManager.StoredMorphData morph : jar.morphs()) {
                String marker = morph.permanent() ? "permanent" : "stored";
                source.sendSystemMessage(Component.literal("  " + morph.identityId() + " (" + marker + ")"));
            }
        }
        return jars.size();
    }

    private static int createJar(CommandSourceStack source, String jarId, String tier, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        if (!SoulJarManager.createJar(player, jarId, tier)) {
            source.sendFailure(Component.literal("Could not create jar " + jarId + " for " + player.getName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Created jar " + jarId + " [" + tier + "] for " + player.getName().getString()), true);
        return 1;
    }

    private static int upgradeJar(CommandSourceStack source, String jarId, String tier, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        if (!SoulJarManager.upgradeJar(player, jarId, tier)) {
            source.sendFailure(Component.literal("Could not upgrade jar " + jarId + " for " + player.getName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Upgraded jar " + jarId + " to [" + tier + "] for " + player.getName().getString()), true);
        return 1;
    }

    private static int storeMorph(CommandSourceStack source, String jarId, ResourceLocation identityId, ServerPlayer target) {
        if (!IdentityProgression.isMorphableIdentity(identityId)) {
            source.sendFailure(Component.literal("Unsupported identity: " + identityId));
            return 0;
        }
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        if (!SoulJarManager.storeMorphInJar(player, jarId, identityId, new CompoundTag())) {
            source.sendFailure(Component.literal("Could not store morph " + identityId + " in jar " + jarId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Stored morph " + identityId + " in jar " + jarId), true);
        return 1;
    }

    private static int removeMorph(CommandSourceStack source, String jarId, ResourceLocation identityId, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        if (!SoulJarManager.removeMorphFromJar(player, jarId, identityId, new CompoundTag())) {
            source.sendFailure(Component.literal("Could not remove morph " + identityId + " from jar " + jarId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Removed morph " + identityId + " from jar " + jarId), true);
        return 1;
    }

    private static int absorbMorph(CommandSourceStack source, String jarId, ResourceLocation identityId, ServerPlayer target) {
        ServerPlayer player = resolveTarget(source, target);
        if (player == null) {
            source.sendFailure(Component.literal("Specify a target player."));
            return 0;
        }
        SoulAbsorptionManager.AbsorptionResult result = SoulAbsorptionManager.absorbMorph(player, jarId, identityId, new CompoundTag());
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static ServerPlayer resolveTarget(CommandSourceStack source, ServerPlayer explicitTarget) {
        if (explicitTarget != null) {
            return explicitTarget;
        }
        return source.getPlayer();
    }
}
