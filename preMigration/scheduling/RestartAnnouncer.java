package samcask.scheduledrestart.scheduling;

import net.minecraft.server.MinecraftServer;
import samcask.scheduledrestart.ScheduledRestart;

import java.time.Duration;

public class RestartAnnouncer {
    public static void announceRestart(MinecraftServer server, int secondsToRestart) {
        if (secondsToRestart == 0) return;
        StringBuilder message = new StringBuilder("The server will restart in ");
        Duration timeToRestart = Duration.ofSeconds(secondsToRestart);
        message = formatRestartWarning(constructRestartWarning(timeToRestart, message));
        ScheduledRestart.sendAnnouncement(server, message.toString(), true);
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
