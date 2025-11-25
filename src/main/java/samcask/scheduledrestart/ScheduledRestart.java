package samcask.scheduledrestart;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public class ScheduledRestart implements ModInitializer {
	public static final String MOD_ID = "scheduled-restart";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static RestartConfig CONFIG;

	@Override
	public void onInitialize() {
		Path configPath = FabricLoader.getInstance().getConfigDir();
		File configFile = configPath.resolve("scheduled-restart.properties").toFile();
		Path oldConfigPath = configPath.resolve("scheduled-restart.json5");
		if (oldConfigPath.toFile().isFile() && !configFile.isFile()) {
			try {
				OldRestartConfig OLD_CONFIG = new OldRestartConfig(oldConfigPath);
				CONFIG = new RestartConfig(configFile, OLD_CONFIG);
			} catch (Exception e) {
				CONFIG = new RestartConfig(configFile);
			}
		} else {
			CONFIG = new RestartConfig(configFile);
		}

		CommandRegistrationCallback.EVENT.register(RestartCommand::register);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (CONFIG.restartScheduleType.value.equals("daily") && CONFIG.restartInterval.value > 0) {
				RestartAutoScheduler.scheduleAutoRestartDaily(server, CONFIG.dailyRestartTimes.value);
			}
			else if (CONFIG.restartScheduleType.value.equals("interval") && CONFIG.restartInterval.value > 0) {
				RestartAutoScheduler.scheduleAutoRestartInterval(server, CONFIG.restartInterval.value);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(RestartNoPlayerScheduler::finalPlayerDisconnected);
	}

	public static void logInfo(String message) {
		ScheduledRestart.LOGGER.info("[ScheduledRestartMod] - " + message);
	}

	public static void logError(String message) {
		ScheduledRestart.LOGGER.error("[ScheduledRestartMod] - " + message);
	}

	public static void sendAnnouncement(MinecraftServer server, String message, boolean logMessage) {
		for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.of("[Server] " + message), false);
		}
		if (logMessage) logInfo(message);
	}
}