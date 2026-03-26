package samcask.scheduledrestart.scheduling;

import net.minecraft.server.MinecraftServer;
import samcask.scheduledrestart.ScheduledRestart;

public class NoPlayerRestart {
    public static void playerConnected() {
        RestartScheduler.cancelScheduledRestart(RestartScheduler.RestartChannel.NoPlayerRestart);
    }

    public static void playerDisconnected(MinecraftServer server) {
        if (server.getCurrentPlayerCount() == 0) finalPlayerDisconnected(server);
    }

    public static void finalPlayerDisconnected(MinecraftServer server) {
        if (ScheduledRestart.CONFIG.noPlayerRestartDelay <= 0) return;
        RestartScheduler.scheduleRestart(server, ScheduledRestart.CONFIG.noPlayerRestartDelay, RestartScheduler.RestartChannel.NoPlayerRestart);
    }
}
