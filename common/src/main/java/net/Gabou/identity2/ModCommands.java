package net.Gabou.identity2;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.Gabou.identity2.commands.TpRelCommand;
//import net.Gabou.identity2.commands.SilentFunctionCommand;
import net.Gabou.identity2.commands.AccelerateCommand;
import net.Gabou.identity2.commands.AccelerateToPosCommand;
import net.Gabou.identity2.commands.AccelerateAltCommand;
import net.Gabou.identity2.commands.ConvertToEntityCommand;
import net.Gabou.identity2.commands.ForLoopCommand;
import net.Gabou.identity2.commands.HealCommand;
import net.Gabou.identity2.commands.RunMultipleCommand;
import net.Gabou.identity2.commands.WithCommand;
import net.Gabou.identity2.commands.AirCommand;
import net.Gabou.identity2.commands.ModifyCustomEntityDataCommand;

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
            TpRelCommand.register(dispatcher);
            //SilentFunctionCommand.register(dispatcher);
            AccelerateCommand.register(dispatcher);
            AccelerateAltCommand.register(dispatcher);
            AccelerateToPosCommand.register(dispatcher);
            ConvertToEntityCommand.register(dispatcher);
            ForLoopCommand.register(dispatcher);
            RunMultipleCommand.register(dispatcher);
            HealCommand.register(dispatcher);
            AirCommand.register(dispatcher);
            ModifyCustomEntityDataCommand.register(dispatcher);
            WithCommand.register(dispatcher, registryAccess);
        });
    }
}
