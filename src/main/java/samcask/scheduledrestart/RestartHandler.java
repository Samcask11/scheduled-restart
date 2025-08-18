package samcask.scheduledrestart;

import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RestartHandler {
    static final ScheduledExecutorService scheduler;
    static ScheduledFuture<?> scheduledRestart = null;
    static ScheduledFuture<?>[] scheduledAnnouncements;

    static {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RestartScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static int restart(MinecraftServer server) {
        String startScriptPath;
        Path rootPath;
        Path fullPath;
        String os = System.getProperty("os.name").toLowerCase();
        String[] command;

        if (!ScheduledRestart.config.startScriptPath.isEmpty()) {
            startScriptPath = ScheduledRestart.config.startScriptPath;
        } else if (os.contains("win")) {
            startScriptPath = "start.bat";
        } else {
            startScriptPath = "start.sh";
        }

        try {
            rootPath = Path.of(".");
            fullPath = rootPath.toRealPath().resolve(startScriptPath);
            if (!Files.exists(fullPath)) {
                ScheduledRestart.logError("Start script not found.");
                return 0;
            }
        } catch (IOException e) {
            ScheduledRestart.logError("Could not find root directory.");
            return 0;
        }

        if (os.contains("win")) {
            command = new String[] { "cmd", "/c", "start", "", startScriptPath };
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            command = new String[] { "bash", startScriptPath };
        } else {
            ScheduledRestart.logError("Operating system not supported.");
            return 0;
        }

        ScheduledRestart.logInfo("Automatically restarting server...");

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.disconnect(Text.literal(ScheduledRestart.config.kickMessage));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Runtime.getRuntime().exec(command, null, rootPath.toFile());
            } catch (IOException e) {
                ScheduledRestart.logError("Failed to run start script.");
            }
        }));
        server.stop(false);
        return 1;
    }

    public static int scheduleRestart(MinecraftServer server, long delaySeconds) {
        cancelRestart();

        LocalDateTime restartTime = LocalDateTime.now().plusSeconds(delaySeconds);
        scheduledRestart = scheduler.schedule(
                () -> server.execute(
                        () -> restart(server)
                ), delaySeconds, TimeUnit.SECONDS
        );

        scheduledAnnouncements = new ScheduledFuture<?>[ScheduledRestart.config.restartWarningTimes.length];
        for (int i = 0; i < ScheduledRestart.config.restartWarningTimes.length; i++) {
            int announcementDelay = ScheduledRestart.config.restartWarningTimes[i];
            if (delaySeconds - announcementDelay > 0) {
                scheduledAnnouncements[i] = scheduler.schedule(
                        () -> server.execute(
                                () -> announceRestart(server, announcementDelay)
                        ), delaySeconds - announcementDelay, TimeUnit.SECONDS
                );
            }
        }

        ScheduledRestart.logInfo("New restart scheduled at time " + restartTime);
        return 1;
    }

    public static void scheduleAutoRestartInterval(MinecraftServer server, long delaySeconds) {
        if (delaySeconds == 0) {
            ScheduledRestart.logInfo("Failed to schedule interval restart, no interval set.");
            return;
        }

        scheduleRestart(server, delaySeconds);
    }

    public static void scheduleAutoRestartDaily(MinecraftServer server, String[] times) {
        if (times.length == 0) {
            ScheduledRestart.logInfo("Failed to schedule daily restart, no times set");
            return;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime restartTime;
        restartTime = LocalDateTime.now().with(LocalTime.parse(times[0]));
        if (currentTime.isAfter(restartTime)) restartTime = restartTime.plusDays(1);
        int delay = (int)Duration.between(currentTime, restartTime).toSeconds();
        int smallestDelay = delay;
        for (int i = 1; i < times.length; i++) {
            restartTime = LocalDateTime.now().with(LocalTime.parse(times[i]));
            if (currentTime.isAfter(restartTime)) restartTime = restartTime.plusDays(1);
            delay = (int)Duration.between(currentTime, restartTime).toSeconds();
            if (delay < smallestDelay) smallestDelay = delay;
        }

        scheduleRestart(server, smallestDelay);
    }

    public static void announceRestart(MinecraftServer server, int secondsToRestart) {
        if (secondsToRestart == 0) return;

        StringBuilder messageBuilder = new StringBuilder("The server will restart in ");
        Duration timeToRestart = Duration.ofSeconds(secondsToRestart);
        if (timeToRestart.toDaysPart() > 0) messageBuilder.append(timeToRestart.toDaysPart()).append(timeToRestart.toDaysPart() == 1 ? " day, " : " days, ");
        if (timeToRestart.toHoursPart() > 0) messageBuilder.append(timeToRestart.toHoursPart()).append(timeToRestart.toHoursPart() == 1 ? " hour, " : " hours, ");
        if (timeToRestart.toMinutesPart() > 0) messageBuilder.append(timeToRestart.toMinutesPart()).append(timeToRestart.toMinutesPart() == 1 ? " minute, " : " minutes, ");
        if (timeToRestart.toSecondsPart() > 0) messageBuilder.append(timeToRestart.toSecondsPart()).append(timeToRestart.toSecondsPart() == 1 ? " second, " : " seconds, ");

        messageBuilder.delete(messageBuilder.toString().length() - 2, messageBuilder.toString().length()).append(".");
        int lastCommaIndex = messageBuilder.lastIndexOf(",");
        StringBuilder message;
        if (lastCommaIndex < 0) message = messageBuilder;
        else {
            message = new StringBuilder();
            message.append(messageBuilder, 0, lastCommaIndex).append(" and").append(messageBuilder, lastCommaIndex + 1, messageBuilder.toString().length());
        }

        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) player.sendMessageToClient(Text.literal("[Server] " + message), false);
        ScheduledRestart.logInfo(message.toString());
    }

    public static int cancelRestart() {
        if (scheduledRestart == null || scheduledRestart.isDone()) return 0;

        scheduledRestart.cancel(false);
        scheduledRestart = null;
        for (ScheduledFuture<?> scheduledAnnouncement : scheduledAnnouncements) {
            if (scheduledAnnouncement != null) scheduledAnnouncement.cancel(false);
        }
        scheduledAnnouncements = null;
        ScheduledRestart.logInfo("Cancelled existing scheduled restart.");

        return 1;
    }
}
