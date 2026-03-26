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
import samcask.scheduledrestart.config.OldConfigData;
import samcask.scheduledrestart.config.RestartConfig;
import samcask.scheduledrestart.scheduling.AutoRestart;
import samcask.scheduledrestart.scheduling.ManualRestart;
import samcask.scheduledrestart.scheduling.NoPlayerRestart;

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
		if (oldConfigPath.toFile().isFile()) {
			try {
				OldConfigData OLD_CONFIG = new OldConfigData(oldConfigPath);
				CONFIG = new RestartConfig(configFile, OLD_CONFIG);
			} catch (Exception e) {
				logInfo(e.toString());
				CONFIG = new RestartConfig(configFile);
			}
			oldConfigPath.toFile().delete();
		} else {
			CONFIG = new RestartConfig(configFile);
		}

		CommandRegistrationCallback.EVENT.register(ManualRestart::register);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (CONFIG.restartScheduleType.equals("daily") && CONFIG.restartInterval > 0) {
				AutoRestart.scheduleAutoRestartDaily(server, CONFIG.dailyRestartTimes);
			}
			else if (CONFIG.restartScheduleType.equals("interval") && CONFIG.restartInterval > 0) {
				AutoRestart.scheduleAutoRestartInterval(server, CONFIG.restartInterval);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(NoPlayerRestart::finalPlayerDisconnected);
	}

	public static void logInfo(String message) {
		LOGGER.info("[ScheduledRestartMod] - " + message);
	}

	public static void logError(String message) {
		LOGGER.error("[ScheduledRestartMod] - " + message);
	}

	public static void sendAnnouncement(MinecraftServer server, String message, boolean logMessage) {
		for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.of("[Server] " + message), false);
		}
		if (logMessage) logInfo(message);
	}
}