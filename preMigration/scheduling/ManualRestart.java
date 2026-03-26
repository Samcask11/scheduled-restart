package samcask.scheduledrestart.scheduling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.arguments.TimeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ManualRestart {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
                CommandManager.literal("restart")
                        .executes((context) ->
                                RestartHandler.restart(context.getSource().getMinecraftServer()))
                        .then(CommandManager.argument("delay", TimeArgumentType.time())
                                .executes((context) ->
                                        RestartScheduler.scheduleRestart(context.getSource().getMinecraftServer(),
                                                (long)IntegerArgumentType.getInteger(context, "delay") / 20,
                                                RestartScheduler.RestartChannel.ManualRestart))
                        ).then(CommandManager.literal("cancel")
                                .executes((context) ->
                                        RestartScheduler.cancelScheduledRestart(RestartScheduler.RestartChannel.ManualRestart))
                        )
        );
    }
}
