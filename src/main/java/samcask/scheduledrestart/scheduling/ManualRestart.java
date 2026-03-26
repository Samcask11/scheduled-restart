package samcask.scheduledrestart.scheduling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;

public class ManualRestart {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("restart")
                        .executes((context) ->
                                RestartHandler.restart(context.getSource().getServer()))
                        .then(Commands.argument("delay", TimeArgument.time())
                                .executes((context) ->
                                        RestartScheduler.scheduleRestart(context.getSource().getServer(),
                                                (long)IntegerArgumentType.getInteger(context, "delay") / 20,
                                                RestartScheduler.RestartChannel.ManualRestart))
                        ).then(Commands.literal("cancel")
                                .executes((context) ->
                                        RestartScheduler.cancelScheduledRestart(RestartScheduler.RestartChannel.ManualRestart))
                        )
        );
    }
}
