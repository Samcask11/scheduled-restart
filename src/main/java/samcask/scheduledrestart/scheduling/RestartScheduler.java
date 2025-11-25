package samcask.scheduledrestart.scheduling;

import net.minecraft.server.MinecraftServer;
import samcask.scheduledrestart.ScheduledRestart;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RestartScheduler {
    static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RestartScheduler");
        t.setDaemon(true);
        return t;
    });
    static HashMap<RestartChannel, ScheduledFuture<?>> scheduledRestarts = new HashMap<>();
    static HashMap<RestartScheduler.RestartChannel, ScheduledFuture<?>[]> scheduledAnnouncements = new HashMap<>();

    public enum RestartChannel {
        ManualRestart,
        AutoRestart,
        NoPlayerRestart
    }

    public static int scheduleRestart(MinecraftServer server, long secondsUntilRestart, RestartScheduler.RestartChannel restartChannel) {
        cancelScheduledRestart(restartChannel);

        scheduledRestarts.put(restartChannel, createScheduledRestart(server, secondsUntilRestart));

        scheduleAnnouncements(server, secondsUntilRestart, restartChannel);

        ScheduledRestart.logInfo("New restart scheduled on channel " + restartChannel + " at time " + LocalDateTime.now().plusSeconds(secondsUntilRestart));
        return 1;
    }

    private static ScheduledFuture<?> createScheduledRestart(MinecraftServer server, long secondsUntilRestart) {
        return scheduler.schedule(
                () -> server.execute(
                        () -> RestartHandler.restart(server)
                ), secondsUntilRestart, TimeUnit.SECONDS
        );
    }

    public static int cancelScheduledRestart(RestartScheduler.RestartChannel restartChannel) {
        ScheduledFuture<?> scheduledRestart = scheduledRestarts.get(restartChannel);
        if (scheduledRestart == null || scheduledRestart.isDone()) return 0;

        scheduledRestart.cancel(false);

        cancelScheduledAnnouncements(restartChannel);

        ScheduledRestart.logInfo("Cancelled existing scheduled restart on channel " + restartChannel);
        return 1;
    }

    private static void scheduleAnnouncements(MinecraftServer server, long secondsUntilRestart, RestartScheduler.RestartChannel restartChannel) {
        ScheduledFuture<?>[] newAnnouncements = new ScheduledFuture<?>[ScheduledRestart.CONFIG.restartWarningTimes.size()];
        for (int i = 0; i < newAnnouncements.length; i++) {
            newAnnouncements[i] = createScheduledAnnouncement(server, secondsUntilRestart, ScheduledRestart.CONFIG.restartWarningTimes.get(i));
        }

        scheduledAnnouncements.put(restartChannel, newAnnouncements);
    }

    private static ScheduledFuture<?> createScheduledAnnouncement(MinecraftServer server, long secondsUntilRestart, int secondsInAdvance) {
        if (secondsUntilRestart - secondsInAdvance > 0) {
            return scheduler.schedule(
                    () -> server.execute(
                            () -> RestartAnnouncer.announceRestart(server, secondsInAdvance)
                    ), secondsUntilRestart - secondsInAdvance, TimeUnit.SECONDS
            );
        }
        return null;
    }

    private static void cancelScheduledAnnouncements(RestartScheduler.RestartChannel restartChannel) {
        if (scheduledAnnouncements.get(restartChannel) == null) return;
        for (ScheduledFuture<?> scheduledAnnouncement : scheduledAnnouncements.get(restartChannel)) {
            if (scheduledAnnouncement != null) scheduledAnnouncement.cancel(false);
        }
    }
}
