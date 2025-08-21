package samcask.scheduledrestart;

import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

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
    static final String os = System.getProperty("os.name").toLowerCase();
    static final Path rootPath = Path.of(".");
    static String startScriptPath = getStartScriptPath();
    
    public static int restart(MinecraftServer server) {
        if (!doesScriptExist()) return 0;
        String[] command = tryConstructRestartCommand();
        if (command == null) return 0;
        ScheduledRestart.logInfo("Automatically restarting server...");
        kickPlayers(server);
        stopServerWithRestart(server, command);
        return 1;
    }

    private static boolean doesScriptExist() {
        try {
            if (!Files.exists(rootPath.toRealPath().resolve(startScriptPath))) {
                ScheduledRestart.logError("Start script not found.");
                return false;
            }
        } catch (IOException e) {
            ScheduledRestart.logError("Could not find root directory.");
            return false;
        }
        return true;
    }

    private static @Nullable String[] tryConstructRestartCommand() {
        if (os.contains("win")) {
            return new String[] { "cmd", "/c", "start", "", startScriptPath };
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return new String[] { "bash", startScriptPath };
        } else {
            ScheduledRestart.logError("Operating system not supported.");
            return null;
        }
    }

    private static String getStartScriptPath() {
        String startScriptPath;
        boolean scriptPathDefinedInConfig = !ScheduledRestart.config.startScriptPath.isEmpty();
        if (scriptPathDefinedInConfig) {
            startScriptPath = ScheduledRestart.config.startScriptPath;
        } else if (os.contains("win")) {
            startScriptPath = "start.bat";
        } else {
            startScriptPath = "start.sh";
        }
        return startScriptPath;
    }

    private static void kickPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.disconnect(Text.literal(ScheduledRestart.config.kickMessage));
        }
    }

    private static void stopServerWithRestart(MinecraftServer server, String[] command) {
        prepareRestart(command);
        server.stop(false);
    }

    private static void prepareRestart(String[] command) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Runtime.getRuntime().exec(command, null, rootPath.toFile());
            } catch (IOException e) {
                ScheduledRestart.logError("Failed to run start script.");
            }
        }));
    }
}
