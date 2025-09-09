package samcask.scheduledrestart;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
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
    static ScheduledFuture<?> scheduledManualRestart = null;
    static ScheduledFuture<?> scheduledAutoRestart = null;
    static ScheduledFuture<?> scheduledNoPlayerRestart = null;
    static ScheduledFuture<?>[] scheduledManualAnnouncements = new ScheduledFuture<?>[]{};
    static ScheduledFuture<?>[] scheduledAutoAnnouncements = new ScheduledFuture<?>[]{};

    public enum RestartChannel {
        ManualRestart,
        AutoRestart,
        NoPlayerRestart
    }

    public static int scheduleRestart(MinecraftServer server, long delaySeconds, RestartChannel restartChannel) {
        cancelScheduledRestart(restartChannel);
        setScheduledRestart(restartChannel, createScheduledRestart(server, delaySeconds));
        createScheduledAnnouncements(server, delaySeconds, getScheduledAnnouncements(restartChannel));
        ScheduledRestart.logInfo("New restart scheduled at time " + LocalDateTime.now().plusSeconds(delaySeconds));
        return 1;
    }

    public static ScheduledFuture<?> getScheduledRestart(RestartChannel restartChannel) {
        return switch (restartChannel) {
            case ManualRestart -> scheduledManualRestart;
            case AutoRestart -> scheduledAutoRestart;
            case NoPlayerRestart -> scheduledNoPlayerRestart;
        };
    }

    public static ScheduledFuture<?>[] getScheduledAnnouncements(RestartChannel restartChannel) {
        return switch (restartChannel) {
            case ManualRestart -> scheduledManualAnnouncements;
            case AutoRestart -> scheduledAutoAnnouncements;
            default -> null;
        };
    }

    public static int cancelScheduledRestart(RestartChannel restartChannel) {
        ScheduledFuture<?> scheduledRestart = getScheduledRestart(restartChannel);
        if (scheduledRestart == null || scheduledRestart.isDone()) return 0;
        cancelScheduledRestart(scheduledRestart);
        cancelScheduledAnnouncements(getScheduledAnnouncements(restartChannel));
        ScheduledRestart.logInfo("Cancelled existing scheduled restart.");
        return 1;
    }

    private static void cancelScheduledRestart(ScheduledFuture<?> scheduledRestart) {
        scheduledRestart.cancel(false);
    }

    private static void cancelScheduledAnnouncements(ScheduledFuture<?>[] scheduledAnnouncements) {
        if (scheduledAnnouncements == null) return;
        for (ScheduledFuture<?> scheduledAnnouncement : scheduledAnnouncements) {
            if (scheduledAnnouncement != null) scheduledAnnouncement.cancel(false);
        }
    }

    private static void setScheduledRestart(RestartChannel restartChannel, ScheduledFuture<?> scheduledRestart) {
        switch (restartChannel) {
            case ManualRestart:
                scheduledManualRestart = scheduledRestart;
            case AutoRestart:
                scheduledAutoRestart = scheduledRestart;
            case NoPlayerRestart:
                scheduledNoPlayerRestart = scheduledRestart;
        };
    }

    private static ScheduledFuture<?> createScheduledRestart(MinecraftServer server, long delaySeconds) {
        return scheduler.schedule(
                () -> server.execute(
                        () -> RestartHandler.restart(server)
                ), delaySeconds, TimeUnit.SECONDS
        );
    }

    private static void createScheduledAnnouncements(MinecraftServer server, long delaySeconds, ScheduledFuture<?>[] scheduledAnnouncements) {
        for (int i = 0; i < ScheduledRestart.config.restartWarningTimes.length; i++) {
            tryCreateScheduledAnnouncement(server, delaySeconds, i, scheduledAnnouncements);
        }
    }

    private static void tryCreateScheduledAnnouncement(MinecraftServer server, long delaySeconds, int i, ScheduledFuture<?>[] scheduledAnnouncements) {
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
        message = formatRestartWarning(constructRestartWarning(timeToRestart, message));
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.of("[Server] " + message), false);
        }
        ScheduledRestart.logInfo(message.toString());
    }

    private static StringBuilder formatRestartWarning(StringBuilder message) {
        StringBuilder messageBuilder;
        message.delete(message.toString().length() - 2, message.toString().length()).append(".");
        int lastCommaIndex = message.lastIndexOf(",");
        if (lastCommaIndex < 0) messageBuilder = message;
        else {
            messageBuilder = new StringBuilder();
            messageBuilder
                    .append(message, 0, lastCommaIndex)
                    .append(" and")
                    .append(message, lastCommaIndex + 1, message.toString().length());
        }
        return messageBuilder;
    }

    private static StringBuilder constructRestartWarning(Duration timeToRestart, StringBuilder message) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(message);
        constructRestartWarningSegment(messageBuilder, timeToRestart.toDaysPart(), "day", "days");
        constructRestartWarningSegment(messageBuilder, timeToRestart.toHoursPart(), "hour", "hours");
        constructRestartWarningSegment(messageBuilder, timeToRestart.toMinutesPart(), "minute", "minutes");
        constructRestartWarningSegment(messageBuilder, timeToRestart.toSecondsPart(), "second", "seconds");
        return messageBuilder;
    }

    private static void constructRestartWarningSegment(StringBuilder message, long timespan, String singular, String plural) {
        if (timespan > 0) message
                .append(timespan)
                .append(" ")
                .append(timespan == 1 ? singular : plural)
                .append(", ");
    }
}
