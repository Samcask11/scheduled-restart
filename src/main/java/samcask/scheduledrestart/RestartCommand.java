package samcask.scheduledrestart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RestartCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
                CommandManager.literal("restart")
                        .executes((context) ->
                                RestartHandler.restart(context.getSource().getServer()))
                        .then(CommandManager.argument("delay", TimeArgumentType.time())
                                .executes((context) ->
                                        RestartScheduler.scheduleRestart(context.getSource().getServer(), (long)IntegerArgumentType.getInteger(context, "delay") / 20))
                        ).then(CommandManager.literal("cancel")
                                .executes((context) ->
                                        RestartScheduler.cancelScheduledRestart())
                        )
        );
    }
}
