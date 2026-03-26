package samcask.scheduledrestart;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import samcask.scheduledrestart.config.OldConfigData;
import samcask.scheduledrestart.config.RestartConfig;
import samcask.scheduledrestart.scheduling.AutoRestart;
import samcask.scheduledrestart.scheduling.ManualRestart;
import samcask.scheduledrestart.scheduling.NoPlayerRestart;

import java.io.File;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ScheduledRestart implements ModInitializer {
	public static final String MOD_ID = "scheduled-restart";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
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
		ServerStartCallback.EVENT.register(server -> {
			if (CONFIG.restartScheduleType.equals("daily") && CONFIG.restartInterval > 0) {
				AutoRestart.scheduleAutoRestartDaily(server, CONFIG.dailyRestartTimes);
			}
			else if (CONFIG.restartScheduleType.equals("interval") && CONFIG.restartInterval > 0) {
				AutoRestart.scheduleAutoRestartInterval(server, CONFIG.restartInterval);
			}
		});
		ServerStartCallback.EVENT.register(NoPlayerRestart::finalPlayerDisconnected);
	}

	public static void logInfo(String message) {
		LOGGER.info("[ScheduledRestartMod] - " + message);
	}

	public static void logError(String message) {
		LOGGER.error("[ScheduledRestartMod] - " + message);
	}

	public static void sendAnnouncement(MinecraftServer server, String message, boolean logMessage) {
		for(ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.displayClientMessage(Component.nullToEmpty("[Server] " + message), false);
		}
		if (logMessage) logInfo(message);
	}
}