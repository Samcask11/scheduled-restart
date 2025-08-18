package samcask.scheduledrestart;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class RestartAutoScheduler {
    static LocalDateTime currentTime;
    static LocalDateTime restartTime;

    public static void scheduleAutoRestartInterval(MinecraftServer server, long delaySeconds) {
        if (delaySeconds == 0) {
            ScheduledRestart.logInfo("Failed to schedule interval restart, no interval set.");
            return;
        }
        RestartScheduler.scheduleRestart(server, delaySeconds);
    }

    public static void scheduleAutoRestartDaily(MinecraftServer server, String[] times) {
        if (times.length == 0) {
            ScheduledRestart.logInfo("Failed to schedule daily restart, no times set");
            return;
        }
        initialiseTimes(times);
        RestartScheduler.scheduleRestart(server, getTimeToNextRestart(times));
    }

    private static int getTimeToNextRestart(String[] times) {
        int delay = (int) Duration.between(currentTime, restartTime).toSeconds();
        int smallestDelay = delay;
        for (int i = 1; i < times.length; i++) {
            restartTime = LocalDateTime.now().with(LocalTime.parse(times[i]));
            if (currentTime.isAfter(restartTime)) restartTime = restartTime.plusDays(1);
            delay = (int)Duration.between(currentTime, restartTime).toSeconds();
            if (delay < smallestDelay) smallestDelay = delay;
        }
        return smallestDelay;
    }

    private static void initialiseTimes(String[] times) {
        currentTime = LocalDateTime.now();
        restartTime = LocalDateTime.now().with(LocalTime.parse(times[0]));
        if (currentTime.isAfter(restartTime)) restartTime = restartTime.plusDays(1);
    }
}
