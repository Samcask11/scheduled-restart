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
        if (ScheduledRestart.CONFIG.noPlayerRestartDelay.value <= 0) return;
        RestartScheduler.scheduleRestart(server, ScheduledRestart.CONFIG.noPlayerRestartDelay.value, RestartScheduler.RestartChannel.NoPlayerRestart);
    }
}
