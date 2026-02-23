package net.Gabou.identity2;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.Gabou.identity2.commands.IdentityCommand;
import net.Gabou.identity2.commands.IdentityProgressionCommand;

public final class ModCommands {
    private static boolean initialized = false;

    private ModCommands() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            IdentityCommand.register(dispatcher);
            IdentityProgressionCommand.register(dispatcher);
        });
    }
}
