package samcask.scheduledrestart;

import net.minecraft.server.MinecraftServer;

public class RestartNoPlayerScheduler {
    public static void playerConnected() {
        RestartScheduler.cancelScheduledRestart(RestartScheduler.RestartChannel.NoPlayerRestart);
    }

    public static void playerDisconnected(MinecraftServer server) {
        if (server.getCurrentPlayerCount() == 0) finalPlayerDisconnected(server);
    }

    public static void finalPlayerDisconnected(MinecraftServer server) {
        if (ScheduledRestart.config.noPlayerRestartDelay <= 0) return;
        RestartScheduler.scheduleRestart(server, ScheduledRestart.config.noPlayerRestartDelay, RestartScheduler.RestartChannel.NoPlayerRestart);
    }
}
