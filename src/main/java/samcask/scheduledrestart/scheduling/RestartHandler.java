package samcask.scheduledrestart.scheduling;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import samcask.scheduledrestart.ScheduledRestart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RestartHandler {
    static final String os = System.getProperty("os.name").toLowerCase();
    static final Path rootPath = Path.of(".");
    static String startupScriptPath = getStartupScriptPath();

    public static int restart(MinecraftServer server) {
        if (!doesStartupScriptExist()) return 0;
        String[] command = constructRestartCommand();
        disconnectPlayers(server);
        stopServerWithRestart(server, command);
        return 1;
    }

    private static boolean doesStartupScriptExist() {
        try {
            if (!Files.exists(rootPath.toRealPath().resolve(startupScriptPath))) {
                ScheduledRestart.logError("Start script not found");
                return false;
            }
        } catch (IOException e) {
            ScheduledRestart.logError("Error while locating startup script: " + e);
            return false;
        }
        return true;
    }

    private static String[] constructRestartCommand() {
        if (os.contains("win")) {
            return new String[] { "cmd", "/c", "start", "", startupScriptPath };
        } else {
            return new String[] { "bash", startupScriptPath };
        }
    }

    private static void disconnectPlayers(MinecraftServer server) {
        ScheduledRestart.logInfo("Disconnecting players...");
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.disconnect(new TextComponent(ScheduledRestart.CONFIG.kickMessage));
            }
            ScheduledRestart.logInfo("Successfully disconnected all players");
        } catch (Exception e) {
            ScheduledRestart.logError("Error while disconnecting players: " + e);
        }
    }

    private static void stopServerWithRestart(MinecraftServer server, String[] command) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Runtime.getRuntime().exec(command, null, rootPath.toFile());
            } catch (Exception e) {
                ScheduledRestart.logError("Error while adding shutdown hook: " + e);
            }
        }));
        ScheduledRestart.logInfo("Restarting server...");
        server.halt(false);
    }

    private static String getStartupScriptPath() {
        String startScriptPath = ScheduledRestart.CONFIG.startScriptPath;
        if (startScriptPath.isEmpty()) {
            if (os.contains("win")) {
                return "start.bat";
            } else {
                return "start.sh";
            }
        }
        return startScriptPath;
    }
}
