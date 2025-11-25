package samcask.scheduledrestart;

import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class RestartAutoScheduler {
    static LocalDateTime currentTime;
    static LocalDateTime restartTime;

    public static void scheduleAutoRestartInterval(MinecraftServer server, long delaySeconds) {
        if (delaySeconds == 0) {
            ScheduledRestart.logInfo("Failed to schedule interval restart, no interval set.");
            return;
        }
        RestartScheduler.scheduleRestart(server, delaySeconds, RestartScheduler.RestartChannel.AutoRestart);
    }

    public static void scheduleAutoRestartDaily(MinecraftServer server, ArrayList<LocalTime> times) {
        if (times.isEmpty()) {
            ScheduledRestart.logInfo("Failed to schedule daily restart, no times set");
            return;
        }
        initialiseTimes(times);
        RestartScheduler.scheduleRestart(server, getTimeToNextDailyRestart(times), RestartScheduler.RestartChannel.AutoRestart);
    }

    private static void initialiseTimes(ArrayList<LocalTime> times) {
        currentTime = LocalDateTime.now();
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime nextRestartTime = LocalDateTime.now().with(times.get(i));
            while (currentTime.isAfter(nextRestartTime)) nextRestartTime = nextRestartTime.plusDays(1);
            if (i == 0 || restartTime.isAfter(nextRestartTime)) restartTime = nextRestartTime;
        }
    }

    private static int getTimeToNextDailyRestart(ArrayList<LocalTime> times) {
        int delay = (int) Duration.between(currentTime, restartTime).toSeconds();
        int smallestDelay = delay;
        for (int i = 1; i < times.size(); i++) {
            restartTime = LocalDateTime.now().with(times.get(i));
            if (currentTime.isAfter(restartTime)) restartTime = restartTime.plusDays(1);
            delay = (int)Duration.between(currentTime, restartTime).toSeconds();
            if (delay < smallestDelay) smallestDelay = delay;
        }
        return smallestDelay;
    }
}
