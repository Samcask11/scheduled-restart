package samcask.scheduledrestart;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    static ScheduledFuture<?> scheduledRestart = null;
    static ScheduledFuture<?>[] scheduledAnnouncements;

    public static int scheduleRestart(MinecraftServer server, long delaySeconds) {
        cancelScheduledRestart();
        createSchedulerRestartTask(server, delaySeconds);
        createSchedulerAnnouncementTasks(server, delaySeconds);
        ScheduledRestart.logInfo("New restart scheduled at time " + LocalDateTime.now().plusSeconds(delaySeconds));
        return 1;
    }

    public static int cancelScheduledRestart() {
        if (scheduledRestart == null || scheduledRestart.isDone()) return 0;
        cancelSchedulerRestartTask();
        cancelSchedulerAnnouncementTasks();
        ScheduledRestart.logInfo("Cancelled existing scheduled restart.");
        return 1;
    }

    private static void cancelSchedulerRestartTask() {
        scheduledRestart.cancel(false);
        scheduledRestart = null;
    }

    private static void cancelSchedulerAnnouncementTasks() {
        for (ScheduledFuture<?> scheduledAnnouncement : scheduledAnnouncements) {
            if (scheduledAnnouncement != null) scheduledAnnouncement.cancel(false);
        }
        scheduledAnnouncements = null;
    }

    private static void createSchedulerRestartTask(MinecraftServer server, long delaySeconds) {
        scheduledRestart = scheduler.schedule(
                () -> server.execute(
                        () -> RestartHandler.restart(server)
                ), delaySeconds, TimeUnit.SECONDS
        );
    }

    private static void createSchedulerAnnouncementTasks(MinecraftServer server, long delaySeconds) {
        scheduledAnnouncements = new ScheduledFuture<?>[ScheduledRestart.config.restartWarningTimes.length];
        for (int i = 0; i < ScheduledRestart.config.restartWarningTimes.length; i++) {
            tryCreateSchedulerAnnouncementTask(server, delaySeconds, i);
        }
    }

    private static void tryCreateSchedulerAnnouncementTask(MinecraftServer server, long delaySeconds, int i) {
        int announcementDelay = ScheduledRestart.config.restartWarningTimes[i];
        if (delaySeconds - announcementDelay > 0) {
            scheduledAnnouncements[i] = scheduler.schedule(
                    () -> server.execute(
                            () -> announceRestart(server, announcementDelay)
                    ), delaySeconds - announcementDelay, TimeUnit.SECONDS
            );
        }
    }

    public static void announceRestart(MinecraftServer server, int secondsToRestart) {
        if (secondsToRestart == 0) return;
        StringBuilder message = new StringBuilder("The server will restart in ");
        Duration timeToRestart = Duration.ofSeconds(secondsToRestart);
        constructRestartWarning(timeToRestart, message);
        message = formatRestartWarning(message);
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) player.sendMessageToClient(Text.literal("[Server] " + message), false);
        ScheduledRestart.logInfo(message.toString());
    }

    private static StringBuilder formatRestartWarning(StringBuilder message) {
        StringBuilder messageBuilder;
        message.delete(message.toString().length() - 2, message.toString().length()).append(".");
        int lastCommaIndex = message.lastIndexOf(",");
        if (lastCommaIndex < 0) messageBuilder = message;
        else {
            messageBuilder = new StringBuilder();
            messageBuilder.append(message, 0, lastCommaIndex).append(" and").append(message, lastCommaIndex + 1, message.toString().length());
        }
        return messageBuilder;
    }

    private static void constructRestartWarning(Duration timeToRestart, StringBuilder message) {
        if (timeToRestart.toDaysPart() > 0) message.append(timeToRestart.toDaysPart()).append(timeToRestart.toDaysPart() == 1 ? " day, " : " days, ");
        if (timeToRestart.toHoursPart() > 0) message.append(timeToRestart.toHoursPart()).append(timeToRestart.toHoursPart() == 1 ? " hour, " : " hours, ");
        if (timeToRestart.toMinutesPart() > 0) message.append(timeToRestart.toMinutesPart()).append(timeToRestart.toMinutesPart() == 1 ? " minute, " : " minutes, ");
        if (timeToRestart.toSecondsPart() > 0) message.append(timeToRestart.toSecondsPart()).append(timeToRestart.toSecondsPart() == 1 ? " second, " : " seconds, ");
    }
}
